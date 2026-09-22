package com.example.fhirstore.store;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredResourceRepository extends JpaRepository<StoredResource, Long> {

    Optional<StoredResource> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    List<StoredResource> findByResourceTypeOrderByResourceIdAsc(String resourceType);
}
