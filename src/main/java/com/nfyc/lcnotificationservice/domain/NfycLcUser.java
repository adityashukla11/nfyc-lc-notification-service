package com.nfyc.lcnotificationservice.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfycLcUser {
    @Null(message = "User id should not be set by user.")
    private UUID userId;
    @Email
    private String email;
    private String fullName;
    private String lcUsername;
}
