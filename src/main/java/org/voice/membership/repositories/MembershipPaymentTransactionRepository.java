package org.voice.membership.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.MembershipPaymentTransaction;

import java.util.Optional;

@Repository
public interface MembershipPaymentTransactionRepository extends JpaRepository<MembershipPaymentTransaction, Long> {
    Optional<MembershipPaymentTransaction> findByPaypalOrderId(String paypalOrderId);

    Optional<MembershipPaymentTransaction> findByPaypalCaptureId(String paypalCaptureId);

    boolean existsByUser_IdAndMembership_IdAndStatus(int userId, int membershipId, String status);

    void deleteByUser_Id(int userId);
}
