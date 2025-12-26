package com.CourtAssist.repository;

import com.CourtAssist.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);
    Optional<Users> findByMobileNo(String mobileNo);
    Optional<Users> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByMobileNo(String mobileNo);
    boolean existsByUsername(String username);
    
    /**
     * Find user by either email or mobile number
     */
    @Query("SELECT u FROM Users u WHERE u.email = :contact OR u.mobileNo = :contact")
    Users findByContact(@Param("contact") String contact);

}