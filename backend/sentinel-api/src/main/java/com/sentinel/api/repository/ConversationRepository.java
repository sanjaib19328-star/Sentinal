package com.sentinel.api.repository;

import com.sentinel.api.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Conversation> findByUserIdAndApplicationIdOrderByUpdatedAtDesc(Long userId, Long applicationId);

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId AND (" +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.metadataJson) LIKE LOWER(CONCAT('%', :query, '%'))" +
           ") ORDER BY c.updatedAt DESC")
    List<Conversation> searchByUserIdAndQuery(@Param("userId") Long userId, @Param("query") String query);

    void deleteByIdAndUserId(Long id, Long userId);
}
