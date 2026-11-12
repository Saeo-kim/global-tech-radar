package com.globaltechradar.repository;

import com.globaltechradar.domain.TrendPoint;
import com.globaltechradar.domain.TrendSeriesType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrendPointRepository extends JpaRepository<TrendPoint, Long> {
    List<TrendPoint> findBySeriesTypeOrderBySeriesKeyAscPeriodAsc(TrendSeriesType type);

    List<TrendPoint> findBySeriesTypeAndSeriesKeyOrderByPeriodAsc(TrendSeriesType type, String key);
}
