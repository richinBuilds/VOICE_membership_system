package org.voice.membership.services;

import org.springframework.stereotype.Service;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for filtering users based on various criteria.
 * Handles all user filtering business logic for admin dashboards and reports.
 */
@Service
public class UserFilterService {

    /**
     * Apply all filters to a list of users.
     * 
     * @param users           List of users to filter
     * @param address         Filter by address or postal code
     * @param city            Filter by city
     * @param province        Filter by province
     * @param chapter         Filter by chapter
     * @param minAge          Minimum child age
     * @param maxAge          Maximum child age
     * @param hearingLossType Filter by hearing loss type
     * @param equipmentType   Filter by equipment type
     * @param startDate       Filter by registration start date (yyyy-MM-dd)
     * @param endDate         Filter by registration end date (yyyy-MM-dd)
     * @param paymentStatus   Filter by payment status ("paid" or "unpaid")
     * @param role            Filter by user role ("ADMIN" or "USER")
     * @return Filtered list of users
     */
    public List<User> filterUsers(List<User> users, String address, String city, String province, String chapter,
            Integer minAge, Integer maxAge,
            String hearingLossType, String equipmentType,
            String startDate, String endDate, String paymentStatus, String role) {
        return users.stream()
                .filter(user -> filterByAddress(user, address))
                .filter(user -> filterByCity(user, city))
                .filter(user -> filterByProvince(user, province))
                .filter(user -> filterByChapter(user, chapter))
                .filter(user -> filterByChildAge(user, minAge, maxAge))
                .filter(user -> filterByHearingLossType(user, hearingLossType))
                .filter(user -> filterByEquipmentType(user, equipmentType))
                .filter(user -> filterByRegistrationDate(user, startDate, endDate))
                .filter(user -> filterByPaymentStatus(user, paymentStatus))
                .filter(user -> filterByRole(user, role))
                .collect(Collectors.toList());
    }

    /**
     * Filter by address or postal code.
     */
    private boolean filterByAddress(User user, String address) {
        if (address == null || address.trim().isEmpty()) {
            return true;
        }
        String userAddress = user.getAddress();
        String userPostalCode = user.getPostalCode();
        String searchTerm = address.toLowerCase();

        return (userAddress != null && userAddress.toLowerCase().contains(searchTerm)) ||
                (userPostalCode != null && userPostalCode.toLowerCase().contains(searchTerm));
    }

    /**
     * Filter by city.
     */
    private boolean filterByCity(User user, String city) {
        if (city == null || city.trim().isEmpty()) {
            return true;
        }
        String userCity = user.getCity();
        return userCity != null && userCity.toLowerCase().contains(city.toLowerCase());
    }

    /**
     * Filter by province.
     */
    private boolean filterByProvince(User user, String province) {
        if (province == null || province.trim().isEmpty()) {
            return true;
        }
        String userProvince = user.getProvince();
        return userProvince != null && userProvince.toLowerCase().contains(province.toLowerCase());
    }

    /**
     * Filter by chapter.
     */
    private boolean filterByChapter(User user, String chapter) {
        if (chapter == null || chapter.trim().isEmpty()) {
            return true;
        }
        String userChapter = user.getChapter();
        return userChapter != null && userChapter.toLowerCase().contains(chapter.toLowerCase());
    }

    /**
     * Filter by child age range.
     * Returns true if user has at least one child within the specified age range.
     */
    private boolean filterByChildAge(User user, Integer minAge, Integer maxAge) {
        if (minAge == null && maxAge == null) {
            return true;
        }

        List<Child> children = user.getChildren();
        if (children == null || children.isEmpty()) {
            return false;
        }

        return children.stream().anyMatch(child -> {
            Integer age = child.getAge();
            if (age == null) {
                return false;
            }
            boolean meetsMin = minAge == null || age >= minAge;
            boolean meetsMax = maxAge == null || age <= maxAge;
            return meetsMin && meetsMax;
        });
    }

    /**
     * Filter by hearing loss type.
     * Returns true if user has at least one child with the specified hearing loss
     * type.
     */
    private boolean filterByHearingLossType(User user, String hearingLossType) {
        if (hearingLossType == null || hearingLossType.trim().isEmpty()) {
            return true;
        }

        List<Child> children = user.getChildren();
        if (children == null || children.isEmpty()) {
            return false;
        }

        return children.stream().anyMatch(child -> hearingLossType.equalsIgnoreCase(child.getHearingLossType()));
    }

    /**
     * Filter by equipment type.
     * Returns true if user has at least one child with the specified equipment
     * type.
     */
    private boolean filterByEquipmentType(User user, String equipmentType) {
        if (equipmentType == null || equipmentType.trim().isEmpty()) {
            return true;
        }

        List<Child> children = user.getChildren();
        if (children == null || children.isEmpty()) {
            return false;
        }

        return children.stream().anyMatch(child -> equipmentType.equalsIgnoreCase(child.getEquipmentType()));
    }

    /**
     * Filter by registration date range.
     * 
     * @param startDateStr Start date in yyyy-MM-dd format
     * @param endDateStr   End date in yyyy-MM-dd format (inclusive)
     */
    private boolean filterByRegistrationDate(User user, String startDateStr, String endDateStr) {
        if ((startDateStr == null || startDateStr.trim().isEmpty()) &&
                (endDateStr == null || endDateStr.trim().isEmpty())) {
            return true;
        }

        Date userCreation = user.getCreation();
        if (userCreation == null) {
            return false;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                Date startDate = dateFormat.parse(startDateStr);
                if (userCreation.before(startDate)) {
                    return false;
                }
            }

            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                Date endDate = dateFormat.parse(endDateStr);
                // Add 24 hours to make end date inclusive
                endDate = new Date(endDate.getTime() + 24 * 60 * 60 * 1000);
                if (userCreation.after(endDate)) {
                    return false;
                }
            }

            return true;
        } catch (ParseException e) {
            // If date parsing fails, don't filter out the user
            return true;
        }
    }

    /**
     * Filter by payment status.
     * 
     * @param paymentStatus "paid" for paid members, "unpaid" for free members
     */
    private boolean filterByPaymentStatus(User user, String paymentStatus) {
        if (paymentStatus == null || paymentStatus.trim().isEmpty()) {
            return true;
        }

        // A user is "paid" if they have a non-free membership
        boolean isPaidMember = user.getMembership() != null && !user.getMembership().isFree();

        if ("paid".equalsIgnoreCase(paymentStatus)) {
            return isPaidMember;
        } else if ("unpaid".equalsIgnoreCase(paymentStatus)) {
            return !isPaidMember;
        }

        return true;
    }

    /**
     * Filter by user role.
     * 
     * @param role User role ("ADMIN" or "USER")
     */
    private boolean filterByRole(User user, String role) {
        if (role == null || role.trim().isEmpty()) {
            return true;
        }
        
        String userRole = user.getRole();
        return role.equalsIgnoreCase(userRole);
    }
}
