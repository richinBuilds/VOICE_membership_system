package org.voice.membership.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Service for exporting user and children data to Excel format.
 * Handles all Excel workbook creation, formatting, and data population.
 */
@Service
public class AdminExportService {

    /**
     * Export users and their children to an Excel workbook.
     * Creates two sheets: one for users and one for children with parent information.
     * 
     * @param users list of users to export
     * @param outputStream output stream to write the Excel file to
     * @throws IOException if an error occurs while writing to the output stream
     */
    public void exportUsersToExcel(List<User> users, OutputStream outputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // Create users sheet
        createUsersSheet(workbook, users);

        // Create children sheet
        createChildrenSheet(workbook, users);

        // Write to output stream and close workbook
        workbook.write(outputStream);
        workbook.close();
    }

    /**
     * Create and populate the users sheet with user data.
     * 
     * @param workbook the Excel workbook
     * @param users list of users to export
     */
    private void createUsersSheet(Workbook workbook, List<User> users) {
        Sheet usersSheet = workbook.createSheet("Users");

        CellStyle headerStyle = createHeaderStyle(workbook);

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

        // Auto-size columns
        for (int i = 0; i < userColumns.length; i++) {
            usersSheet.autoSizeColumn(i);
        }
    }

    /**
     * Create and populate the children sheet with children and parent data.
     * 
     * @param workbook the Excel workbook
     * @param users list of users whose children to export
     */
    private void createChildrenSheet(Workbook workbook, List<User> users) {
        Sheet childrenSheet = workbook.createSheet("Children");

        CellStyle headerStyle = createHeaderStyle(workbook);

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

        // Auto-size columns
        for (int i = 0; i < childColumns.length; i++) {
            childrenSheet.autoSizeColumn(i);
        }
    }

    /**
     * Create a bold header cell style for Excel headers.
     * 
     * @param workbook the Excel workbook
     * @return a CellStyle with bold font
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }
}
