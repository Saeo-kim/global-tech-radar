package com.globaltechradar.repository;

import com.globaltechradar.domain.Technology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechnologyRepository extends JpaRepository<Technology, String> {
    List<Technology> findAllByOrderByMomentumDesc();
}
