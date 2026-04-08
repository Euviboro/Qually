package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDTO {
    private String userEmail;
    private String fullName;
    private String role;
}
