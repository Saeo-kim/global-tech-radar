package com.globaltechradar.repository;

import com.globaltechradar.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, String> {
    List<Company> findAllByOrderByNameAsc();
}
