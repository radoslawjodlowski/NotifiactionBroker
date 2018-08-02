package pl.raaadziu.nb1;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


@Component
public class SocketHandler extends TextWebSocketHandler {

    private SessionsContainer sessionsContainer;
    private MemBox memBox;

    private static Logger log = LoggerFactory.getLogger("SOCKET");

    @Autowired
    public SocketHandler(SessionsContainer sessionsContainer,MemBox memBox)
    {
        this.sessionsContainer = sessionsContainer;
        this.memBox = memBox;
    }
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws SqlApiException {
        String text = message.getPayload();
        JSONObject a = new JSONObject(text);
        if (a.has("commit"))
        {
            Integer id = a.getInt("commit");
            memBox.commitById(id);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug(session.getId() + " connected ");
        sessionsContainer.addSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        log.debug(session.getId() + " disconnected ");
        sessionsContainer.removeSession(session);
    }
}