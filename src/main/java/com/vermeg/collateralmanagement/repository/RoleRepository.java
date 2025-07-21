package com.vermeg.collateralmanagement.repository;

import com.vermeg.collateralmanagement.entity.Role;
import com.vermeg.collateralmanagement.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Optional<Role> findByType(RoleType type);

    List<Role> findByIsActiveTrue();

    boolean existsByName(String name);
}