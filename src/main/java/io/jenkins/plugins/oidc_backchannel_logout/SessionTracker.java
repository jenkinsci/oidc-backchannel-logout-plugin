package io.jenkins.plugins.oidc_backchannel_logout;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SessionTracker {
    private static final Logger LOGGER = Logger.getLogger(SessionTracker.class.getName());
    private static final Map<String, HttpSession> SESSION_MAP = new ConcurrentHashMap<>();

    private SessionTracker() {
        throw new IllegalStateException("Utility class");
    }

    public static void track(String oidcSid, HttpSession session) {
        SESSION_MAP.put(oidcSid, session);
    }

    public static void invalidate(String oidcSid) {
        HttpSession session = SESSION_MAP.remove(oidcSid);
        if (session != null) {
            try {
                session.invalidate();
                LOGGER.fine("Successfully invalidated Jenkins session matching OIDC SID.");
            } catch (Exception e) {
                LOGGER.fine("Session already expired or not found during backchannel logout.");
            }
        }
    }
}