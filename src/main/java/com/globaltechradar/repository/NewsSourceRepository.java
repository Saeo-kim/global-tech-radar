package com.globaltechradar.repository;

import com.globaltechradar.domain.NewsSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsSourceRepository extends JpaRepository<NewsSource, String> {
    List<NewsSource> findAllByOrderByNameAsc();

    List<NewsSource> findByEnabledTrue();

    boolean existsByUrl(String url);
}
