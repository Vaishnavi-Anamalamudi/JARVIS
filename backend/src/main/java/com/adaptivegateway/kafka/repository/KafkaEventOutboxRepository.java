package com.adaptivegateway.kafka.repository;

import com.adaptivegateway.kafka.entity.KafkaEventOutbox;
import com.adaptivegateway.kafka.enums.KafkaOutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KafkaEventOutboxRepository extends JpaRepository<KafkaEventOutbox, UUID> {

    Page<KafkaEventOutbox> findByDeletedAtIsNull(Pageable pageable);

    Page<KafkaEventOutbox> findByStatusAndDeletedAtIsNull(KafkaOutboxStatus status, Pageable pageable);

    Optional<KafkaEventOutbox> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select event from KafkaEventOutbox event
            where event.deletedAt is null
            and event.status = com.adaptivegateway.kafka.enums.KafkaOutboxStatus.PENDING
            and (event.nextAttemptAt is null or event.nextAttemptAt <= :now)
            order by event.createdAt asc
            """)
    List<KafkaEventOutbox> findDuePendingEvents(@Param("now") Instant now, Pageable pageable);
}
