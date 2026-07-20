package com.SmartDam.Dam_Control_System.repository;

import com.SmartDam.Dam_Control_System.entity.ControlLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ControlLogRepository extends JpaRepository<ControlLog, Long> {
    Optional<ControlLog> findFirstByOrderByTimestampDesc();
}
