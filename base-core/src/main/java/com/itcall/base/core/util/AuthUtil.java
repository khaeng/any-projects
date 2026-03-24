package com.itcall.base.core.util;

import java.util.Map;

public class AuthUtil {

    public static String getUsername(Object tokenOrSession) {
        if (tokenOrSession instanceof Map) {
            // Assuming Map<String, Object> claims or Session attributes
            Map<?, ?> map = (Map<?, ?>) tokenOrSession;
            // standard sub or username
            if (map.containsKey("sub"))
                return (String) map.get("sub");
            if (map.containsKey("username"))
                return (String) map.get("username");
        }
        // If it's a specific object (HttpSession), we'd need dependency.
        // Base-core shouldn't depend on Servlet API heavily if possible, or maybe it
        // does?
        // Base-core usually has util classes.
        // Let's assume generic Map or Object with reflection/cast if needed, but Map is
        // safest common ground.
        return null;
    }

    public static String getClientId(Object tokenOrSession) {
        if (tokenOrSession instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) tokenOrSession;
            if (map.containsKey("clientId"))
                return (String) map.get("clientId");
            if (map.containsKey("aud"))
                return (String) map.get("aud"); // Audience often client id
        }
        return null;
    }
}
