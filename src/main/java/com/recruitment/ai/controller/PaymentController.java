package com.recruitment.ai.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.recruitment.ai.entity.Payment;
import com.recruitment.ai.entity.Subscription;
import com.recruitment.ai.entity.User;
import com.recruitment.ai.repository.PaymentRepository;
import com.recruitment.ai.repository.SubscriptionRepository;
import com.recruitment.ai.repository.UserRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) {
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            int amountInRupees = Integer.parseInt(data.get("amount").toString().replaceAll("[^0-9]", ""));
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInRupees * 100); 
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            Order order = razorpay.orders.create(orderRequest);

            // Record initial payment entry
            Object principal = SecurityContextHolder.getContext().getAuthentication() != null ? 
                    SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
            String email = null;
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else if (data.containsKey("email")) {
                email = data.get("email").toString();
            }
            
            final String finalEmail = email;
            User user = (finalEmail != null) ? userRepository.findByEmail(finalEmail).orElse(null) : null;

            // If user doesn't exist in local DB (e.g. signed up via Supabase), create them
            if (user == null && finalEmail != null) {
                user = User.builder()
                        .username(finalEmail)
                        .email(finalEmail)
                        .fullName(data.get("name") != null ? data.get("name").toString() : finalEmail.split("@")[0])
                        .companyName(data.get("company") != null ? data.get("company").toString() : "")
                        .password("EXT_AUTH_" + System.currentTimeMillis()) // Random password for external auth
                        .role(com.recruitment.ai.entity.Role.HR)
                        .build();
                user = userRepository.save(user);
            }

            Payment payment = Payment.builder()
                    .userId(user != null ? user.getId() : null)
                    .razorpayOrderId(order.get("id"))
                    .amount((double) amountInRupees)
                    .currency("INR")
                    .status("CREATED")
                    .planName(data.get("planName").toString())
                    .build();
            
            paymentRepository.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("id", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("keyId", keyId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data) {
        String orderId = data.get("razorpay_order_id");
        String paymentId = data.get("razorpay_payment_id");
        String signature = data.get("razorpay_signature");

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                // Update payment record
                Payment payment = paymentRepository.findByRazorpayOrderId(orderId).orElse(null);
                if (payment != null) {
                    payment.setRazorpayPaymentId(paymentId);
                    payment.setRazorpaySignature(signature);
                    payment.setStatus("SUCCESS");
                    paymentRepository.save(payment);

                    // Create/Update Subscription
                    if (payment.getUserId() != null) {
                        // Deactivate current active subscription for this user
                        subscriptionRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(payment.getUserId(), "ACTIVE")
                            .ifPresent(sub -> {
                                sub.setStatus("EXPIRED");
                                subscriptionRepository.save(sub);
                            });

                        int limit = 50; // default for Starter
                        if (payment.getPlanName().equalsIgnoreCase("Recruiter")) {
                            limit = 500;
                        } else if (payment.getPlanName().equalsIgnoreCase("Enterprise")) {
                            limit = 999999;
                        }

                        Subscription subscription = Subscription.builder()
                                .userId(payment.getUserId())
                                .planName(payment.getPlanName())
                                .status("ACTIVE")
                                .resumeLimit(limit)
                                .resumesUsed(0)
                                .startDate(java.time.LocalDateTime.now())
                                .endDate(java.time.LocalDateTime.now().plusDays(30))
                                .build();
                        
                        subscriptionRepository.save(subscription);
                    }
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Payment verified and subscription activated");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(400).body(Map.of("error", "Invalid signature"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Verification process failed: " + e.getMessage()));
        }
    }

    @GetMapping("/subscription/active")
    public ResponseEntity<?> getActiveSubscription(@RequestParam(required = false) String email) {
        try {
            String userEmail = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
            } else if (email != null && !email.isEmpty()) {
                userEmail = email;
            }

            if (userEmail == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Subscription subscription = subscriptionRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "ACTIVE")
                    .orElse(null);

            if (subscription == null) {
                return ResponseEntity.ok(null);
            }

            // Check if subscription has expired
            if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(java.time.LocalDateTime.now())) {
                subscription.setStatus("EXPIRED");
                subscriptionRepository.save(subscription);
                return ResponseEntity.ok(null);
            }

            // Return keys that match what the frontend currently expects (snake_case from Supabase sync)
            Map<String, Object> response = new HashMap<>();
            response.put("id", subscription.getId());
            response.put("plan_name", subscription.getPlanName());
            response.put("status", subscription.getStatus());
            response.put("resume_limit", subscription.getResumeLimit());
            response.put("resumes_used", subscription.getResumesUsed());
            response.put("start_date", subscription.getStartDate());
            response.put("end_date", subscription.getEndDate());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
