package com.SmartDam.Dam_Control_System.repository;

import com.SmartDam.Dam_Control_System.entity.ReservoirState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReservoirStateRepository extends JpaRepository<ReservoirState, Long> {
    Optional<ReservoirState> findFirstByOrderByTimestampDesc();
    Optional<ReservoirState> findFirstByDamIdOrderByTimestampDesc(String damId);
}
