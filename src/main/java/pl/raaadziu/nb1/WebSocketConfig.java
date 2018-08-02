package pl.raaadziu.nb1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private SocketHandler socketHandler;

    @Autowired
    public WebSocketConfig(SocketHandler socketHandler)
    {
        this.socketHandler = socketHandler;
    }

    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler, "/notificationBroker").setAllowedOrigins("*");
    }
}

