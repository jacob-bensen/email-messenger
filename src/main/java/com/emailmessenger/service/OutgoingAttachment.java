package com.emailmessenger.service;

/**
 * A file to attach to an outgoing reply. Web-layer {@code MultipartFile}s are
 * converted to this so {@link ReplyService} stays free of servlet types and is
 * easy to unit-test.
 */
public record OutgoingAttachment(String filename, String contentType, byte[] content) {}
