package com.github.im.server.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    // Store active WebSocket sessions
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // When a WebSocket message is received from the client
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        System.out.println("Received WebSocket message: " + payload);

        try {
            // Parse the incoming JSON message (you can use a library like Jackson to parse JSON)
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(payload);

            String type = jsonNode.get("type").asText();

            // Handling different types of signaling messages
            switch (type) {
                case "offer":
                    handleOffer(session, jsonNode);
                    break;
                case "answer":
                    handleAnswer(session, jsonNode);
                    break;
                case "ice-candidate":
                    handleIceCandidate(session, jsonNode);
                    break;
                default:
                    System.out.println("Unknown message type: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handle WebSocket connection open
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);  // Store the session ID
        System.out.println("WebSocket connection established: " + session.getId());
    }

    // Handle WebSocket connection close
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());  // Remove the session ID when connection closes
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    // Handle the offer message
    private void handleOffer(WebSocketSession session, JsonNode jsonNode) throws Exception {
        String offer = jsonNode.get("offer").asText();  // Assume offer is in the "offer" field
        System.out.println("Handling offer from " + session.getId());

        // Find the target session based on some logic (e.g., session ID or user ID)
        // For now, we'll assume it's a simple lookup, but you can extend this logic
        WebSocketSession targetSession = findTargetSession(session);
        if (targetSession != null) {
            JsonNode response = new ObjectMapper().createObjectNode()
                    .put("type", "offer")
                    .put("offer", offer);
            targetSession.sendMessage(new TextMessage(response.toString()));
        } else {
            session.sendMessage(new TextMessage("Target not connected"));
        }
    }

    // Handle the answer message
    private void handleAnswer(WebSocketSession session, JsonNode jsonNode) throws Exception {
        String answer = jsonNode.get("answer").asText();  // Assume answer is in the "answer" field
        System.out.println("Handling answer from " + session.getId());

        // Find the target session based on some logic (e.g., session ID or user ID)
        WebSocketSession targetSession = findTargetSession(session);
        if (targetSession != null) {
            JsonNode response = new ObjectMapper().createObjectNode()
                    .put("type", "answer")
                    .put("answer", answer);
            targetSession.sendMessage(new TextMessage(response.toString()));
        } else {
            session.sendMessage(new TextMessage("Target not connected"));
        }
    }

    // Handle ICE candidates
    private void handleIceCandidate(WebSocketSession session, JsonNode jsonNode) throws Exception {
        String candidate = jsonNode.get("candidate").asText();  // Assume candidate is in the "candidate" field
        String sdpMid = jsonNode.get("sdpMid").asText();  // SDP information
        int sdpMLineIndex = jsonNode.get("sdpMLineIndex").asInt();  // SDP information
        System.out.println("Handling ICE candidate from " + session.getId());

        // Find the target session based on some logic (e.g., session ID or user ID)
        WebSocketSession targetSession = findTargetSession(session);
        if (targetSession != null) {
            JsonNode response = new ObjectMapper().createObjectNode()
                    .put("type", "ice-candidate")
                    .put("candidate", candidate)
                    .put("sdpMid", sdpMid)
                    .put("sdpMLineIndex", sdpMLineIndex);
            targetSession.sendMessage(new TextMessage(response.toString()));
        } else {
            session.sendMessage(new TextMessage("Target not connected"));
        }
    }

    // Find target session based on your logic (e.g., session ID, username, etc.)
    private WebSocketSession findTargetSession(WebSocketSession session) {
        // Placeholder logic: In a real-world scenario, you'd probably need a more sophisticated way to find the target.
        // For now, return the first available session or use session ID.
        return sessions.values().stream().filter(s -> !s.getId().equals(session.getId())).findFirst().orElse(null);
    }
}
