package org.voice.membership.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.voice.membership.entities.MembershipPaymentTransaction;

import java.util.Optional;

@Repository
public interface MembershipPaymentTransactionRepository extends JpaRepository<MembershipPaymentTransaction, Long> {
    Optional<MembershipPaymentTransaction> findByPaypalOrderId(String paypalOrderId);

    Optional<MembershipPaymentTransaction> findByPaypalCaptureId(String paypalCaptureId);

    boolean existsByUser_IdAndMembership_IdAndStatus(int userId, int membershipId, String status);
}
