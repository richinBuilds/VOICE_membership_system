package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.ChildDto;
import org.voice.membership.dtos.MultiStepRegistrationDto;
import org.voice.membership.dtos.RegisterStep2Request;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationStep2Service {

    public void initializeChildrenIfNeeded(MultiStepRegistrationDto registrationData) {
        if (registrationData.getChildren() == null || registrationData.getChildren().isEmpty()) {
            registrationData.setChildren(new ArrayList<>());
            registrationData.getChildren().add(new ChildDto());
        }
    }

    public boolean isAddChildAction(RegisterStep2Request request) {
        return "addChild".equals(request.getAction());
    }

    public void addEmptyChild(MultiStepRegistrationDto registrationData) {
        if (registrationData.getChildren() == null) {
            registrationData.setChildren(new ArrayList<>());
        }
        registrationData.getChildren().add(new ChildDto());
    }

    public List<ChildDto> mapChildren(RegisterStep2Request request) {
        List<ChildDto> children = new ArrayList<>();
        List<String> names = request.getChildName();
        if (names == null || names.isEmpty()) {
            return children;
        }

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (name == null || name.trim().isEmpty()) {
                continue;
            }

            ChildDto child = new ChildDto();
            child.setName(name);

            if (request.getChildAge() != null && i < request.getChildAge().size()) {
                child.setAge(request.getChildAge().get(i));
            }

            if (request.getChildDob() != null && i < request.getChildDob().size()) {
                String dob = request.getChildDob().get(i);
                if (dob != null && !dob.isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        child.setDateOfBirth(sdf.parse(dob));
                    } catch (Exception ignored) {
                    }
                }
            }

            if (request.getHearingLossType() != null && i < request.getHearingLossType().size()) {
                child.setHearingLossType(request.getHearingLossType().get(i));
            }
            if (request.getEquipmentType() != null && i < request.getEquipmentType().size()) {
                child.setEquipmentType(request.getEquipmentType().get(i));
            }
            if (request.getSiblingsNames() != null && i < request.getSiblingsNames().size()) {
                child.setSiblingsNames(request.getSiblingsNames().get(i));
            }
            if (request.getChapterLocation() != null && i < request.getChapterLocation().size()) {
                child.setChapterLocation(request.getChapterLocation().get(i));
            }

            children.add(child);
        }
        return children;
    }
}
