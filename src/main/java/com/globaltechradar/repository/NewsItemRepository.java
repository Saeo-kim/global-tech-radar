package com.globaltechradar.repository;

import com.globaltechradar.domain.DataStatus;
import com.globaltechradar.domain.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    boolean existsByDedupeKey(String dedupeKey);

    boolean existsByTitleKeyAndCollectedAtAfter(String titleKey, Instant after);

    long countByProvenanceDataStatus(DataStatus status);

    List<NewsItem> findByFeedIdOrderByPublishedAtDesc(String feedId);

    @Transactional
    @Modifying
    @Query("delete from NewsItem n where n.provenance.dataStatus = :status")
    int deleteByStatus(DataStatus status);

    @Transactional
    @Modifying
    @Query("delete from NewsItem n where n.provenance.dataStatus = com.globaltechradar.domain.DataStatus.LIVE and n.collectedAt < :before")
    int deleteLiveCollectedBefore(Instant before);
}
