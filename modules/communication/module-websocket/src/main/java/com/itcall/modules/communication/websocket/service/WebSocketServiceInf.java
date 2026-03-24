package com.itcall.modules.communication.websocket.service;

import java.io.IOException;

public interface WebSocketServiceInf {
    void sendMessageToUser(String username, String message) throws IOException;

    void broadcastMessage(String message) throws IOException;
}
