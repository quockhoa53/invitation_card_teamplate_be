package com.invitation.backend.repository;

import com.invitation.backend.entity.TemplateSchemaKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateSchemaKeyRepository extends JpaRepository<TemplateSchemaKey, UUID> {

    List<TemplateSchemaKey> findByIsActiveTrueOrderByDisplayOrderAscCreatedAtAsc();

    List<TemplateSchemaKey> findAllByOrderByDisplayOrderAscCreatedAtAsc();

    Optional<TemplateSchemaKey> findByKeyName(String keyName);

    boolean existsByKeyName(String keyName);

    boolean existsByKeyNameAndIdNot(String keyName, UUID id);
}
