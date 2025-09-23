package org.keycloak.example.util;

import java.util.UUID;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UUIDUtil {

    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
