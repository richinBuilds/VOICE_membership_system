package org.voice.membership.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.voice.membership.dtos.AdminUpdateUserRequest;
import org.voice.membership.dtos.AdminAddMemberRequest;
import org.voice.membership.dtos.BulkEmailRequest;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.Membership;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.services.EmailSenderService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.validation.Valid;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
/**
 * Provides administrative dashboards, filtering, and export features.
 * Allows admins to view and export member data with various filters applied.
 */
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MembershipRepository membershipRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailSenderService emailSenderService;

    @GetMapping("/dashboard")
    public String adminDashboard(
            Model model,
            Principal principal,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String hearingLossType,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String paymentStatus) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();
        User admin = userRepository.findByEmail(adminEmail);

        String adminName = "Admin";
        if (admin != null) {
            adminName = admin.getFirstName() +
                    (admin.getMiddleName() != null && !admin.getMiddleName().isEmpty() ? " " + admin.getMiddleName()
                            : "")
                    +
                    " " + admin.getLastName();
        }
        model.addAttribute("adminName", adminName);
        model.addAttribute("adminEmail", adminEmail);

        List<User> allUsers = userRepository.findAll();

        List<User> filteredUsers = filterUsers(allUsers, address, city, province, minAge, maxAge,
                hearingLossType, equipmentType,
                startDate, endDate, paymentStatus);

        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("users", filteredUsers);

        model.addAttribute("address", address);
        model.addAttribute("city", city);
        model.addAttribute("province", province);
        model.addAttribute("minAge", minAge);
        model.addAttribute("maxAge", maxAge);
        model.addAttribute("hearingLossType", hearingLossType);
        model.addAttribute("equipmentType", equipmentType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("paymentStatus", paymentStatus);

        return "admin";
    }

    private List<User> filterUsers(List<User> users, String address, String city, String province,
            Integer minAge, Integer maxAge,
            String hearingLossType, String equipmentType,
            String startDate, String endDate, String paymentStatus) {
        return users.stream()
                .filter(user -> filterByAddress(user, address))
                .filter(user -> filterByCity(user, city))
                .filter(user -> filterByProvince(user, province))
                .filter(user -> filterByChildAge(user, minAge, maxAge))
                .filter(user -> filterByHearingLossType(user, hearingLossType))
                .filter(user -> filterByEquipmentType(user, equipmentType))
                .filter(user -> filterByRegistrationDate(user, startDate, endDate))
                .filter(user -> filterByPaymentStatus(user, paymentStatus))
                .collect(Collectors.toList());
    }

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

    private boolean filterByCity(User user, String city) {
        if (city == null || city.trim().isEmpty()) {
            return true;
        }
        String userCity = user.getCity();
        return userCity != null && userCity.toLowerCase().contains(city.toLowerCase());
    }

    private boolean filterByProvince(User user, String province) {
        if (province == null || province.trim().isEmpty()) {
            return true;
        }
        String userProvince = user.getProvince();
        return userProvince != null && userProvince.toLowerCase().contains(province.toLowerCase());
    }

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
                endDate = new Date(endDate.getTime() + 24 * 60 * 60 * 1000);
                if (userCreation.after(endDate)) {
                    return false;
                }
            }

            return true;
        } catch (ParseException e) {
            return true;
        }
    }

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

    @GetMapping("/user/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable Integer id) {
        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("id", user.getId());
        userDetails.put("firstName", user.getFirstName());
        userDetails.put("middleName", user.getMiddleName());
        userDetails.put("lastName", user.getLastName());
        userDetails.put("email", user.getEmail());
        userDetails.put("phone", user.getPhone());
        userDetails.put("address", user.getAddress());
        userDetails.put("postalCode", user.getPostalCode());
        userDetails.put("role", user.getRole() != null ? user.getRole() : "USER");
        userDetails.put("creation", user.getCreation());
        userDetails.put("children", user.getChildren());
        userDetails.put("membership", user.getMembership());

        return ResponseEntity.ok(userDetails);
    }

    @GetMapping("/export-users")
    public void exportUsersToExcel(HttpServletResponse response) throws IOException {
        List<User> users = userRepository.findAll();

        Workbook workbook = new XSSFWorkbook();

        Sheet usersSheet = workbook.createSheet("Users");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Users sheet headers
        Row userHeaderRow = usersSheet.createRow(0);
        String[] userColumns = { "ID", "First Name", "Middle Name", "Last Name", "Email", "Phone", "Address",
                "City", "Province", "Postal Code", "Role", "Registration Date",
                "Number of Children" };

        for (int i = 0; i < userColumns.length; i++) {
            Cell cell = userHeaderRow.createCell(i);
            cell.setCellValue(userColumns[i]);
            cell.setCellStyle(headerStyle);
        }

        int userRowNum = 1;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (User user : users) {
            Row row = usersSheet.createRow(userRowNum++);

            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getFirstName() != null ? user.getFirstName() : "");
            row.createCell(2).setCellValue(user.getMiddleName() != null ? user.getMiddleName() : "");
            row.createCell(3).setCellValue(user.getLastName() != null ? user.getLastName() : "");
            row.createCell(4).setCellValue(user.getEmail() != null ? user.getEmail() : "");
            row.createCell(5).setCellValue(user.getPhone() != null ? user.getPhone() : "");
            row.createCell(6).setCellValue(user.getAddress() != null ? user.getAddress() : "");
            row.createCell(7).setCellValue(user.getCity() != null ? user.getCity() : "");
            row.createCell(8).setCellValue(user.getProvince() != null ? user.getProvince() : "");
            row.createCell(9).setCellValue(user.getPostalCode() != null ? user.getPostalCode() : "");
            row.createCell(10).setCellValue(user.getRole() != null ? user.getRole() : "USER");
            row.createCell(11).setCellValue(user.getCreation() != null ? dateFormat.format(user.getCreation()) : "");
            row.createCell(12).setCellValue(user.getChildren() != null ? user.getChildren().size() : 0);
        }

        for (int i = 0; i < userColumns.length; i++) {
            usersSheet.autoSizeColumn(i);
        }

        Sheet childrenSheet = workbook.createSheet("Children");

        // Children sheet headers
        Row childHeaderRow = childrenSheet.createRow(0);
        String[] childColumns = { "Child ID", "Child Name", "Age", "Date of Birth", "Hearing Loss Type",
                "Equipment Type", "Chapter Location", "Siblings Names",
                "Parent ID", "Parent First Name", "Parent Middle Name", "Parent Last Name", "Parent Email",
                "Parent Phone" };

        for (int i = 0; i < childColumns.length; i++) {
            Cell cell = childHeaderRow.createCell(i);
            cell.setCellValue(childColumns[i]);
            cell.setCellStyle(headerStyle);
        }

        int childRowNum = 1;
        SimpleDateFormat dobFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (User user : users) {
            List<Child> children = user.getChildren();
            if (children != null && !children.isEmpty()) {
                for (Child child : children) {
                    Row row = childrenSheet.createRow(childRowNum++);

                    row.createCell(0).setCellValue(child.getId());
                    row.createCell(1).setCellValue(child.getName() != null ? child.getName() : "");
                    row.createCell(2).setCellValue(child.getAge() != null ? child.getAge() : 0);
                    row.createCell(3).setCellValue(
                            child.getDateOfBirth() != null ? dobFormat.format(child.getDateOfBirth()) : "");
                    row.createCell(4)
                            .setCellValue(child.getHearingLossType() != null ? child.getHearingLossType() : "");
                    row.createCell(5).setCellValue(child.getEquipmentType() != null ? child.getEquipmentType() : "");
                    row.createCell(6)
                            .setCellValue(child.getChapterLocation() != null ? child.getChapterLocation() : "");
                    row.createCell(7).setCellValue(child.getSiblingsNames() != null ? child.getSiblingsNames() : "");
                    row.createCell(8).setCellValue(user.getId());
                    row.createCell(9).setCellValue(user.getFirstName() != null ? user.getFirstName() : "");
                    row.createCell(10).setCellValue(user.getMiddleName() != null ? user.getMiddleName() : "");
                    row.createCell(11).setCellValue(user.getLastName() != null ? user.getLastName() : "");
                    row.createCell(12).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                    row.createCell(13).setCellValue(user.getPhone() != null ? user.getPhone() : "");
                }
            }
        }

        for (int i = 0; i < childColumns.length; i++) {
            childrenSheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=users_and_children_" + System.currentTimeMillis() + ".xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    /**
     * Display the edit member profile form
     */
    @GetMapping("/edit-member/{id}")
    public String showEditMemberForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id).orElse(null);
            
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Member not found");
                return "redirect:/admin/dashboard";
            }
            
            // Create DTO from user
            AdminUpdateUserRequest updateRequest = AdminUpdateUserRequest.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .province(user.getProvince())
                .postalCode(user.getPostalCode())
                .membershipId(user.getMembership() != null ? user.getMembership().getId() : null)
                .emailVerified(user.isEmailVerified())
                .accountLocked(user.isAccountLocked())
                .build();
            
            model.addAttribute("updateRequest", updateRequest);
            model.addAttribute("user", user);
            model.addAttribute("memberships", membershipRepository.findByActiveTrue());
            
            // Get admin info for header
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth.getName();
            User admin = userRepository.findByEmail(adminEmail);
            String adminName = "Admin";
            if (admin != null) {
                adminName = admin.getFirstName() +
                        (admin.getMiddleName() != null && !admin.getMiddleName().isEmpty() ? " " + admin.getMiddleName() : "")
                        + " " + admin.getLastName();
            }
            model.addAttribute("adminName", adminName);
            model.addAttribute("adminEmail", adminEmail);
            
            return "admin-edit-member";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading member profile");
            return "redirect:/admin/dashboard";
        }
    }
    
    /**
     * Process the edit member profile form
     */
    @PostMapping("/edit-member/{id}")
    public String updateMemberProfile(
            @PathVariable Integer id,
            @Valid @ModelAttribute("updateRequest") AdminUpdateUserRequest updateRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Load the user to be edited
            User user = userRepository.findById(id).orElse(null);
            
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Member not found or has been deleted");
                return "redirect:/admin/dashboard";
            }
            
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                model.addAttribute("updateRequest", updateRequest);
                model.addAttribute("user", user);
                model.addAttribute("memberships", membershipRepository.findByActiveTrue());
                
                // Get admin info for header
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String adminEmail = auth.getName();
                User admin = userRepository.findByEmail(adminEmail);
                String adminName = "Admin";
                if (admin != null) {
                    adminName = admin.getFirstName() +
                            (admin.getMiddleName() != null && !admin.getMiddleName().isEmpty() ? " " + admin.getMiddleName() : "")
                            + " " + admin.getLastName();
                }
                model.addAttribute("adminName", adminName);
                model.addAttribute("adminEmail", adminEmail);
                
                return "admin-edit-member";
            }
            
            // Check if email is being changed and if it conflicts with another user
            if (!user.getEmail().equalsIgnoreCase(updateRequest.getEmail())) {
                List<User> usersWithEmail = userRepository.findAllByEmailIgnoreCase(updateRequest.getEmail());
                boolean emailConflict = usersWithEmail.stream().anyMatch(u -> u.getId() != user.getId());
                
                if (emailConflict) {
                    bindingResult.addError(new FieldError("updateRequest", "email", 
                        "Email already exists. Please choose a different email."));
                    model.addAttribute("updateRequest", updateRequest);
                    model.addAttribute("user", user);
                    model.addAttribute("memberships", membershipRepository.findByActiveTrue());
                    
                    // Get admin info
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    String adminEmail = auth.getName();
                    User admin = userRepository.findByEmail(adminEmail);
                    String adminName = "Admin";
                    if (admin != null) {
                        adminName = admin.getFirstName() +
                                (admin.getMiddleName() != null && !admin.getMiddleName().isEmpty() ? " " + admin.getMiddleName() : "")
                                + " " + admin.getLastName();
                    }
                    model.addAttribute("adminName", adminName);
                    model.addAttribute("adminEmail", adminEmail);
                    
                    return "admin-edit-member";
                }
            }
            
            // Update user information
            user.setFirstName(updateRequest.getFirstName());
            user.setMiddleName(updateRequest.getMiddleName());
            user.setLastName(updateRequest.getLastName());
            user.setEmail(updateRequest.getEmail());
            user.setPhone(updateRequest.getPhone());
            user.setAddress(updateRequest.getAddress());
            user.setCity(updateRequest.getCity());
            user.setProvince(updateRequest.getProvince());
            user.setPostalCode(updateRequest.getPostalCode());
            
            // Update membership if changed
            if (updateRequest.getMembershipId() != null) {
                Membership membership = membershipRepository.findById(updateRequest.getMembershipId()).orElse(null);
                user.setMembership(membership);
            } else {
                user.setMembership(null);
            }
            
            // Update email verification status
            if (updateRequest.getEmailVerified() != null) {
                user.setEmailVerified(updateRequest.getEmailVerified());
            }
            
            // Update account locked status
            if (updateRequest.getAccountLocked() != null) {
                user.setAccountLocked(updateRequest.getAccountLocked());
                if (!updateRequest.getAccountLocked()) {
                    // If unlocking account, reset failed attempts
                    user.setFailedLoginAttempts(0);
                    user.setLockoutTime(null);
                }
            }
            
            // Save the updated user
            userRepository.save(user);
            
            redirectAttributes.addFlashAttribute("success", 
                "Member profile for " + user.getFirstName() + " " + user.getLastName() + " has been successfully updated");
            return "redirect:/admin/dashboard";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "System error occurred while updating the profile. Please try again.");
            return "redirect:/admin/dashboard";
        }
    }
    
    /**
     * Display the add member form for admin to manually add a new member.
     * 
     * @param model the model to add attributes
     * @return the add member page template name
     */
    @GetMapping("/add-member")
    public String showAddMemberForm(Model model, Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();
        User admin = userRepository.findByEmail(adminEmail);
        
        String adminName = "Admin";
        if (admin != null) {
            adminName = admin.getFirstName() + 
                    (admin.getMiddleName() != null && !admin.getMiddleName().isEmpty() ? " " + admin.getMiddleName() : "") + 
                    " " + admin.getLastName();
        }
        
        model.addAttribute("adminName", adminName);
        model.addAttribute("adminEmail", adminEmail);
        model.addAttribute("memberRequest", new AdminAddMemberRequest());
        model.addAttribute("memberships", membershipRepository.findAll());
        
        return "admin-add-member";
    }
    
    /**
     * Handle add member form submission.
     * Creates a new user account with the provided details.
     * 
     * @param memberRequest the validated member data
     * @param bindingResult validation results
     * @param redirectAttributes flash attributes for success/error messages
     * @return redirect to admin dashboard
     */
    @PostMapping("/add-member")
    public String addMember(
            @Valid @ModelAttribute("memberRequest") AdminAddMemberRequest memberRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model,
            Principal principal) {
        
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                StringBuilder errorMessage = new StringBuilder("Validation errors: ");
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
                }
                redirectAttributes.addFlashAttribute("error", errorMessage.toString());
                return "redirect:/admin/add-member";
            }
            
            // Check if email already exists
            User existingUser = userRepository.findByEmail(memberRequest.getEmail());
            if (existingUser != null) {
                redirectAttributes.addFlashAttribute("error", 
                    "A member with email " + memberRequest.getEmail() + " already exists in the system.");
                return "redirect:/admin/add-member";
            }
            
            // Create new user
            User newUser = User.builder()
                    .firstName(memberRequest.getFirstName())
                    .middleName(memberRequest.getMiddleName())
                    .lastName(memberRequest.getLastName())
                    .email(memberRequest.getEmail())
                    .password(passwordEncoder.encode(memberRequest.getPassword()))
                    .phone(memberRequest.getPhone())
                    .address(memberRequest.getAddress())
                    .postalCode(memberRequest.getPostalCode())
                    .role(Role.USER.name())
                    .emailVerified(memberRequest.getEmailVerified())
                    .accountLocked(memberRequest.getAccountLocked())
                    .creation(new Date())
                    .build();
            
            // Assign membership if selected
            if (memberRequest.getMembershipId() != null) {
                Membership membership = membershipRepository.findById(memberRequest.getMembershipId()).orElse(null);
                if (membership != null) {
                    newUser.setMembership(membership);
                }
            }
            
            userRepository.save(newUser);
            
            redirectAttributes.addFlashAttribute("success", 
                "Member " + newUser.getFirstName() + " " + newUser.getLastName() + " has been successfully added to the system.");
            return "redirect:/admin/dashboard";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "System error occurred while adding the member. Please try again.");
            return "redirect:/admin/add-member";
        }
    }
    
    /**
     * Handle member deletion.
     * Removes a member from the system after confirmation.
     * 
     * @param id the user ID to delete
     * @param redirectAttributes flash attributes for success/error messages
     * @return redirect to admin dashboard
     */
    @PostMapping("/delete-member/{id}")
    public String deleteMember(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userRepository.findById(id).orElse(null);
            
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", 
                    "Member not found. The member may have already been deleted.");
                return "redirect:/admin/dashboard";
            }
            
            // Prevent deleting admin accounts
            if (user.getRole() != null && user.getRole().equals(Role.ADMIN.name())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Cannot delete admin accounts. Please contact system administrator.");
                return "redirect:/admin/dashboard";
            }
            
            String memberName = user.getFirstName() + " " + user.getLastName();
            userRepository.delete(user);
            
            redirectAttributes.addFlashAttribute("success", 
                "Member " + memberName + " has been successfully deleted from the system.");
            return "redirect:/admin/dashboard";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "System error occurred while deleting the member. Please try again.");
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/send-bulk-email")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendBulkEmail(
            @RequestBody BulkEmailRequest request,
            Authentication authentication) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            // Get admin info
            String adminEmail = authentication.getName();
            User admin = userRepository.findByEmail(adminEmail);
            String adminName = "Admin";
            if (admin != null) {
                adminName = admin.getFirstName() + " " + admin.getLastName();
            }
            
            // Validate request
            if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
                response.put("status", "error");
                response.put("message", "No recipients selected");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Subject cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (request.getMessageBody() == null || request.getMessageBody().trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Message body cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int failureCount = 0;
            StringBuilder failedEmails = new StringBuilder();
            
            // Send email to each recipient
            for (Integer userId : request.getRecipientIds()) {
                try {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null && user.getEmail() != null) {
                        emailSenderService.sendCustomEmail(
                            user.getEmail(), 
                            request.getSubject(), 
                            request.getMessageBody(), 
                            adminName
                        );
                        successCount++;
                    } else {
                        failureCount++;
                        if (user != null) {
                            failedEmails.append(user.getFirstName()).append(" ")
                                .append(user.getLastName()).append(", ");
                        }
                    }
                } catch (Exception e) {
                    failureCount++;
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        failedEmails.append(user.getEmail()).append(", ");
                    }
                }
            }
            
            // Build response message
            String message = "Emails sent successfully to " + successCount + " recipient(s)";
            if (failureCount > 0) {
                message += ". Failed to send to " + failureCount + " recipient(s)";
                if (failedEmails.length() > 0) {
                    String failed = failedEmails.toString();
                    if (failed.endsWith(", ")) {
                        failed = failed.substring(0, failed.length() - 2);
                    }
                    message += ": " + failed;
                }
            }
            
            if (successCount == 0) {
                response.put("status", "error");
                response.put("message", "Failed to send emails to all recipients");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("status", "success");
            response.put("message", message);
            response.put("successCount", String.valueOf(successCount));
            response.put("failureCount", String.valueOf(failureCount));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Server error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
