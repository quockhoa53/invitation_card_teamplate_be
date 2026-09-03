package com.invitation.backend.repository;

import com.invitation.backend.entity.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateCategoryRepository extends JpaRepository<TemplateCategory, UUID> {

    List<TemplateCategory> findByIsActiveTrueOrderByDisplayOrderAscCreatedAtAsc();

    List<TemplateCategory> findAllByOrderByDisplayOrderAscCreatedAtAsc();

    Optional<TemplateCategory> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
