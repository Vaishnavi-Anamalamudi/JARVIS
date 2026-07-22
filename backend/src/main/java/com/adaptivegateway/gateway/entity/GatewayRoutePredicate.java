package com.adaptivegateway.gateway.entity;

import com.adaptivegateway.gateway.enums.RoutePredicateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "gateway_route_predicates")
public class GatewayRoutePredicate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private GatewayRoute route;

    @Enumerated(EnumType.STRING)
    @Column(name = "predicate_type", nullable = false, length = 60)
    private RoutePredicateType predicateType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "predicate_config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> predicateConfig = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public GatewayRoute getRoute() {
        return route;
    }

    public void setRoute(GatewayRoute route) {
        this.route = route;
    }

    public RoutePredicateType getPredicateType() {
        return predicateType;
    }

    public void setPredicateType(RoutePredicateType predicateType) {
        this.predicateType = predicateType;
    }

    public Map<String, Object> getPredicateConfig() {
        return predicateConfig;
    }

    public void setPredicateConfig(Map<String, Object> predicateConfig) {
        this.predicateConfig = predicateConfig == null ? new LinkedHashMap<>() : new LinkedHashMap<>(predicateConfig);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
