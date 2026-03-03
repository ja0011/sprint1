package com.example.socialapp.controller;

import com.example.socialapp.model.Message;
import com.example.socialapp.model.User;
import com.example.socialapp.repository.MessageRepository;
import com.example.socialapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
// REMOVED @CrossOrigin - using global CORS config instead
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MessageController(MessageRepository messageRepository, 
    UserRepository userRepository) {
    this.messageRepository = messageRepository;
    this.userRepository = userRepository;
    }

    @Autowired(required = false)
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
    }

    // Get all conversations for a user
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestParam Long userId) {
    try {
    System.out.println("[MessageController] Getting conversations for user: " + userId);
    List<Message> messages = messageRepository.findLastMessageForEachConversation(userId);
    
    // Get unique conversation partners
    Set<Long> conversationPartners = new HashSet<>();
    for (Message msg : messages) {
    if (msg.getSender().getId().equals(userId)) {
    conversationPartners.add(msg.getReceiver().getId());
    } else {
    conversationPartners.add(msg.getSender().getId());
    }
    }
    
    System.out.println("[MessageController] Found " + conversationPartners.size() + " conversations");
    return ResponseEntity.ok(conversationPartners);
    } catch (Exception e) {
    System.err.println("[MessageController] Error getting conversations: " + e.getMessage());
    e.printStackTrace();
    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    }

    // Get messages between two users
    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<?> getConversation(
    @PathVariable Long otherUserId,
    @RequestParam Long userId) {
    try {
    System.out.println("[MessageController] Getting conversation between " + userId + " and " + otherUserId);
    List<Message> messages = messageRepository.findConversationBetweenUsers(userId, otherUserId);
    System.out.println("[MessageController] Found " + messages.size() + " messages");
    
    // Format messages for frontend
    List<Map<String, Object>> formattedMessages = messages.stream()
    .map(msg -> {
    Map<String, Object> formatted = new HashMap<>();
    formatted.put("id", msg.getId());
    formatted.put("content", msg.getContent());
    formatted.put("senderId", msg.getSender().getId());
    formatted.put("receiverId", msg.getReceiver().getId());
    formatted.put("isSent", msg.getSender().getId().equals(userId));
    formatted.put("timestamp", formatTimestamp(msg.getCreatedAt()));
    return formatted;
    })
    .collect(Collectors.toList());
    
    return ResponseEntity.ok(formattedMessages);
    } catch (Exception e) {
    System.err.println("[MessageController] Error getting conversation: " + e.getMessage());
    e.printStackTrace();
    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    }

    // Send a message via REST API
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest request) {
    try {
    System.out.println("[MessageController] Sending message from " + request.getSenderId() + " to " + request.getReceiverId());
    
    // Get users
    Optional<User> senderOpt = userRepository.findById(request.getSenderId());
    Optional<User> receiverOpt = userRepository.findById(request.getReceiverId());
    
    if (!senderOpt.isPresent() || !receiverOpt.isPresent()) {
    System.err.println("[MessageController] Invalid user ID");
    return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID"));
    }
    
    User sender = senderOpt.get();
    User receiver = receiverOpt.get();
    
    Message message = new Message(sender, receiver, request.getContent());
    Message savedMessage = messageRepository.save(message);
    System.out.println("[MessageController] Message saved with ID: " + savedMessage.getId());
    
    // Send via WebSocket if available
    if (messagingTemplate != null) {
    try {
    messagingTemplate.convertAndSend(
    "/queue/messages/" + request.getReceiverId(),
    savedMessage
    );
    System.out.println("[MessageController] Message sent via WebSocket to receiver");
    } catch (Exception e) {
    System.err.println("[MessageController] WebSocket send failed: " + e.getMessage());
    }
    }
    
    // Return formatted message
    Map<String, Object> response = new HashMap<>();
    response.put("id", savedMessage.getId());
    response.put("content", savedMessage.getContent());
    response.put("senderId", savedMessage.getSender().getId());
    response.put("receiverId", savedMessage.getReceiver().getId());
    response.put("timestamp", formatTimestamp(savedMessage.getCreatedAt()));
    
    return ResponseEntity.ok(response);
    } catch (Exception e) {
    System.err.println("[MessageController] Error sending message: " + e.getMessage());
    e.printStackTrace();
    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    }

    // WebSocket message handler
    @MessageMapping("/chat")
    public void handleChatMessage(@Payload SendMessageRequest request) {
    try {
    System.out.println("[MessageController] WebSocket message from " + request.getSenderId() + " to " + request.getReceiverId());
    
    // Get users
    Optional<User> senderOpt = userRepository.findById(request.getSenderId());
    Optional<User> receiverOpt = userRepository.findById(request.getReceiverId());
    
    if (!senderOpt.isPresent() || !receiverOpt.isPresent()) {
    System.err.println("[MessageController] Invalid user ID in WebSocket message");
    return;
    }
    
    User sender = senderOpt.get();
    User receiver = receiverOpt.get();
    
    Message message = new Message(sender, receiver, request.getContent());
    Message savedMessage = messageRepository.save(message);
    System.out.println("[MessageController] WebSocket message saved with ID: " + savedMessage.getId());
    
    // Format message for WebSocket
    Map<String, Object> formattedMessage = new HashMap<>();
    formattedMessage.put("id", savedMessage.getId());
    formattedMessage.put("content", savedMessage.getContent());
    formattedMessage.put("senderId", savedMessage.getSender().getId());
    formattedMessage.put("receiverId", savedMessage.getReceiver().getId());
    formattedMessage.put("timestamp", formatTimestamp(savedMessage.getCreatedAt()));
    
    // Send to receiver
    if (messagingTemplate != null) {
    messagingTemplate.convertAndSend(
    "/queue/messages/" + request.getReceiverId(),
    formattedMessage
    );
    System.out.println("[MessageController] Sent to receiver queue");
    
    // Also send to sender for confirmation
    messagingTemplate.convertAndSend(
    "/queue/messages/" + request.getSenderId(),
    formattedMessage
    );
    System.out.println("[MessageController] Sent to sender queue");
    }
    } catch (Exception e) {
    System.err.println("[MessageController] Error handling chat message: " + e.getMessage());
    e.printStackTrace();
    }
    }

    private String formatTimestamp(Instant timestamp) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a")
    .withZone(ZoneId.systemDefault());
    return formatter.format(timestamp);
    }

    // Request DTO
    public static class SendMessageRequest {
    private Long senderId;
    private Long receiverId;
    private String content;

    public Long getSenderId() {
    return senderId;
    }

    public void setSenderId(Long senderId) {
    this.senderId = senderId;
    }

    public Long getReceiverId() {
    return receiverId;
    }

    public void setReceiverId(Long receiverId) {
    this.receiverId = receiverId;
    }

    public String getContent() {
    return content;
    }

    public void setContent(String content) {
    this.content = content;
    }
    }
}