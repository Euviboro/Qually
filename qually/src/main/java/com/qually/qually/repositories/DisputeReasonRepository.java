package com.qually.qually.repositories;

import com.qually.qually.models.DisputeReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeReasonRepository extends JpaRepository<DisputeReason, Integer> {
}
