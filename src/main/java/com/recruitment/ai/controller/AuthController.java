package com.recruitment.ai.controller;

import com.recruitment.ai.dto.JwtResponse;
import com.recruitment.ai.dto.LoginRequest;
import com.recruitment.ai.dto.ResetPasswordRequest;
import com.recruitment.ai.dto.SignupRequest;
import com.recruitment.ai.entity.PasswordResetToken;
import com.recruitment.ai.entity.Role;
import com.recruitment.ai.entity.User;
import com.recruitment.ai.repository.PasswordResetTokenRepository;
import com.recruitment.ai.repository.UserRepository;
import com.recruitment.ai.service.EmailService;
import com.recruitment.ai.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordResetTokenRepository tokenRepository;

    @Autowired
    EmailService emailService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(item -> item.getAuthority().replace("ROLE_", ""))
                .orElse("HR");

        return ResponseEntity.ok(JwtResponse.builder()
                .token(jwt)
                .username(userDetails.getUsername())
                .role(role)
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("companyName", user.getCompanyName());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Successfully logged out");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Email is already in use!"));
        }

        User user = User.builder()
                .username(signUpRequest.getEmail())
                .email(signUpRequest.getEmail())
                .fullName(signUpRequest.getName())
                .companyName(signUpRequest.getCompany())
                .password(encoder.encode(signUpRequest.getPassword()))
                .role(Role.HR)
                .build();

        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Signup successful");
        response.put("role", "HR");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        System.out.println("DEBUG: Password reset request received for: " + email);
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Delete any existing tokens for this user
            tokenRepository.deleteByUser(user);

            // Create new token
            String tokenValue = UUID.randomUUID().toString();
            PasswordResetToken token = PasswordResetToken.builder()
                    .token(tokenValue)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(15))
                    .build();

            tokenRepository.save(token);

            // Send email
            String resetLink = "http://localhost:3001/reset-password?token=" + tokenValue;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        }

        // Return same message regardless of whether user exists for security
        return ResponseEntity
                .ok(Map.of("message", "If an account exists with this email, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(request.getToken());

        if (tokenOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid password reset token."));
        }

        PasswordResetToken token = tokenOptional.get();
        if (token.isExpired()) {
            tokenRepository.delete(token);
            return ResponseEntity.badRequest().body(Map.of("message", "Token has expired."));
        }

        User user = token.getUser();
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Clean up token after use
        tokenRepository.delete(token);

        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset."));
    }
}
