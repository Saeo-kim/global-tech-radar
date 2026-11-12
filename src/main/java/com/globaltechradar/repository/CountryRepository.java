package com.globaltechradar.repository;

import com.globaltechradar.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CountryRepository extends JpaRepository<Country, String> {
    List<Country> findAllByOrderByTechIndexDesc();
}
