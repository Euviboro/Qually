package com.qually.qually.repositories;

import com.qually.qually.models.Subattribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubattributeRepository extends JpaRepository<Subattribute, Integer> {
        List<Subattribute> findByAuditQuestion_QuestionId(Integer questionId);
}
