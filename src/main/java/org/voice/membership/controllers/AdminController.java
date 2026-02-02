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
/**
 * Provides administrative dashboards, filtering, and export features.
 * Allows admins to view and export member data with various filters applied.
 */
public class AdminController {

    @Autowired
    private UserRepository userRepository;

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
            @RequestParam(required = false) String endDate) {

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
                startDate, endDate);

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

        return "admin";
    }

    private List<User> filterUsers(List<User> users, String address, String city, String province,
            Integer minAge, Integer maxAge,
            String hearingLossType, String equipmentType,
            String startDate, String endDate) {
        return users.stream()
                .filter(user -> filterByAddress(user, address))
                .filter(user -> filterByCity(user, city))
                .filter(user -> filterByProvince(user, province))
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
}
