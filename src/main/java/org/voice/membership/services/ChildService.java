package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.ChildRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Service for managing child entities.
 * Handles all child CRUD operations and business logic.
 */
@Service
@RequiredArgsConstructor
public class ChildService {

    private final ChildRepository childRepository;

    /**
     * Create a new child for a user.
     * 
     * @param user            The parent user
     * @param name            Child's name
     * @param age             Child's age
     * @param dateOfBirthStr  Date of birth as string (yyyy-MM-dd)
     * @param hearingLossType Type of hearing loss
     * @param equipmentType   Type of equipment used
     * @param siblingsNames   Names of siblings
     * @param chapterLocation Chapter location
     * @return The created child entity
     */
    public Child createChild(User user, String name, Integer age, String dateOfBirthStr,
            String hearingLossType, String equipmentType, String siblingsNames, String chapterLocation) {

        Child child = Child.builder()
                .name(name)
                .age(age)
                .hearingLossType(hearingLossType)
                .equipmentType(equipmentType)
                .siblingsNames(siblingsNames)
                .chapterLocation(chapterLocation)
                .user(user)
                .build();

        // Parse date of birth if provided
        if (dateOfBirthStr != null && !dateOfBirthStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                child.setDateOfBirth(sdf.parse(dateOfBirthStr));
            } catch (ParseException e) {
                // If parsing fails, leave dateOfBirth as null
            }
        }

        return childRepository.save(child);
    }

    /**
     * Update an existing child's information.
     * 
     * @param childId         The child's ID
     * @param user            The parent user (for ownership validation)
     * @param name            Updated name
     * @param age             Updated age
     * @param dateOfBirthStr  Updated date of birth (yyyy-MM-dd)
     * @param hearingLossType Updated hearing loss type
     * @param equipmentType   Updated equipment type
     * @param siblingsNames   Updated siblings names
     * @param chapterLocation Updated chapter location
     * @return The updated child entity, or empty if not found or not owned by user
     */
    public Optional<Child> updateChild(int childId, User user, String name, Integer age, String dateOfBirthStr,
            String hearingLossType, String equipmentType, String siblingsNames, String chapterLocation) {

        Optional<Child> childOpt = childRepository.findById(childId);

        // Validate ownership
        if (childOpt.isEmpty() || childOpt.get().getUser().getId() != user.getId()) {
            return Optional.empty();
        }

        Child child = childOpt.get();
        child.setName(name);
        child.setAge(age);
        child.setHearingLossType(hearingLossType);
        child.setEquipmentType(equipmentType);
        child.setSiblingsNames(siblingsNames);
        child.setChapterLocation(chapterLocation);

        // Parse date of birth if provided
        if (dateOfBirthStr != null && !dateOfBirthStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                child.setDateOfBirth(sdf.parse(dateOfBirthStr));
            } catch (ParseException e) {
                // If parsing fails, keep existing dateOfBirth
            }
        }

        return Optional.of(childRepository.save(child));
    }

    /**
     * Delete a child if owned by the user.
     * 
     * @param childId The child's ID
     * @param user    The parent user (for ownership validation)
     * @return true if deleted, false if not found or not owned by user
     */
    public boolean deleteChild(int childId, User user) {
        Optional<Child> childOpt = childRepository.findById(childId);

        if (childOpt.isPresent() && childOpt.get().getUser().getId() == user.getId()) {
            childRepository.delete(childOpt.get());
            return true;
        }

        return false;
    }

    /**
     * Get a child by ID if owned by the user.
     * 
     * @param childId The child's ID
     * @param user    The parent user (for ownership validation)
     * @return The child if found and owned by user, empty otherwise
     */
    public Optional<Child> getChildByIdForUser(int childId, User user) {
        Optional<Child> childOpt = childRepository.findById(childId);

        if (childOpt.isPresent() && childOpt.get().getUser().getId() == user.getId()) {
            return childOpt;
        }

        return Optional.empty();
    }
}
