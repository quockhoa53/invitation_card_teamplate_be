package com.invitation.backend.repository;

import com.invitation.backend.entity.Role;
import com.invitation.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByRole(Role role, Pageable pageable);
    long countByRole(Role role);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.creditsBalance = u.creditsBalance + :amount WHERE u.id = :userId")
    int atomicAddCredits(@org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("amount") Long amount);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.creditsBalance = u.creditsBalance - :amount WHERE u.id = :userId AND u.creditsBalance >= :amount")
    int atomicDeductCredits(@org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("amount") Long amount);
}
