package server.sea_battle_server.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;

public class GameSession {

    @Getter
    private final String code;

    @Getter
    private final WebSocketSession player1;

    @Getter
    @Setter
    private WebSocketSession player2;

    private JsonNode ships1;
    private JsonNode ships2;

    private int hp1 = 20;
    private int hp2 = 20;

    public GameSession(String code, WebSocketSession player1) {
        this.code = code;
        this.player1 = player1;
    }

    public void setShips(WebSocketSession player, JsonNode ships) {
        if (player == player1) ships1 = ships;
        else ships2 = ships;
    }

    public boolean bothReady() {
        return ships1 != null && ships2 != null;
    }

    public WebSocketSession randomFirstPlayer() {
        return Math.random() < 0.5 ? player1 : player2;
    }

    public WebSocketSession other(WebSocketSession p) {
        return p == player1 ? player2 : player1;
    }

    public void registerShotResult(WebSocketSession enemy, String result) {
        if (result.equals("hit") || result.equals("kill")) {
            if (enemy == player1) hp1--;
            else hp2--;
        }
    }

    public boolean isGameOver() {
        return hp1 == 0 || hp2 == 0;
    }

    public WebSocketSession getWinner() {
        return hp1 == 0 ? player2 : player1;
    }

    public JsonNode getShips(WebSocketSession player) {
        return player == player1 ? ships1 : ships2;
    }

    public void close() {
        try { player1.close(); } catch (Exception ignored) {}
        try { player2.close(); } catch (Exception ignored) {}
    }

}
