package org.callistotech.rhea.repository;

import org.callistotech.rhea.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUserName(String userName);

    boolean existsByUserName(String userName);
}
