package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.config.PayPalProperties;
import org.voice.membership.dtos.ProfileCancellationViewData;
import org.voice.membership.dtos.ProfileUpgradeCheckoutViewData;
import org.voice.membership.dtos.ProfileUpgradeViewData;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.MembershipRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileMembershipService {

    private final CurrentUserService currentUserService;
    private final MembershipRepository membershipRepository;
    private final PayPalProperties payPalProperties;
    private final MembershipCancellationService membershipCancellationService;

    public ProfileUpgradeViewData buildUpgradePageViewData(String email) {
        User user = currentUserService.getCurrentUser(email);
        if (user == null) {
            return null;
        }

        Membership membership = user.getMembership();
        if (membership == null || !membership.isFree()) {
            return null;
        }

        List<Membership> paidMemberships = membershipRepository.findByIsFree(false);
        return new ProfileUpgradeViewData(user, membership, paidMemberships, fullName(user));
    }

    public ProfileUpgradeCheckoutViewData buildUpgradeCheckoutViewData(String email, Integer membershipId) {
        if (membershipId == null) {
            return null;
        }

        User user = currentUserService.getCurrentUser(email);
        if (user == null) {
            return null;
        }

        Membership currentMembership = user.getMembership();
        if (currentMembership == null || !currentMembership.isFree()) {
            return null;
        }

        Optional<Membership> paidMembershipOpt = membershipRepository.findById(membershipId);
        if (paidMembershipOpt.isEmpty() || paidMembershipOpt.get().isFree()) {
            return null;
        }

        Membership paidMembership = paidMembershipOpt.get();
        return new ProfileUpgradeCheckoutViewData(
                user,
                currentMembership,
                paidMembership,
                paidMembership.getName(),
                paidMembership.getPrice(),
                fullName(user),
                payPalProperties.getClientId(),
                payPalProperties.getCurrency(),
                "upgrade");
    }

    public ProfileCancellationViewData buildCancellationViewData(String email) {
        User user = currentUserService.getCurrentUser(email);
        if (user == null || !membershipCancellationService.canCancelMembership(user.getId())) {
            return null;
        }

        MembershipCancellationService.MembershipInfo membershipInfo =
                membershipCancellationService.getCurrentMembershipInfo(user.getId());

        return new ProfileCancellationViewData(
                user,
                fullName(user),
                membershipInfo.getName(),
                membershipInfo.isFree());
    }

    public MembershipCancellationService.CancellationResult cancelMembership(String email) {
        User user = currentUserService.getCurrentUser(email);
        if (user == null) {
            return new MembershipCancellationService.CancellationResult(false, "User not found");
        }
        return membershipCancellationService.cancelMembership(user.getId());
    }

    private String fullName(User user) {
        return user.getFirstName()
                + (user.getMiddleName() != null && !user.getMiddleName().isEmpty() ? " " + user.getMiddleName() : "")
                + " " + user.getLastName();
    }
}
