package com.invitation.backend.repository;

import com.invitation.backend.entity.User;
import com.invitation.backend.entity.User2FA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface User2FARepository extends JpaRepository<User2FA, UUID> {
    Optional<User2FA> findByUser(User user);
    Optional<User2FA> findByUserId(UUID userId);
}
