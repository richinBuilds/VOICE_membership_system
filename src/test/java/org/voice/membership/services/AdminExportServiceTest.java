package org.voice.membership.services;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AdminExportService
 * Tests Excel export functionality for users and children data
 */
class AdminExportServiceTest {

    private AdminExportService adminExportService;
    private List<User> testUsers;
    private SimpleDateFormat dateFormat;

    @BeforeEach
    void setUp() throws Exception {
        adminExportService = new AdminExportService();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Create test users
        User user1 = new User();
        user1.setId(1);
        user1.setFirstName("John");
        user1.setMiddleName("M");
        user1.setLastName("Doe");
        user1.setEmail("john@example.com");
        user1.setPhone("555-1234");
        user1.setAddress("123 Main St");
        user1.setCity("Toronto");
        user1.setProvince("ON");
        user1.setPostalCode("M1M1M1");
        user1.setRole("USER");
        user1.setCreation(dateFormat.parse("2024-01-15"));

        Child child1 = Child.builder()
                .id(101)
                .name("Child One")
                .age(5)
                .dateOfBirth(dateFormat.parse("2019-06-15"))
                .hearingLossType("Sensorineural")
                .equipmentType("Hearing Aid")
                .chapterLocation("Toronto")
                .siblingsNames("Sibling A")
                .user(user1)
                .build();
        user1.setChildren(new ArrayList<>(List.of(child1)));

        User user2 = new User();
        user2.setId(2);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setEmail("jane@example.com");
        user2.setPhone("555-5678");
        user2.setAddress("456 Oak Ave");
        user2.setCity("Vancouver");
        user2.setProvince("BC");
        user2.setPostalCode("V5V5V5");
        user2.setRole("USER");
        user2.setCreation(dateFormat.parse("2024-03-20"));

        Child child2a = Child.builder()
                .id(201)
                .name("Child Two A")
                .age(8)
                .dateOfBirth(dateFormat.parse("2016-03-10"))
                .hearingLossType("Conductive")
                .equipmentType("Cochlear Implant")
                .chapterLocation("Vancouver")
                .siblingsNames("Sibling B, Sibling C")
                .user(user2)
                .build();

        Child child2b = Child.builder()
                .id(202)
                .name("Child Two B")
                .age(6)
                .dateOfBirth(dateFormat.parse("2018-08-22"))
                .hearingLossType("Mixed")
                .equipmentType("FM System")
                .chapterLocation("Vancouver")
                .siblingsNames("Sibling B, Sibling C")
                .user(user2)
                .build();

        user2.setChildren(new ArrayList<>(Arrays.asList(child2a, child2b)));

        testUsers = new ArrayList<>(Arrays.asList(user1, user2));
    }

    // ==================== Export Users to Excel Tests ====================

    @Test
    void exportUsersToExcel_WithValidUsers_ShouldCreateWorkbook() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(testUsers, outputStream);

