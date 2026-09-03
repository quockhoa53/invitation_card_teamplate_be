package com.invitation.backend.repository;

import com.invitation.backend.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<Template, UUID> {
    Optional<Template> findBySlug(String slug);
    boolean existsBySlug(String slug);
    
    // Public queries: Only return Active AND Published templates
    List<Template> findByIsActiveTrueAndIsPublishedTrue();
    Page<Template> findByIsActiveTrueAndIsPublishedTrue(Pageable pageable);
    List<Template> findByIsActiveTrueAndIsPublishedTrueAndCategory(String category);
    
    // Admin queries
    List<Template> findByIsActiveTrue();
    long countByIsActiveTrue();
    long countByIsPublishedTrue();
}
