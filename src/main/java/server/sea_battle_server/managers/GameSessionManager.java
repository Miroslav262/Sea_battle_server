package server.sea_battle_server.managers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import server.sea_battle_server.models.GameSession;


import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameSessionManager {

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> playerToSession = new ConcurrentHashMap<>();


    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private final Random random = new Random();

    public void handleCreate(WebSocketSession creator) throws IOException {
        String code = generateCode();
        GameSession session = new GameSession(code, creator);

        sessions.put(code, session);
        playerToSession.put(creator.getId(), code);
        WebSocketMessage<?> message = json("invite", Map.of("code", code));
        System.out.println(message);
        creator.sendMessage(message);
    }


    public void handleInvite(WebSocketSession joiner, JsonNode json) throws IOException {
        String code = json.get("code").asText();
        GameSession session = sessions.get(code);

        if (session == null || session.getPlayer2() != null) {
            joiner.sendMessage(json("create", Map.of("status", "error")));
            return;
        }

        session.setPlayer2(joiner);
        playerToSession.put(joiner.getId(), code);

        WebSocketMessage<?> message = json("create", Map.of("status", "ok"));
        System.out.println(message);
        session.getPlayer2().sendMessage(message);
    }


    public void handleGameState(WebSocketSession player, JsonNode json) throws IOException {
        GameSession session = getSession(player);
        System.out.println("ships: " + json.get("ships"));
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

        WebSocketMessage<?> message = json("shot", Map.of(
                "col", json.get("col").asInt(),
                "row", json.get("row").asInt()
        ));

        System.out.println("shot message: " + message);
        enemy.sendMessage(message);
    }


    public void handleShotResult(WebSocketSession player, JsonNode json) throws IOException {
        GameSession session = getSession(player);
        WebSocketSession enemy = session.other(player);

        session.registerShotResult(enemy, json.get("result").asText());

        WebSocketMessage<?> message = json("shot-result", Map.of("result", json.get("result").asText()));
        System.out.println("shot-result" + message);

        enemy.sendMessage(message);

        if (session.isGameOver()) {
            sendGameResult(session);
        }
    }


    private void sendGameResult(GameSession session) throws IOException {
        WebSocketSession winner = session.getWinner();
        WebSocketSession loser = session.other(winner);

        WebSocketMessage<?> messegeToWin = json("gameResult", Map.of(
                "state", "win",
                "ships", session.getShips(loser)
        ));
        System.out.println("messageToWin: " + messegeToWin);
        winner.sendMessage(messegeToWin);

        WebSocketMessage<?> messegeToLose = json("gameResult", Map.of(
                "state", "lose",
                "ships", session.getShips(winner)
        ));
        System.out.println("messageToLose: " + messegeToLose);
        loser.sendMessage(messegeToLose);

        session.close();
    }


    private GameSession getSession(WebSocketSession player) {
        return sessions.get(playerToSession.get(player.getId()));
    }



    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
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
