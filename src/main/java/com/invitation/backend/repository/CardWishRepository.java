package com.invitation.backend.repository;

import com.invitation.backend.entity.Card;
import com.invitation.backend.entity.CardWish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardWishRepository extends JpaRepository<CardWish, UUID> {
    List<CardWish> findByCardOrderByCreatedAtDesc(Card card);
    long countByCard(Card card);
}
