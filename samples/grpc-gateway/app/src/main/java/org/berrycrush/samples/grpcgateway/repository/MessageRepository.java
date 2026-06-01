package org.berrycrush.samples.grpcgateway.repository;

import org.berrycrush.samples.grpcgateway.model.Message;
import org.berrycrush.samples.grpcgateway.model.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderId(String senderId);
    List<Message> findByRecipientId(String recipientId);
    List<Message> findByStatus(MessageStatus status);
    List<Message> findByRecipientIdAndStatus(String recipientId, MessageStatus status);
}
