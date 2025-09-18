package pipelines;

import io.javalin.websocket.WsContext;

import java.util.Set;

record Link(String href, String method) {}

class BookingWebSocketHub {
    private final Set<WsContext> sessions = new java.util.concurrent.CopyOnWriteArraySet<>();

    public void register(WsContext ctx) {
        sessions.add(ctx);
    }

    public void unregister(WsContext ctx) {
        sessions.remove(ctx);
    }

    public void broadcast(Object message) {
        for (WsContext session : sessions) {
            if (session.session.isOpen()) {
                session.send(message);
            }
        }
    }
}