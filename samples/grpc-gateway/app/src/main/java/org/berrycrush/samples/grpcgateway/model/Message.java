package org.berrycrush.samples.grpcgateway.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Message entity following gRPC-style message structure.
 * Field names follow protobuf/gRPC naming conventions (snake_case in proto, camelCase in Java).
 */
@Entity
@Table(name = "messages")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String senderId;
    
    @Column(nullable = false)
    private String recipientId;
    
    @Column(nullable = false, length = 1000)
    private String content;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;
    
    @Column
    private Integer priority;
    
    public Message() {
        this.timestamp = Instant.now();
        this.status = MessageStatus.PENDING;
        this.priority = 0;
    }
    
    public Message(String senderId, String recipientId, String content) {
        this();
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
