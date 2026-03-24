package com.itcall.modules.security.session.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

// User mentioned "session... map... create claims".
// Spring Session usually handles session creation via HttpServletRequest.
// But if user wants a "service" to create session manually or similar to JWT validation?
// "session also... create claims from map... (login success)"
// This implies putting attributes into session.

@Service
public class SessionService {

    // Helper to interact with current session or manage session data.
    // In Spring, we usually use HttpSession.
    // Provide helper to set attributes.

    // Example: createSession(Map<String, Object> claims, HttpSession session)
    // But HttpSession is usually bound to request.
    // If usage is "Login Service calls createSession", it must pass the HttpSession
    // object.

    public void createSession(Map<String, Object> claims, jakarta.servlet.http.HttpSession session) {
        claims.forEach(session::setAttribute);
        // Maybe set a specific flag?
        session.setAttribute("authenticated", true);
    }

    // If we need to access session from non-web context, we rely on Spring Session
    // repository,
    // but that requires Session ID.
}
