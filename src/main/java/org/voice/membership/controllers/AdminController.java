package org.voice.membership.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.voice.membership.entities.User;
import org.voice.membership.entities.Child;
import org.voice.membership.repositories.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
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
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String adminDashboard(
            Model model,
            Principal principal,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String hearingLossType,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        // Get admin info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();
        User admin = userRepository.findByEmail(adminEmail);

        model.addAttribute("adminName", admin != null ? admin.getName() : "Admin");
        model.addAttribute("adminEmail", adminEmail);

        // Get all users
        List<User> allUsers = userRepository.findAll();

        // Apply filters
        List<User> filteredUsers = filterUsers(allUsers, address, minAge, maxAge,
                hearingLossType, equipmentType,
                startDate, endDate);

        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("users", filteredUsers);

        // Add filter values back to model for form persistence
        model.addAttribute("address", address);
        model.addAttribute("minAge", minAge);
        model.addAttribute("maxAge", maxAge);
        model.addAttribute("hearingLossType", hearingLossType);
        model.addAttribute("equipmentType", equipmentType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin";
    }

    private List<User> filterUsers(List<User> users, String address, Integer minAge, Integer maxAge,
            String hearingLossType, String equipmentType,
            String startDate, String endDate) {
        return users.stream()
                .filter(user -> filterByAddress(user, address))
                .filter(user -> filterByChildAge(user, minAge, maxAge))
                .filter(user -> filterByHearingLossType(user, hearingLossType))
                .filter(user -> filterByEquipmentType(user, equipmentType))
                .filter(user -> filterByRegistrationDate(user, startDate, endDate))
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
                // Add one day to include the end date
                endDate = new Date(endDate.getTime() + 24 * 60 * 60 * 1000);
                if (userCreation.after(endDate)) {
                    return false;
                }
            }

            return true;
        } catch (ParseException e) {
            System.err.println("Error parsing date: " + e.getMessage());
            return true;
        }
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
        userDetails.put("name", user.getName());
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

        // Create workbook
        Workbook workbook = new XSSFWorkbook();

        // Create Users sheet
        Sheet usersSheet = workbook.createSheet("Users");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Users sheet headers
        Row userHeaderRow = usersSheet.createRow(0);
        String[] userColumns = { "ID", "Name", "Email", "Phone", "Address", "Postal Code", "Role", "Registration Date",
                "Number of Children" };

        for (int i = 0; i < userColumns.length; i++) {
            Cell cell = userHeaderRow.createCell(i);
            cell.setCellValue(userColumns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fill users data
        int userRowNum = 1;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (User user : users) {
            Row row = usersSheet.createRow(userRowNum++);

            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getName() != null ? user.getName() : "");
            row.createCell(2).setCellValue(user.getEmail() != null ? user.getEmail() : "");
            row.createCell(3).setCellValue(user.getPhone() != null ? user.getPhone() : "");
            row.createCell(4).setCellValue(user.getAddress() != null ? user.getAddress() : "");
            row.createCell(5).setCellValue(user.getPostalCode() != null ? user.getPostalCode() : "");
            row.createCell(6).setCellValue(user.getRole() != null ? user.getRole() : "USER");
            row.createCell(7).setCellValue(user.getCreation() != null ? dateFormat.format(user.getCreation()) : "");
            row.createCell(8).setCellValue(user.getChildren() != null ? user.getChildren().size() : 0);
        }

        // Auto-size columns for users sheet
        for (int i = 0; i < userColumns.length; i++) {
            usersSheet.autoSizeColumn(i);
        }

        // Create Children sheet
        Sheet childrenSheet = workbook.createSheet("Children");

        // Children sheet headers
        Row childHeaderRow = childrenSheet.createRow(0);
        String[] childColumns = { "Child ID", "Child Name", "Age", "Date of Birth", "Hearing Loss Type",
                "Equipment Type", "Chapter Location", "Siblings Names",
                "Parent ID", "Parent Name", "Parent Email", "Parent Phone" };

        for (int i = 0; i < childColumns.length; i++) {
            Cell cell = childHeaderRow.createCell(i);
            cell.setCellValue(childColumns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fill children data
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
                    row.createCell(9).setCellValue(user.getName() != null ? user.getName() : "");
                    row.createCell(10).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                    row.createCell(11).setCellValue(user.getPhone() != null ? user.getPhone() : "");
                }
            }
        }

        // Auto-size columns for children sheet
        for (int i = 0; i < childColumns.length; i++) {
            childrenSheet.autoSizeColumn(i);
        }

        // Set response headers
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=users_and_children_" + System.currentTimeMillis() + ".xlsx");

        // Write workbook to response
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
