package com.invitation.backend.repository;

import com.invitation.backend.entity.User;
import com.invitation.backend.entity.Withdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {

    Page<Withdrawal> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Withdrawal> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Withdrawal> findByStatusOrderByCreatedAtDesc(Withdrawal.Status status, Pageable pageable);

    @Query("SELECT w FROM Withdrawal w JOIN FETCH w.user WHERE (:status IS NULL OR w.status = :status) " +
           "AND (:search IS NULL OR LOWER(w.accountHolder) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(w.accountNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(w.user.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Withdrawal> findAllFiltered(@Param("status") Withdrawal.Status status,
                                     @Param("search") String search,
                                     Pageable pageable);
}
