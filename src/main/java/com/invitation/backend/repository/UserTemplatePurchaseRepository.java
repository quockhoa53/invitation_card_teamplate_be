package com.invitation.backend.repository;

import com.invitation.backend.entity.Template;
import com.invitation.backend.entity.User;
import com.invitation.backend.entity.UserTemplatePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserTemplatePurchaseRepository extends JpaRepository<UserTemplatePurchase, UUID> {

    boolean existsByUserAndTemplate(User user, Template template);

    boolean existsByUserIdAndTemplateId(UUID userId, UUID templateId);

    @Query("SELECT p.template.id FROM UserTemplatePurchase p WHERE p.user.id = :userId")
    List<UUID> findPurchasedTemplateIdsByUserId(@Param("userId") UUID userId);

    List<UserTemplatePurchase> findByUserOrderByPurchasedAtDesc(User user);
}
