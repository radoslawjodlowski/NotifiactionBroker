package pl.raaadziu.nb1;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SessionsContainer
{
    private CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    private Iterator<WebSocketSession> getSessionIterator()
    {
        return sessions.iterator();
    }

    void addSession(WebSocketSession ws)
    {
        sessions.add(ws);
    }

    void removeSession(WebSocketSession ws)
    {
        sessions.remove(ws);
    }

    void broadCastToAll(String input)
    {
        Iterator<WebSocketSession> iws = getSessionIterator();
        while(iws.hasNext())
        {
            WebSocketSession ws = iws.next();
            try {
                if (ws.isOpen()) ws.sendMessage(new TextMessage(input));
            }catch (IOException e)
            {
                //.... technically ok
            }
        }
    }

}
