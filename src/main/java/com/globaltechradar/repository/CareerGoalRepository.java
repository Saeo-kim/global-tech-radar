package com.globaltechradar.repository;

import com.globaltechradar.domain.CareerGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareerGoalRepository extends JpaRepository<CareerGoal, Long> {
    List<CareerGoal> findAllByOrderByTargetDateAsc();
}
