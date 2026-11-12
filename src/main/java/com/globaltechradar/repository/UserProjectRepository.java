package com.globaltechradar.repository;

import com.globaltechradar.domain.UserProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {
    List<UserProject> findAllByOrderByCreatedAtDesc();
}
