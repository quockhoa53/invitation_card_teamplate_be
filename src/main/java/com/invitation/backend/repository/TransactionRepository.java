package com.invitation.backend.repository;

import com.invitation.backend.entity.Transaction;
import com.invitation.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByOrderCode(String orderCode);
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    Page<Transaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'SUCCESS' AND t.paymentMethod != 'WALLET' AND (t.type IS NULL OR t.type = 'DEPOSIT')")
    Long getTotalRevenue();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'SUCCESS' AND t.paymentMethod != 'WALLET' AND (t.type IS NULL OR t.type = 'DEPOSIT')")
    Long countRevenueTransactions();
    
    long countByStatus(Transaction.Status status);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user u WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(t.orderCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<Transaction> findAllFiltered(
            @Param("status") Transaction.Status status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user u WHERE t.status = :status")
    Page<Transaction> findAllByStatus(@Param("status") Transaction.Status status, Pageable pageable);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.user u")
    Page<Transaction> findAllWithUser(Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Transaction t SET t.status = :newStatus, t.completedAt = :completedAt, t.gatewayPayload = :gatewayPayload WHERE t.orderCode = :orderCode AND t.status != com.invitation.backend.entity.Transaction.Status.SUCCESS")
    int atomicMarkSuccess(
            @Param("orderCode") String orderCode,
            @Param("newStatus") Transaction.Status newStatus,
            @Param("completedAt") java.time.LocalDateTime completedAt,
            @Param("gatewayPayload") String gatewayPayload
    );
}
