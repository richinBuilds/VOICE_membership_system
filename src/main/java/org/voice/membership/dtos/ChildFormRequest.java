package org.voice.membership.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChildFormRequest {

    @NotBlank(message = "Child name is required")
    private String name;

    private Integer age;
    private String dateOfBirth;
    private String hearingLossType;
    private String equipmentType;
    private String siblingsNames;
    private String chapterLocation;
}
