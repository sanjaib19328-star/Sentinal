package com.sentinel.api.repository;

import com.sentinel.api.model.ConversationMessage;
import com.sentinel.api.model.MessageSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    List<ConversationMessage> findByConversationIdAndSender(Long conversationId, MessageSender sender);

    void deleteByConversationId(Long conversationId);
}
