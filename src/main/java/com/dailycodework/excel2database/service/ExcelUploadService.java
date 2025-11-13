package com.dailycodework.excel2database.service;

import com.dailycodework.excel2database.domain.Subject;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ExcelUploadService {

    public boolean isValidExcelFile(MultipartFile file) {
        return Objects.equals(file.getContentType(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    public List<Subject> getSubjectsDataFromExcel(InputStream inputStream) {
        List<Subject> subjects = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            XSSFSheet sheet = workbook.getSheetAt(0);

            // Start reading from row 1 (second row), skipping the first row (header)
            int rowIndex = 0;
            for (Row row : sheet) {
                if (rowIndex == 0) { // Skip the first row (header)
                    rowIndex++;
                    continue;
                }

                Iterator<Cell> cellIterator = row.iterator();
                Subject subject = new Subject();
                int cellIndex = 0;

                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();

                    switch (cellIndex) {
                        case 0 -> {
                            if (cell.getCellType() == CellType.NUMERIC) {
                                subject.setSno((int) cell.getNumericCellValue());  // Subject number
                            } else {
                                log.warn("Expected numeric value for sno but found a non-numeric value.");
                            }
                        }
                        case 1 -> {
                            if (cell.getCellType() == CellType.STRING) {
                                subject.setHtno(cell.getStringCellValue());  // Htno (roll number)
                            } else {
                                log.warn("Expected string value for htno but found a non-string value.");
                            }
                        }
                        case 2 -> {
                            if (cell.getCellType() == CellType.STRING) {
                                subject.setSubcode(cell.getStringCellValue());  // Subcode
                            } else {
                                log.warn("Expected string value for subcode but found a non-string value.");
                            }
                        }
                        case 3 -> {
                            if (cell.getCellType() == CellType.STRING) {
                                subject.setSubname(cell.getStringCellValue());  // Subname
                            } else {
                                log.warn("Expected string value for subname but found a non-string value.");
                            }
                        }
                        case 4 -> {
                            if (cell.getCellType() == CellType.NUMERIC) {
                                subject.setInternals((int) cell.getNumericCellValue());  // Internals
                            } else {
                                log.warn("Expected numeric value for internals but found a non-numeric value.");
                            }
                        }
                        case 5 -> {
                            if (cell.getCellType() == CellType.STRING) {
                                subject.setGrade(cell.getStringCellValue());  // Grade
                            } else {
                                log.warn("Expected string value for grade but found a non-string value.");
                            }
                        }
                        case 6 -> {
                            if (cell.getCellType() == CellType.NUMERIC) {
                                subject.setCredit((double) cell.getNumericCellValue());  // Credit
                            } else {
                                log.warn("Expected numeric value for credit but found a non-numeric value.");
                            }
                        }
                        default -> log.warn("Unexpected column index: {}", cellIndex);
                    }

                    cellIndex++;
                }

                // Skip subjects where sno is 0
                if (subject.getSno() != 0) {
                    subjects.add(subject);
                }

            }
        } catch (IOException e) {
            log.error("Error reading Excel file: {}", e.getMessage());
            throw new RuntimeException("Failed to process Excel file", e);
        }

        return subjects;
    }
}
