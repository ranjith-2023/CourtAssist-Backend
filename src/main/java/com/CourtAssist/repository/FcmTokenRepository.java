package com.CourtAssist.repository;

import com.CourtAssist.model.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    @Query("SELECT f FROM FcmToken f WHERE f.userId = :userId")
    List<FcmToken> getTokensByUserId(@Param("userId") Long userId);

    FcmToken findByUserId(Long userId);
}