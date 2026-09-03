using System.Text.Json;
using DamControlSystem.BackgroundServices;
using DamControlSystem.Data;
using DamControlSystem.Data.Repositories;
using DamControlSystem.Services;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// 1. Controller & JSON Configuration (camelCase matches frontend JavaScript)
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.PropertyNameCaseInsensitive = true;
    });

// 2. CORS Policy for Frontend / Development
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

// 3. Database Context Setup (MySQL with automatic SQLite fallback for standalone local runs)
var provider = builder.Configuration.GetValue<string>("DatabaseProvider") ?? "Auto";
var mysqlConn = builder.Configuration.GetConnectionString("DefaultConnection") 
    ?? "Server=localhost;Port=3306;Database=smart_dam_db;User=root;Password=tanmayyash2005;";
var sqliteConn = builder.Configuration.GetConnectionString("SqliteConnection") 
    ?? "Data Source=smart_dam.db";

if (provider.Equals("MySql", StringComparison.OrdinalIgnoreCase))
{
    builder.Services.AddDbContext<SmartDamDbContext>(options =>
        options.UseMySql(mysqlConn, ServerVersion.AutoDetect(mysqlConn)));
}
else if (provider.Equals("Sqlite", StringComparison.OrdinalIgnoreCase))
{
    builder.Services.AddDbContext<SmartDamDbContext>(options =>
        options.UseSqlite(sqliteConn));
}
else if (provider.Equals("InMemory", StringComparison.OrdinalIgnoreCase))
{
    builder.Services.AddDbContext<SmartDamDbContext>(options =>
        options.UseInMemoryDatabase("SmartDamDb"));
}
else // "Auto" mode: Detect if MySQL is reachable, otherwise gracefully use SQLite
{
    bool mysqlAvailable = false;
    try
    {
        var serverVersion = ServerVersion.AutoDetect(mysqlConn);
        mysqlAvailable = true;
        builder.Services.AddDbContext<SmartDamDbContext>(options =>
            options.UseMySql(mysqlConn, serverVersion));
    }
    catch
    {
        mysqlAvailable = false;
    }

    if (!mysqlAvailable)
    {
        builder.Services.AddDbContext<SmartDamDbContext>(options =>
            options.UseSqlite(sqliteConn));
    }
}

// 4. Register Repositories
builder.Services.AddScoped<IReservoirStateRepository, ReservoirStateRepository>();
builder.Services.AddScoped<IControlLogRepository, ControlLogRepository>();
builder.Services.AddScoped<IEmergencyAlertRepository, EmergencyAlertRepository>();

// 5. Register HTTP Client & Weather Forecast Service
var weatherBaseUrl = builder.Configuration.GetValue<string>("WeatherApi:BaseUrl") ?? "https://api.open-meteo.com/";
builder.Services.AddHttpClient<IWeatherForecastService, WeatherForecastService>(client =>
{
    client.BaseAddress = new Uri(weatherBaseUrl);
    client.Timeout = TimeSpan.FromSeconds(10);
});

// 6. Register Business Services
builder.Services.AddScoped<IDamControlEngineService, DamControlEngineService>();
builder.Services.AddScoped<IAiSuggestionService, AiSuggestionService>();

// 7. Register Background Scheduling Service (Hourly dam flow evaluation)
builder.Services.AddHostedService<WaterFlowBackgroundService>();

var app = builder.Build();

// 8. Auto-create database schema on launch & migrate existing MySQL tables if necessary
using (var scope = app.Services.CreateScope())
{
    var dbContext = scope.ServiceProvider.GetRequiredService<SmartDamDbContext>();
    var logger = scope.ServiceProvider.GetRequiredService<ILogger<Program>>();
    try
    {
        dbContext.Database.EnsureCreated();

        if (dbContext.Database.IsMySql())
        {
            try
            {
                var conn = dbContext.Database.GetDbConnection();
                if (conn.State != System.Data.ConnectionState.Open)
                {
                    conn.Open();
                }

                bool HasColumn(string table, string column)
                {
                    using var cmd = conn.CreateCommand();
                    cmd.CommandText = $"SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = '{conn.Database}' AND table_name = '{table}' AND column_name = '{column}'";
                    var count = cmd.ExecuteScalar();
                    return count != null && Convert.ToInt64(count) > 0;
                }

                if (!HasColumn("reservoir_state", "dam_id"))
                {
                    logger.LogInformation("Adding dam_id column to reservoir_state table...");
                    dbContext.Database.ExecuteSqlRaw("ALTER TABLE reservoir_state ADD COLUMN dam_id VARCHAR(255) DEFAULT 'erai';");
                }

                if (!HasColumn("control_log", "dam_id"))
                {
                    logger.LogInformation("Adding dam_id column to control_log table...");
                    dbContext.Database.ExecuteSqlRaw("ALTER TABLE control_log ADD COLUMN dam_id VARCHAR(255) DEFAULT 'erai';");
                }

                if (!HasColumn("control_log", "approval_status"))
                {
                    logger.LogInformation("Adding approval_status column to control_log table...");
                    dbContext.Database.ExecuteSqlRaw("ALTER TABLE control_log ADD COLUMN approval_status VARCHAR(255) DEFAULT 'PENDING_OPERATOR';");
                }

                dbContext.Database.ExecuteSqlRaw(
                    @"CREATE TABLE IF NOT EXISTS emergency_alert (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        dam_id VARCHAR(255) NOT NULL,
                        priority VARCHAR(50) NOT NULL,
                        message VARCHAR(1000) NOT NULL,
                        timestamp DATETIME(6) NOT NULL,
                        shift_officer_name VARCHAR(255) NOT NULL,
                        resolved BOOLEAN NOT NULL DEFAULT FALSE
                    );");
            }
            catch (Exception ex)
            {
                logger.LogWarning("Schema synchronization notice: {Message}", ex.Message);
            }
        }

        logger.LogInformation("Database verified / synchronized successfully.");
    }
    catch (Exception ex)
    {
        logger.LogError(ex, "Failed to ensure database creation: {Message}", ex.Message);
    }
}

// 9. Middleware Pipeline
app.UseCors();

// Serve static frontend from wwwroot (HYDRO-OS dashboard UI)
app.UseDefaultFiles();
app.UseStaticFiles();

app.UseRouting();

app.MapControllers();

// Fallback to index.html for client-side routing
app.MapFallbackToFile("index.html");

app.Run();

// Required for WebApplicationFactory in integration tests
public partial class Program { }
