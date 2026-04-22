package com.qually.qually.repositories;

import com.qually.qually.models.Role;
import com.qually.qually.models.enums.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(String roleName);
    List<Role> findByDepartment(Department department);
}
