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
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.creditsBalance = u.creditsBalance + :amount, u.realBalance = u.realBalance + :amount WHERE u.id = :userId")
    int atomicAddCredits(@org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("amount") Long amount);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.realBalance = u.realBalance + :realAmount, u.bonusBalance = u.bonusBalance + :bonusAmount, u.creditsBalance = u.creditsBalance + :realAmount + :bonusAmount WHERE u.id = :userId")
    int atomicAddDepositWithBonus(@org.springframework.data.repository.query.Param("userId") UUID userId,
                                  @org.springframework.data.repository.query.Param("realAmount") Long realAmount,
                                  @org.springframework.data.repository.query.Param("bonusAmount") Long bonusAmount);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.realBalance = u.realBalance - :realDeduct, u.bonusBalance = u.bonusBalance - :bonusDeduct, u.creditsBalance = u.creditsBalance - :realDeduct - :bonusDeduct WHERE u.id = :userId AND u.realBalance >= :realDeduct AND u.bonusBalance >= :bonusDeduct")
    int atomicDeductForPurchase(@org.springframework.data.repository.query.Param("userId") UUID userId,
                                @org.springframework.data.repository.query.Param("realDeduct") Long realDeduct,
                                @org.springframework.data.repository.query.Param("bonusDeduct") Long bonusDeduct);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.creditsBalance = u.creditsBalance - :amount WHERE u.id = :userId AND u.creditsBalance >= :amount")
    int atomicDeductCredits(@org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("amount") Long amount);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.realBalance = u.realBalance - :amount, u.creditsBalance = u.creditsBalance - :amount WHERE u.id = :userId AND u.realBalance >= :amount")
    int atomicLockForWithdrawal(@org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("amount") Long amount);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.realBalance = u.realBalance + :amount, u.creditsBalance = u.creditsBalance + :amount WHERE u.id = :userId")
    int atomicRefundWithdrawal(@org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("amount") Long amount);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.creditsBalance = u.creditsBalance - u.bonusBalance, u.bonusBalance = 0 WHERE u.id = :userId")
    int atomicClawbackBonus(@org.springframework.data.repository.query.Param("userId") UUID userId);
}
