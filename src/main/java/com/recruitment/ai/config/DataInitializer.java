package com.recruitment.ai.config;

import com.recruitment.ai.entity.Role;
import com.recruitment.ai.entity.User;
import com.recruitment.ai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create default Admin if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@recruiter.com")
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println("Default Admin user created: admin / admin123");
        }

        // Create default HR if not exists
        if (!userRepository.existsByUsername("hr_user")) {
            User hr = User.builder()
                    .username("hr_user")
                    .password(passwordEncoder.encode("hr123"))
                    .email("hr@recruiter.com")
                    .role(Role.HR)
                    .build();
            userRepository.save(hr);
            System.out.println("Default HR user created: hr_user / hr123");
        }
    }
}
