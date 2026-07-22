package com.adaptivegateway.gateway.entity;

import com.adaptivegateway.gateway.enums.GatewayRouteStatus;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "gateway_routes")
public class GatewayRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "upstream_service_id", nullable = false)
    private UpstreamService upstreamService;

    @Column(name = "route_key", nullable = false, length = 120)
    private String routeKey;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "path_pattern", nullable = false, length = 512)
    private String pathPattern;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_methods", nullable = false, columnDefinition = "text[]")
    private String[] allowedMethods;

    @Column(name = "strip_prefix", nullable = false)
    private Integer stripPrefix;

    @Column(nullable = false)
    private Integer priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GatewayRouteStatus status;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    private List<GatewayRoutePredicate> predicates = new ArrayList<>();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public UpstreamService getUpstreamService() {
        return upstreamService;
    }

    public void setUpstreamService(UpstreamService upstreamService) {
        this.upstreamService = upstreamService;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public void setRouteKey(String routeKey) {
        this.routeKey = routeKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public String[] getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String[] allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public Integer getStripPrefix() {
        return stripPrefix;
    }

    public void setStripPrefix(Integer stripPrefix) {
        this.stripPrefix = stripPrefix;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public GatewayRouteStatus getStatus() {
        return status;
    }

    public void setStatus(GatewayRouteStatus status) {
        this.status = status;
    }

    public List<GatewayRoutePredicate> getPredicates() {
        return predicates;
    }

    public void replacePredicates(List<GatewayRoutePredicate> replacement) {
        predicates.clear();
        replacement.forEach(predicate -> {
            predicate.setRoute(this);
            predicates.add(predicate);
        });
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

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
