package com.qually.qually.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "user_email", length = 150)
    private String userEmail;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "role", length = 50)
    private String role;
}