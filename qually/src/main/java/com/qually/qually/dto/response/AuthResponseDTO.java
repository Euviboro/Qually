package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * User display data returned by {@code GET /api/auth/me}.
 *
 * <p>Tokens are in httpOnly cookies — not in this payload.
 * {@code forcePinChange} removed: PINs no longer exist.</p>
 */
@Getter
@Builder
public class AuthResponseDTO {
    private Integer userId;
    private String  fullName;
    private String  roleName;
    private String  department;
    private boolean canBeAudited;
    private boolean canRaiseDispute;
}