package com.adaptivegateway.analytics.repository;

import com.adaptivegateway.analytics.entity.ClientMetric;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientMetricRepository extends JpaRepository<ClientMetric, UUID> {

    @EntityGraph(attributePaths = {"consumer"})
    @Query("""
            select metric
            from ClientMetric metric
            where metric.deletedAt is null
            and (:consumerId is null or metric.consumer.id = :consumerId)
            """)
    Page<ClientMetric> search(@Param("consumerId") UUID consumerId, Pageable pageable);
}
