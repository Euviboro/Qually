package com.qually.qually.dto.response;

import com.qually.qually.models.enums.Department;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response DTO for a {@link com.qually.qually.models.User}.
 *
 * <p>{@code canBeAudited} and {@code canRaiseDispute} are surfaced here
 * so the frontend can use them directly for permission checks (e.g. whether
 * to show the Dispute button) without having to re-implement the role flag
 * logic in JavaScript.</p>
 */
@Getter
@Builder
public class UserResponseDTO {
    private Integer userId;
    private String userEmail;
    private String fullName;
    private Integer roleId;
    private String roleName;
    private Department department;
    private Integer hierarchyLevel;
    /** Derived from {@code role.canBeAudited}. */
    private Boolean canBeAudited;
    /** Derived from {@code role.canRaiseDispute}. */
    private Boolean canRaiseDispute;
    private Integer managerId;
    private String managerName;
    private List<Integer> clientIds;
    private Boolean isActive;
}
