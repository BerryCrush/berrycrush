package org.berrycrush.samples.grpcgateway.model;

/**
 * Message status enum following gRPC/protobuf enum conventions.
 */
public enum MessageStatus {
    MESSAGE_STATUS_UNSPECIFIED,
    PENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