        // Assert
        assertThat(outputStream.size()).isGreaterThan(0);
    }

    @Test
    void exportUsersToExcel_ShouldCreateUsersSheet() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(testUsers, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet usersSheet = workbook.getSheet("Users");

        assertThat(usersSheet).isNotNull();
        workbook.close();
    }

    @Test
    void exportUsersToExcel_ShouldCreateChildrenSheet() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(testUsers, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet childrenSheet = workbook.getSheet("Children");

        assertThat(childrenSheet).isNotNull();
        workbook.close();
    }

    @Test
    void exportUsersToExcel_UsersSheet_ShouldHaveCorrectHeaders() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(testUsers, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet usersSheet = workbook.getSheet("Users");
        Row headerRow = usersSheet.getRow(0);

        assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("ID");
        assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("First Name");
        assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Middle Name");
        assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("Last Name");
        assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("Email");
        assertThat(headerRow.getCell(5).getStringCellValue()).isEqualTo("Phone");
        assertThat(headerRow.getCell(6).getStringCellValue()).isEqualTo("Address");
        assertThat(headerRow.getCell(7).getStringCellValue()).isEqualTo("City");
        assertThat(headerRow.getCell(8).getStringCellValue()).isEqualTo("Province");
        assertThat(headerRow.getCell(9).getStringCellValue()).isEqualTo("Postal Code");
        assertThat(headerRow.getCell(10).getStringCellValue()).isEqualTo("Role");
        assertThat(headerRow.getCell(11).getStringCellValue()).isEqualTo("Registration Date");
        assertThat(headerRow.getCell(12).getStringCellValue()).isEqualTo("Number of Children");

        workbook.close();
    }

    @Test
    void exportUsersToExcel_UsersSheet_ShouldHaveCorrectData() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(testUsers, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet usersSheet = workbook.getSheet("Users");

        // Check first user
        Row user1Row = usersSheet.getRow(1);
        assertThat(user1Row.getCell(0).getNumericCellValue()).isEqualTo(1);
        assertThat(user1Row.getCell(1).getStringCellValue()).isEqualTo("John");
        assertThat(user1Row.getCell(2).getStringCellValue()).isEqualTo("M");
        assertThat(user1Row.getCell(3).getStringCellValue()).isEqualTo("Doe");
        assertThat(user1Row.getCell(4).getStringCellValue()).isEqualTo("john@example.com");
        assertThat(user1Row.getCell(5).getStringCellValue()).isEqualTo("555-1234");
        assertThat(user1Row.getCell(7).getStringCellValue()).isEqualTo("Toronto");
        assertThat(user1Row.getCell(8).getStringCellValue()).isEqualTo("ON");
        assertThat(user1Row.getCell(10).getStringCellValue()).isEqualTo("USER");
        assertThat(user1Row.getCell(12).getNumericCellValue()).isEqualTo(1);

        // Check second user
        Row user2Row = usersSheet.getRow(2);
        assertThat(user2Row.getCell(0).getNumericCellValue()).isEqualTo(2);
        assertThat(user2Row.getCell(1).getStringCellValue()).isEqualTo("Jane");
        assertThat(user2Row.getCell(3).getStringCellValue()).isEqualTo("Smith");
        assertThat(user2Row.getCell(4).getStringCellValue()).isEqualTo("jane@example.com");
        assertThat(user2Row.getCell(12).getNumericCellValue()).isEqualTo(2);

        workbook.close();
    }

    @Test
    void exportUsersToExcel_ChildrenSheet_ShouldHaveCorrectHeaders() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(testUsers, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet childrenSheet = workbook.getSheet("Children");
        Row headerRow = childrenSheet.getRow(0);

        assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Child ID");
        assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Child Name");
        assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Age");
        assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("Date of Birth");
        assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("Hearing Loss Type");
        assertThat(headerRow.getCell(5).getStringCellValue()).isEqualTo("Equipment Type");
        assertThat(headerRow.getCell(6).getStringCellValue()).isEqualTo("Chapter Location");
        assertThat(headerRow.getCell(7).getStringCellValue()).isEqualTo("Siblings Names");
        assertThat(headerRow.getCell(8).getStringCellValue()).isEqualTo("Parent ID");
        assertThat(headerRow.getCell(9).getStringCellValue()).isEqualTo("Parent First Name");

        workbook.close();
    }

    @Test
    void exportUsersToExcel_ChildrenSheet_ShouldHaveCorrectData() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(testUsers, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet childrenSheet = workbook.getSheet("Children");

        // Check first child
        Row child1Row = childrenSheet.getRow(1);
        assertThat(child1Row.getCell(0).getNumericCellValue()).isEqualTo(101);
        assertThat(child1Row.getCell(1).getStringCellValue()).isEqualTo("Child One");
        assertThat(child1Row.getCell(2).getNumericCellValue()).isEqualTo(5);
        assertThat(child1Row.getCell(3).getStringCellValue()).isEqualTo("2019-06-15");
        assertThat(child1Row.getCell(4).getStringCellValue()).isEqualTo("Sensorineural");
        assertThat(child1Row.getCell(5).getStringCellValue()).isEqualTo("Hearing Aid");
        assertThat(child1Row.getCell(6).getStringCellValue()).isEqualTo("Toronto");
        assertThat(child1Row.getCell(8).getNumericCellValue()).isEqualTo(1);
        assertThat(child1Row.getCell(9).getStringCellValue()).isEqualTo("John");
        assertThat(child1Row.getCell(12).getStringCellValue()).isEqualTo("john@example.com");

        // Check second user's first child
        Row child2aRow = childrenSheet.getRow(2);
        assertThat(child2aRow.getCell(0).getNumericCellValue()).isEqualTo(201);
        assertThat(child2aRow.getCell(1).getStringCellValue()).isEqualTo("Child Two A");
        assertThat(child2aRow.getCell(2).getNumericCellValue()).isEqualTo(8);
        assertThat(child2aRow.getCell(8).getNumericCellValue()).isEqualTo(2);

        // Check second user's second child
        Row child2bRow = childrenSheet.getRow(3);
        assertThat(child2bRow.getCell(0).getNumericCellValue()).isEqualTo(202);
        assertThat(child2bRow.getCell(1).getStringCellValue()).isEqualTo("Child Two B");
        assertThat(child2bRow.getCell(2).getNumericCellValue()).isEqualTo(6);

        workbook.close();
    }

    @Test
    void exportUsersToExcel_WithEmptyUserList_ShouldCreateEmptySheets() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<User> emptyList = new ArrayList<>();

        // Act
        adminExportService.exportUsersToExcel(emptyList, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet usersSheet = workbook.getSheet("Users");
        Sheet childrenSheet = workbook.getSheet("Children");

        assertThat(usersSheet).isNotNull();
        assertThat(childrenSheet).isNotNull();
        assertThat(usersSheet.getLastRowNum()).isEqualTo(0); // Only header row
        assertThat(childrenSheet.getLastRowNum()).isEqualTo(0); // Only header row

        workbook.close();
    }

    @Test
    void exportUsersToExcel_WithUserWithoutChildren_ShouldOnlyExportUser() throws IOException {
        // Arrange
        User userWithoutChildren = new User();
        userWithoutChildren.setId(99);
        userWithoutChildren.setFirstName("NoKids");
        userWithoutChildren.setLastName("User");
        userWithoutChildren.setEmail("nokids@example.com");
        userWithoutChildren.setRole("USER");
        userWithoutChildren.setCreation(new Date());
        userWithoutChildren.setChildren(new ArrayList<>());

        List<User> users = List.of(userWithoutChildren);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(users, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet usersSheet = workbook.getSheet("Users");
        Sheet childrenSheet = workbook.getSheet("Children");

        assertThat(usersSheet.getLastRowNum()).isEqualTo(1); // Header + 1 user
        assertThat(childrenSheet.getLastRowNum()).isEqualTo(0); // Only header, no children

        workbook.close();
    }

    @Test
    void exportUsersToExcel_ShouldApplyBoldHeaderStyle() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(testUsers, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet usersSheet = workbook.getSheet("Users");
        Row headerRow = usersSheet.getRow(0);
        Cell headerCell = headerRow.getCell(0);
        CellStyle headerStyle = headerCell.getCellStyle();
        Font headerFont = workbook.getFontAt(headerStyle.getFontIndex());

        assertThat(headerFont.getBold()).isTrue();

        workbook.close();
    }

    @Test
    void exportUsersToExcel_WithNullFields_ShouldHandleGracefully() throws IOException {
        // Arrange
        User userWithNulls = new User();
        userWithNulls.setId(50);
        userWithNulls.setFirstName("Partial");
        userWithNulls.setLastName("User");
        userWithNulls.setEmail("partial@example.com");
        // Leave many fields as null
        userWithNulls.setCreation(new Date());
        userWithNulls.setChildren(new ArrayList<>());

        List<User> users = List.of(userWithNulls);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(users, outputStream);

        // Assert
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet usersSheet = workbook.getSheet("Users");
        Row dataRow = usersSheet.getRow(1);

        assertThat(dataRow.getCell(2).getStringCellValue()).isEmpty(); // Middle name
        assertThat(dataRow.getCell(5).getStringCellValue()).isEmpty(); // Phone
        assertThat(dataRow.getCell(6).getStringCellValue()).isEmpty(); // Address

        workbook.close();
    }

    @Test
    void exportUsersToExcel_ShouldAutoSizeColumns() throws IOException {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Act
        adminExportService.exportUsersToExcel(testUsers, outputStream);

        // Assert - Just verify it completes without error
        // Auto-sizing is applied during export
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet usersSheet = workbook.getSheet("Users");

        assertThat(usersSheet).isNotNull();
        workbook.close();
    }
}
