package com.qually.qually.dto.response;

import com.qually.qually.models.enums.Department;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDTO {
    private Integer userId;
    private String userEmail;
    private String fullName;
    private Integer roleId;
    private String roleName;
    private Department department;
    private Integer managerId;
    private String managerName;
}
