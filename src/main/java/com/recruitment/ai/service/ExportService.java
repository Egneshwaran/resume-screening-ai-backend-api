package com.recruitment.ai.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.recruitment.ai.entity.MatchingScore;
import com.recruitment.ai.repository.MatchingScoreRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExportService {

    @Autowired
    private MatchingScoreRepository matchingScoreRepository;

    public ByteArrayInputStream exportToPdf(Long jobId) {
        List<MatchingScore> scores;
        if (jobId != null) {
            scores = matchingScoreRepository.findByJob_IdOrderByTotalScoreDesc(jobId);
        } else {
            scores = matchingScoreRepository.findAll();
        }

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontTitle.setSize(18);
            fontTitle.setColor(Color.BLUE);

            Paragraph title = new Paragraph("Resume Screening Results", fontTitle);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3, 3, 2, 2, 2 });

            String[] pdfHeaders = { "Candidate", "Job Role", "Match Score", "Skills", "Status" };
            for (String header : pdfHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (MatchingScore score : scores) {
                table.addCell(score.getResume().getCandidateName());
                table.addCell(score.getJob().getTitle());
                table.addCell(String.format("%.1f%%", score.getTotalScore()));
                table.addCell(String.format("%.1f%%", score.getSkillMatchScore()));
                table.addCell(score.getStatus());
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream exportToExcel(Long jobId) throws IOException {
        List<MatchingScore> scores;
        if (jobId != null) {
            scores = matchingScoreRepository.findByJob_IdOrderByTotalScoreDesc(jobId);
        } else {
            scores = matchingScoreRepository.findAll();
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Screening Results");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont font = ((XSSFWorkbook) workbook).createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 12);
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);

            // Create Headers
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] excelHeaders = { "Candidate Name", "Email", "Job Title", "Total Score", "Skill Score",
                    "Experience Score", "Status" };
            for (int i = 0; i < excelHeaders.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(excelHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create Data Rows
            int rowIdx = 1;
            for (MatchingScore score : scores) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(score.getResume().getCandidateName());
                row.createCell(1).setCellValue(score.getResume().getCandidateEmail());
                row.createCell(2).setCellValue(score.getJob().getTitle());
                row.createCell(3).setCellValue(score.getTotalScore());
                row.createCell(4).setCellValue(score.getSkillMatchScore());
                row.createCell(5).setCellValue(score.getExperienceScore());
                row.createCell(6).setCellValue(score.getStatus());
            }

            // Auto-size columns
            for (int i = 0; i < excelHeaders.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
