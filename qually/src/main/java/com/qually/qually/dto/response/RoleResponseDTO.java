package com.qually.qually.dto.response;

import com.qually.qually.models.enums.Department;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for a {@link com.qually.qually.models.Role}.
 *
 * <p>Includes the capability flags {@code canBeAudited} and
 * {@code canRaiseDispute} so consumers can determine role permissions
 * without querying the roles table separately.</p>
 */
@Getter
@Builder
public class RoleResponseDTO {
    private Integer    roleId;
    private String     roleName;
    private Department department;
    private Integer    hierarchyLevel;
    /** When true, users with this role may be selected as the member audited. */
    private Boolean    canBeAudited;
    /** When true, users with this role may formally raise disputes. */
    private Boolean    canRaiseDispute;
}