package com.SmartDam.Dam_Control_System.repository;

import com.SmartDam.Dam_Control_System.entity.EmergencyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {
    List<EmergencyAlert> findByDamIdAndResolvedOrderByTimestampDesc(String damId, boolean resolved);
    List<EmergencyAlert> findByResolvedOrderByTimestampDesc(boolean resolved);
}
