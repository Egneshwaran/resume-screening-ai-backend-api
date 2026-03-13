package com.recruitment.ai.controller;

import com.recruitment.ai.entity.Settings;
import com.recruitment.ai.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SettingsController {

    @Autowired
    private SettingsRepository settingsRepository;

    @GetMapping
    public Settings getSettings() {
        List<Settings> allSettings = settingsRepository.findAll();
        if (allSettings.isEmpty()) {
            return settingsRepository.save(new Settings());
        }
        return allSettings.get(0);
    }

    @PutMapping
    public Settings updateSettings(@RequestBody Settings updatedSettings) {
        List<Settings> allSettings = settingsRepository.findAll();
        Settings existing;
        if (allSettings.isEmpty()) {
            existing = new Settings();
        } else {
            existing = allSettings.get(0);
        }
        
        // Preserve ID to ensure update instead of insert if id was missing in request
        updatedSettings.setId(existing.getId());
        Settings saved = settingsRepository.save(updatedSettings);
        System.out.println("Settings updated successfully: " + saved.getId());
        return saved;
    }
}
