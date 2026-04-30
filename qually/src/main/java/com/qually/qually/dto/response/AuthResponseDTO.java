package com.qually.qually.dto.response;

import com.qually.qually.models.enums.Department;
import lombok.Builder;
import lombok.Getter;

/**
 * Response body for {@code POST /api/auth/login} and
 * {@code POST /api/auth/refresh}.
 *
 * <p>The access and refresh tokens are NOT included here — they are sent
 * as {@code httpOnly} cookies by {@link com.qually.qually.controllers.AuthController}.
 * This body carries only the information the frontend needs to render the
 * UI correctly (name, role, department, flag for PIN change).</p>
 *
 * <p>When Microsoft Auth replaces the PIN login, {@code forcePinChange}
 * is removed from this DTO.</p>
 */
@Getter
@Builder
public class AuthResponseDTO {
    private Integer    userId;
    private String     fullName;
    private String     roleName;
    private Department department;
    private Boolean    canBeAudited;
    private Boolean    canRaiseDispute;
    /** When true the frontend must redirect to /change-pin before the dashboard. */
    private Boolean    forcePinChange;
}