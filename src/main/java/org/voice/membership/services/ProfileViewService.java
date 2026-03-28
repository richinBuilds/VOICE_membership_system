package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.ProfilePageViewData;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.ChildRepository;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileViewService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final MembershipService membershipService;

    public ProfilePageViewData buildProfilePageView(String userEmail) {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            return null;
        }

        membershipService.downgradeExpiredMembership(user);
        user = userRepository.findByEmail(userEmail);

        List<Child> children = childRepository.findByUser(user);
        Membership membership = user.getMembership();

        String fullName = user.getFirstName()
                + (user.getMiddleName() != null && !user.getMiddleName().isEmpty() ? " " + user.getMiddleName() : "")
                + " " + user.getLastName();

        String memberSince = user.getCreation() != null
                ? membershipService.formatMembershipDate(user.getCreation())
                : "Recently";

        if (membership == null) {
            return ProfilePageViewData.builder()
                    .user(user)
                    .userName(fullName)
                    .userEmail(user.getEmail())
                    .userPhone(valueOrDefault(user.getPhone()))
                    .userAddress(valueOrDefault(user.getAddress()))
                    .userCity(valueOrDefault(user.getCity()))
                    .userProvince(valueOrDefault(user.getProvince()))
                    .userPostalCode(valueOrDefault(user.getPostalCode()))
                    .memberSince(memberSince)
                    .children(children)
                    .membershipStatus("None")
                    .membershipType("No Membership Yet")
                    .hasPaidMembership(false)
                    .membershipExpiryDate("-")
                    .showBenefits(false)
                    .isMembershipExpired(false)
                    .membershipBenefit(null)
                    .build();
        }

        if (membership.isFree()) {
            return ProfilePageViewData.builder()
                    .user(user)
                    .userName(fullName)
                    .userEmail(user.getEmail())
                    .userPhone(valueOrDefault(user.getPhone()))
                    .userAddress(valueOrDefault(user.getAddress()))
                    .userCity(valueOrDefault(user.getCity()))
                    .userProvince(valueOrDefault(user.getProvince()))
                    .userPostalCode(valueOrDefault(user.getPostalCode()))
                    .memberSince(memberSince)
                    .children(children)
                    .membershipStatus("Free")
                    .membershipType(membership.getName())
                    .hasPaidMembership(false)
                    .membershipExpiryDate("No expiry")
                    .showBenefits(true)
                    .isMembershipExpired(false)
                    .membershipBenefit(membership.getDescription() != null ? membership.getDescription() : "-")
                    .build();
        }

        Date expiryDate = user.getMembershipExpiryDate();
        String formattedExpiryDate = expiryDate != null
                ? membershipService.formatMembershipDate(expiryDate)
                : "-";

        return ProfilePageViewData.builder()
                .user(user)
                .userName(fullName)
                .userEmail(user.getEmail())
                .userPhone(valueOrDefault(user.getPhone()))
                .userAddress(valueOrDefault(user.getAddress()))
                .userCity(valueOrDefault(user.getCity()))
                .userProvince(valueOrDefault(user.getProvince()))
                .userPostalCode(valueOrDefault(user.getPostalCode()))
                .memberSince(memberSince)
                .children(children)
                .membershipStatus("Paid")
                .membershipType(membership.getName())
                .hasPaidMembership(true)
                .membershipExpiryDate(formattedExpiryDate)
                .showBenefits(false)
                .isMembershipExpired(false)
                .membershipBenefit(null)
                .build();
    }

    private String valueOrDefault(String value) {
        return value != null ? value : "Not provided";
    }
}
