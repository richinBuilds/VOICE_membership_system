package org.voice.membership.dtos;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
/**
 * Aggregates data across the multi-step registration wizard.
 * Holds user details, child info, and membership/cart selections between steps.
 */
public class MultiStepRegistrationDto {
    // Step 1: User details
    private RegisterDto userDetails;

    // Step 2: Child information
    private List<ChildDto> children = new ArrayList<>();

    // Step 3: Membership selection
    private Integer selectedMembershipId;

    // Step 4: Cart (will be populated from selected membership)
    private Integer cartMembershipId;
}
