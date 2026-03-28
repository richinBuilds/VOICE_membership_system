package org.voice.membership.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.voice.membership.dtos.ChildFormRequest;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;
import org.voice.membership.services.ChildService;
import org.voice.membership.services.CurrentUserService;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/profile/child")
@RequiredArgsConstructor
public class ProfileChildController {

    private final CurrentUserService currentUserService;
    private final ChildService childService;

    @GetMapping("/add")
    public String addChildForm(Model model, Principal principal) {
        User user = currentUserService.getCurrentUser(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("childForm", new ChildFormRequest());
        model.addAttribute("child", new Child());
        model.addAttribute("isEdit", false);
        return "editChild";
    }

    @PostMapping("/add")
    public String saveChild(
            @Valid @ModelAttribute("childForm") ChildFormRequest childForm,
            BindingResult bindingResult,
            Model model,
            Principal principal) {
        User user = currentUserService.getCurrentUser(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("child", new Child());
            model.addAttribute("isEdit", false);
            return "editChild";
        }

        childService.createChild(
                user,
                childForm.getName(),
                childForm.getAge(),
                childForm.getDateOfBirth(),
                childForm.getHearingLossType(),
                childForm.getEquipmentType(),
                childForm.getSiblingsNames(),
                childForm.getChapterLocation());

        return "redirect:/profile";
    }

    @GetMapping("/edit/{id}")
    public String editChild(@PathVariable("id") int id, Model model, Principal principal) {
        User user = currentUserService.getCurrentUser(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Child> childOpt = childService.getChildByIdForUser(id, user);
        if (childOpt.isEmpty()) {
            return "redirect:/profile";
        }

        Child child = childOpt.get();
        ChildFormRequest childForm = new ChildFormRequest();
        childForm.setName(child.getName());
        childForm.setAge(child.getAge());
        childForm.setHearingLossType(child.getHearingLossType());
        childForm.setEquipmentType(child.getEquipmentType());
        childForm.setSiblingsNames(child.getSiblingsNames());
        childForm.setChapterLocation(child.getChapterLocation());
        if (child.getDateOfBirth() != null) {
            childForm.setDateOfBirth(new java.text.SimpleDateFormat("yyyy-MM-dd").format(child.getDateOfBirth()));
        }

        model.addAttribute("child", child);
        model.addAttribute("childForm", childForm);
        model.addAttribute("isEdit", true);
        return "editChild";
    }

    @PostMapping("/edit/{id}")
    public String updateChild(
            @PathVariable("id") int id,
            @Valid @ModelAttribute("childForm") ChildFormRequest childForm,
            BindingResult bindingResult,
            Model model,
            Principal principal) {
        User user = currentUserService.getCurrentUser(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            Optional<Child> childOpt = childService.getChildByIdForUser(id, user);
            if (childOpt.isEmpty()) {
                return "redirect:/profile";
            }
            model.addAttribute("child", childOpt.get());
            model.addAttribute("isEdit", true);
            return "editChild";
        }

        Optional<Child> updatedChild = childService.updateChild(
                id,
                user,
                childForm.getName(),
                childForm.getAge(),
                childForm.getDateOfBirth(),
                childForm.getHearingLossType(),
                childForm.getEquipmentType(),
                childForm.getSiblingsNames(),
                childForm.getChapterLocation());

        if (updatedChild.isEmpty()) {
            return "redirect:/profile";
        }

        return "redirect:/profile";
    }

    @PostMapping("/delete/{id}")
    public String deleteChild(@PathVariable("id") int id, Principal principal) {
        User user = currentUserService.getCurrentUser(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        childService.deleteChild(id, user);
        return "redirect:/profile";
    }
}
