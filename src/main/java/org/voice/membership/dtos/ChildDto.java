package org.voice.membership.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Date;

@Data
public class ChildDto {

    @NotEmpty(message = "Child's name is required")
    private String name;

    private Integer age;

    @NotNull(message = "Date of birth is required")
    private Date dateOfBirth;

    private String hearingLossType;

    private String equipmentType;

    private String siblingsNames;

    private String chapterLocation;
}
