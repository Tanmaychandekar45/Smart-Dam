using DamControlSystem.Models;
using Xunit;

namespace DamControlSystem.Tests;

public class DamMetadataTests
{
    [Fact]
    public void GetRegistry_ContainsExpectedDams()
    {
        var registry = DamMetadata.GetRegistry();

        Assert.True(registry.ContainsKey("erai"));
        Assert.True(registry.ContainsKey("khadakwasla"));
        Assert.True(registry.ContainsKey("panshet"));
        Assert.True(registry.ContainsKey("mulshi"));
    }

    [Fact]
    public void Get_UnknownDam_FallsBackToErai()
    {
        var dam = DamMetadata.Get("unknown_dam_xyz");
        Assert.Equal("erai", dam.Id);
    }

    [Theory]
    [InlineData("erai", 226500000.0, 800.0)]
    [InlineData("khadakwasla", 56000000.0, 500.0)]
    [InlineData("panshet", 294000000.0, 1000.0)]
    [InlineData("mulshi", 522000000.0, 1500.0)]
    public void DamMetadata_HasAccurateCapacitiesAndSafeDischarge(string id, double expectedCap, double expectedSafeDischarge)
    {
        var dam = DamMetadata.Get(id);
        Assert.Equal(expectedCap, dam.MaxCapacityM3);
        Assert.Equal(expectedSafeDischarge, dam.MaxSafeDischargeM3s);
        Assert.NotEmpty(dam.DownstreamVillages);
    }
}
