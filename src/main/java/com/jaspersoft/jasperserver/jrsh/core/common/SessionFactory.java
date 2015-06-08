package com.jaspersoft.jasperserver.jrsh.core.common;

import com.jaspersoft.jasperserver.jaxrs.client.core.AuthenticationCredentials;
import com.jaspersoft.jasperserver.jaxrs.client.core.RestClientConfiguration;
import com.jaspersoft.jasperserver.jaxrs.client.core.Session;
import com.jaspersoft.jasperserver.jaxrs.client.core.SessionStorage;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Alexander Krasnyanskiy
 */
public class SessionFactory {

    private static final Logger log = Logger.getLogger(SessionFactory.class);
    private static Session SHARED_SESSION;


    public static Session getSharedSession() {
        return SHARED_SESSION;
    }


    public static Session createUnsharedSession(String url, String username, String password, String organization) {
        return createSession(url, username, password, organization);
    }


    public static Session createSharedSession(String url, String username, String password, String organization) {
        return SHARED_SESSION = createSession(url, username, password, organization);
    }


    private static Session createSession(String url, String username, String password, String organization) {
        username = organization == null ? username : username.concat("|").concat(organization);
        url = url.startsWith("http") ? url : "http://".concat(url);
        Map<String, Integer> map = getClientTimeout();
        //
        // Create a new session
        //
        SHARED_SESSION = new Session(new SessionStorage(new RestClientConfiguration(url), new AuthenticationCredentials(username, password)));
        //
        // Set connection timeout
        //
        Integer connectionTimeout = map.get("connection");
        SHARED_SESSION
                .getStorage()
                .getConfiguration()
                .setConnectionTimeout(connectionTimeout);
        //
        // Set read timeout
        //
        Integer readTimeout = map.get("connection");
        SHARED_SESSION
                .getStorage()
                .getConfiguration()
                .setReadTimeout(readTimeout);

        log.info(format("timeout: {read: %s}, {connection: %s}", connectionTimeout, readTimeout));
        return SHARED_SESSION;
    }

    public static void updateSharedSession(Session session) {
        SHARED_SESSION = session;
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Integer> getClientTimeout() {
        Yaml yaml = new Yaml();
        Map<String, Object> config = new HashMap<>();
        try {
            try (InputStream file = SessionFactory.class.getClassLoader().getResourceAsStream("client.yml")) {
                config.putAll((Map<String, Object>) yaml.load(file));
            }
        } catch (IOException ignored) {
            // NOP
        }
        return (Map<String, Integer>)
                ((Map) config.get("client")).get("timeout");
    }

}
