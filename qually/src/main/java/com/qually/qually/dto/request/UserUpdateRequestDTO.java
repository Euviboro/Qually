package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserUpdateRequestDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private Integer roleId;
    private Integer managerId;
    private List<Integer> clientIds;
}
