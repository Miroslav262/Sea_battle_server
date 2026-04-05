package server.sea_battle_server.handlers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import server.sea_battle_server.managers.GameSessionManager;


public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameSessionManager sessionManager = new GameSessionManager();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Client connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = mapper.readTree(message.getPayload());
        System.out.println(json.asText());
        String type = json.get("type").asText();

        switch (type) {
            case "create" -> sessionManager.handleCreate(session);
            case "invite" -> sessionManager.handleInvite(session, json);
            case "gameState" -> sessionManager.handleGameState(session, json);
            case "shot" -> sessionManager.handleShot(session, json);
            case "shot-result" -> sessionManager.handleShotResult(session, json);
            default -> System.out.println("Unknown message type: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        System.out.println("Client disconnected: " + session.getId());
        sessionManager.removePlayer(session);

    }
}
