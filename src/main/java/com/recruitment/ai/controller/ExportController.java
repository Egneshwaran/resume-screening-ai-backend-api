package com.recruitment.ai.controller;

import com.recruitment.ai.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping("/pdf")
    public ResponseEntity<InputStreamResource> exportPdf(@RequestParam(required = false) Long jobId) {
        ByteArrayInputStream bis = exportService.exportToPdf(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=screening_results.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    @GetMapping("/excel")
    public ResponseEntity<InputStreamResource> exportExcel(@RequestParam(required = false) Long jobId)
            throws IOException {
        ByteArrayInputStream bis = exportService.exportToExcel(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=screening_results.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(bis));
    }
}
