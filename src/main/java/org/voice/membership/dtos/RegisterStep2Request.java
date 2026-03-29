package org.voice.membership.dtos;

import lombok.Data;

import java.util.List;

@Data
public class RegisterStep2Request {
    private List<String> childName;
    private List<Integer> childAge;
    private List<String> childDob;
    private List<String> hearingLossType;
    private List<String> equipmentType;
    private List<String> siblingsNames;
    private List<String> chapterLocation;
    private String action;
}
