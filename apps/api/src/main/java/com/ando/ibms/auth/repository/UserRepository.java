package com.ando.ibms.auth.repository;

import com.ando.ibms.auth.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link User} persistence operations. */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Finds a user by their unique email address. */
    Optional<User> findByEmail(String email);

    /** Checks whether a user with the given email already exists. */
    boolean existsByEmail(String email);
}
