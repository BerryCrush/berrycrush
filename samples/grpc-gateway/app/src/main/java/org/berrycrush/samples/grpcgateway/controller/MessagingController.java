package org.berrycrush.samples.grpcgateway.controller;

import org.berrycrush.samples.grpcgateway.dto.SendMessageRequest;
import org.berrycrush.samples.grpcgateway.model.Message;
import org.berrycrush.samples.grpcgateway.model.MessageStatus;
import org.berrycrush.samples.grpcgateway.repository.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST controller following gRPC-Gateway URL patterns.
 * 
 * gRPC-Gateway typically maps:
 * - rpc SendMessage(SendMessageRequest) returns (SendMessageResponse) 
 *   → POST /v1/messages:send
 * - rpc GetMessage(GetMessageRequest) returns (Message)
 *   → GET /v1/messages/{message_id}
 * - rpc ListMessages(ListMessagesRequest) returns (ListMessagesResponse)
 *   → GET /v1/messages
 */
@RestController
@RequestMapping("/v1/messaging")
public class MessagingController {
    
    private final MessageRepository repository;
    
    public MessagingController(MessageRepository repository) {
        this.repository = repository;
    }
    
    /**
     * SendMessage - Create and send a new message.
     * Maps to: rpc SendMessage(SendMessageRequest) returns (SendMessageResponse)
     */
    @PostMapping("/messages:send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody SendMessageRequest request) {
        Message message = new Message(
            request.senderId(),
            request.recipientId(),
            request.content()
        );
        if (request.priority() != null) {
            message.setPriority(request.priority());
        }
        message.setStatus(MessageStatus.SENT);
        Message saved = repository.save(message);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "messageId", saved.getId(),
            "timestamp", saved.getTimestamp().toString(),
            "status", saved.getStatus().name()
        ));
    }
    
    /**
     * GetMessage - Retrieve a message by ID.
     * Maps to: rpc GetMessage(GetMessageRequest) returns (Message)
     */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable Long messageId) {
        return repository.findById(messageId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * ListMessages - List messages with optional filters.
     * Maps to: rpc ListMessages(ListMessagesRequest) returns (ListMessagesResponse)
     */
    @GetMapping("/messages")
    public Map<String, Object> listMessages(
            @RequestParam(required = false) String recipientId,
            @RequestParam(required = false) String senderId,
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(required = false) String pageToken) {
        
        List<Message> messages;
        if (recipientId != null && status != null) {
            messages = repository.findByRecipientIdAndStatus(recipientId, status);
        } else if (recipientId != null) {
            messages = repository.findByRecipientId(recipientId);
        } else if (senderId != null) {
            messages = repository.findBySenderId(senderId);
        } else if (status != null) {
            messages = repository.findByStatus(status);
        } else {
            messages = repository.findAll();
        }
        
        // Simulate pagination (simplified)
        int size = Math.min(messages.size(), pageSize);
        List<Message> page = messages.subList(0, size);
        
        return Map.of(
            "messages", page,
            "nextPageToken", messages.size() > pageSize ? "next" : ""
        );
    }
    
    /**
     * UpdateMessageStatus - Update the delivery status of a message.
     * Maps to: rpc UpdateMessageStatus(UpdateStatusRequest) returns (Message)
     */
    @PostMapping("/messages/{messageId}:updateStatus")
    public Message updateMessageStatus(
            @PathVariable Long messageId,
            @RequestBody Map<String, String> request) {
        Message message = repository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Message not found: " + messageId));
        
        String statusStr = request.get("status");
        if (statusStr != null) {
            message.setStatus(MessageStatus.valueOf(statusStr));
        }
        return repository.save(message);
    }
    
    /**
     * BatchSendMessages - Send multiple messages in a batch.
     * Maps to: rpc BatchSendMessages(BatchSendRequest) returns (BatchSendResponse)
     */
    @PostMapping("/messages:batchSend")
    public Map<String, Object> batchSendMessages(@RequestBody Map<String, List<SendMessageRequest>> request) {
        List<SendMessageRequest> requests = request.get("messages");
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No messages provided");
        }
        
        List<Long> messageIds = requests.stream()
            .map(req -> {
                Message message = new Message(req.senderId(), req.recipientId(), req.content());
                if (req.priority() != null) {
                    message.setPriority(req.priority());
                }
                message.setStatus(MessageStatus.SENT);
                return repository.save(message).getId();
            })
            .toList();
        
        return Map.of(
            "messageIds", messageIds,
            "successCount", messageIds.size()
        );
    }
    
    /**
     * DeleteMessage - Delete a message.
     * Maps to: rpc DeleteMessage(DeleteMessageRequest) returns (google.protobuf.Empty)
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        Message message = repository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Message not found: " + messageId));
        repository.delete(message);
        return ResponseEntity.noContent().build();
    }
}
