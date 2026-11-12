package com.globaltechradar.repository;

import com.globaltechradar.domain.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findAllByOrderByProficiencyDescNameAsc();

    Optional<UserSkill> findFirstByNameIgnoreCase(String name);
}
