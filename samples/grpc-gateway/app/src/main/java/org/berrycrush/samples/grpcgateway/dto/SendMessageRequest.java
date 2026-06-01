package org.berrycrush.samples.grpcgateway.dto;

/**
 * Request message for SendMessage RPC.
 * Follows gRPC-Gateway JSON mapping conventions.
 */
public record SendMessageRequest(
    String senderId,
    String recipientId,
    String content,
    Integer priority
) {}
