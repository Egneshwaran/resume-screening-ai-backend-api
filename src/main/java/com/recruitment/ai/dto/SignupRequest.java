package com.recruitment.ai.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String name;
    private String email;
    private String company;
    private String password;
}
