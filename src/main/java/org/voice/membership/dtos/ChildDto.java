package org.voice.membership.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import java.util.Date;

@Data
/**
 * Data transfer object for child information in the registration flow.
 * Mirrors the Child entity fields needed on registration and profile forms.
 */
public class ChildDto {

    @NotEmpty(message = "Child's name is required")
    private String name;

    private Integer age;

    @PastOrPresent(message = "Date of birth must be in the past or today")
    private Date dateOfBirth;

    private String hearingLossType;

    private String equipmentType;

    private String siblingsNames;

    private String chapterLocation;
}
