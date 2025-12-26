package com.CourtAssist.repository;

import com.CourtAssist.dto.NotificationDTO;
import com.CourtAssist.dto.NotificationMessageDTO;
import com.CourtAssist.model.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId")
    List<Notification> findByUserId(@Param("userId") Long userId);

    @Query("SELECT new com.CourtAssist.dto.NotificationDTO(" +
            "n.id, n.hearingId, n.caseRef, n.hearingDate, n.hearingTime, " +
            "n.court, n.stage, n.parties, n.advocates, " +
            "n.isRead, n.isSent, n.createdAt) " +
            "FROM Notification n WHERE n.user.userId = :userId")
    List<NotificationDTO> findDTOByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.userId = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

}