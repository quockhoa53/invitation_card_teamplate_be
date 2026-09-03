package com.invitation.backend.repository;

import com.invitation.backend.entity.Card;
import com.invitation.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    Optional<Card> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Card> findByUserOrderByCreatedAtDesc(User user);
    Page<Card> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    long countByUser(User user);
    long countByIsPublishedTrue();
}
