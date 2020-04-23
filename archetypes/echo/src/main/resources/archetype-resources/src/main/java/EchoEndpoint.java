package $package;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Server endpoint "listening" on path "/echo".
 */
@ServerEndpoint("/echo")
public class EchoEndpoint {

    /**
     * Method handling incoming text messages.
     * <p/>
     * This implementations just sends back received message.
     *
     * @param message received message.
     * @return returned value will be sent back to client. You can also declare this method to not return anything. In
     * that case, you would need to obtain {@link jakarta.websocket.Session} object and call
     * {@link jakarta.websocket.Session#getBasicRemote()#sendText();} i order to send message.
     */
    @OnMessage
    public String echo(String message) {
        return message;
    }
}
