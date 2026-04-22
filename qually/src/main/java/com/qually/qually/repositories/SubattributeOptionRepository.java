package com.qually.qually.repositories;

import com.qually.qually.models.Subattribute;
import com.qually.qually.models.SubattributeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubattributeOptionRepository extends JpaRepository<SubattributeOption, Long> {
    List<SubattributeOption> findBySubattribute_SubattributeId(Integer subattributeId);
}