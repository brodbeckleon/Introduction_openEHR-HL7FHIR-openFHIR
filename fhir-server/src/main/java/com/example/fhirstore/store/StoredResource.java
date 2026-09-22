package com.example.fhirstore.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * One FHIR resource, kept as the JSON it arrived as.
 *
 * <p>Storing the serialised resource rather than a column per field is the point: this server holds
 * whatever a profile says a Patient may carry, and a schema that enumerated the fields would have to
 * be changed every time the profile was. What is pulled out into columns is only what has to be
 * searchable.
 */
@Entity
@Table(
        name = "stored_resource",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resource_type", "resource_id"}))
public class StoredResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 128)
    private String resourceId;

    @Column(name = "json", nullable = false, columnDefinition = "text")
    private String json;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StoredResource() {}

    public StoredResource(String resourceType, String resourceId, String json) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.json = json;
        this.updatedAt = Instant.now();
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getJson() {
        return json;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void replaceWith(String newJson) {
        this.json = newJson;
        this.updatedAt = Instant.now();
    }
}
