package com.globaltechradar.repository;

import com.globaltechradar.domain.MarketCategory;
import com.globaltechradar.domain.MarketSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketSignalRepository extends JpaRepository<MarketSignal, Long> {
    List<MarketSignal> findByCategoryOrderByMetricValueDesc(MarketCategory category);
}
