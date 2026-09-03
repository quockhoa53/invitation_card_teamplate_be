package com.invitation.backend.repository;

import com.invitation.backend.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    Page<Promotion> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE (:search IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:isActive IS NULL OR p.isActive = :isActive) ORDER BY p.createdAt DESC")
    Page<Promotion> findAllFiltered(@Param("search") String search,
                                    @Param("isActive") Boolean isActive,
                                    Pageable pageable);
}
