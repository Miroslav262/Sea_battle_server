package server.sea_battle_server.managers;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import server.sea_battle_server.models.GameSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameSessionManager {

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> playerToSession = new ConcurrentHashMap<>();


    public void handleCreate(WebSocketSession creator) throws IOException {
        String code = generateCode();
        GameSession session = new GameSession(code, creator);

        sessions.put(code, session);
        playerToSession.put(creator.getId(), code);

        creator.sendMessage(json("create", Map.of("status", "ok")));
    }


    public void handleInvite(WebSocketSession joiner, JsonNode json) throws IOException {
        String code = json.get("code").asText();
        GameSession session = sessions.get(code);

        if (session == null || session.getPlayer2() != null) {
            joiner.sendMessage(json("invite", Map.of("status", "error")));
            return;
        }

        session.setPlayer2(joiner);
        playerToSession.put(joiner.getId(), code);


        session.getPlayer1().sendMessage(json("invite", Map.of("code", code)));
        joiner.sendMessage(json("invite", Map.of("code", code)));
    }


    public void handleGameState(WebSocketSession player, JsonNode json) throws IOException {
        GameSession session = getSession(player);
        session.setShips(player, json.get("ships"));

        if (session.bothReady()) {
            WebSocketSession first = session.randomFirstPlayer();
            WebSocketSession second = session.other(first);

            first.sendMessage(json("gameState", Map.of("turn", "you")));
            second.sendMessage(json("gameState", Map.of("turn", "enemy")));
        }
    }


    public void handleShot(WebSocketSession player, JsonNode json) throws IOException {
        GameSession session = getSession(player);
        WebSocketSession enemy = session.other(player);

        enemy.sendMessage(json("shot", Map.of(
                "col", json.get("col").asInt(),
                "row", json.get("row").asInt()
        )));
    }


    public void handleShotResult(WebSocketSession player, JsonNode json) throws IOException {
        GameSession session = getSession(player);
        WebSocketSession enemy = session.other(player);

        session.registerShotResult(enemy, json.get("result").asText());

        enemy.sendMessage(json("shot-result", Map.of("result", json.get("result").asText())));

        if (session.isGameOver()) {
            sendGameResult(session);
        }
    }


    private void sendGameResult(GameSession session) throws IOException {
        WebSocketSession winner = session.getWinner();
        WebSocketSession loser = session.other(winner);

        winner.sendMessage(json("gameResult", Map.of(
                "state", "win",
                "ships", session.getShips(loser)
        )));

        loser.sendMessage(json("gameResult", Map.of(
                "state", "lose",
                "ships", session.getShips(winner)
        )));

        session.close();
    }


    private GameSession getSession(WebSocketSession player) {
        return sessions.get(playerToSession.get(player.getId()));
    }

    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    private WebSocketMessage<?> json(String type, Map<String, Object> fields) {
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("type", type);
        fields.forEach(node::putPOJO);
        return new TextMessage(node.toString());
    }

    public void removePlayer(WebSocketSession session) {
        String code = playerToSession.remove(session.getId());
        if (code != null) sessions.remove(code);
    }
}
