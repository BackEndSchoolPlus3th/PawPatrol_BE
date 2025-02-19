package com.patrol.api.member.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record EmailRequest(
    @NotBlank
    @Email
    String email
) {}

