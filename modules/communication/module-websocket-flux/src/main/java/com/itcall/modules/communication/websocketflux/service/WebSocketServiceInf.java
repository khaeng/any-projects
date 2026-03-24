package com.itcall.modules.communication.websocketflux.service;

import reactor.core.publisher.Mono;

public interface WebSocketServiceInf {
    Mono<Void> sendMessageToUser(String username, String message);

    Mono<Void> broadcastMessage(String message);
}
