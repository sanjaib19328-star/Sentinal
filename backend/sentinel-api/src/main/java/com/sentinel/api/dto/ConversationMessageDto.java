package com.sentinel.api.dto;

import com.sentinel.api.model.MessageSender;

import java.time.Instant;

public class ConversationMessageDto {

    private Long id;
    private Long conversationId;
    private MessageSender sender;
    private String content;
    private String metadataJson;
    private Instant createdAt;

    public ConversationMessageDto() {}

    public ConversationMessageDto(Long id, Long conversationId, MessageSender sender, String content, String metadataJson, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.sender = sender;
        this.content = content;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public MessageSender getSender() {
        return sender;
    }

    public void setSender(MessageSender sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
