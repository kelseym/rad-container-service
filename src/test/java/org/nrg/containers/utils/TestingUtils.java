package org.nrg.containers.utils;

import com.google.common.collect.Maps;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import org.mockito.ArgumentMatcher;

import java.util.Map;

public class TestingUtils {
    public static boolean canConnectToDocker(DockerClient client) throws InterruptedException, DockerException{
        return client.ping().equals("OK");
    }

    @SuppressWarnings("unchecked")
    public static ArgumentMatcher<Map<String, String>> isMapWithEntry(final String key, final String value) {
        return new ArgumentMatcher<Map<String, String>>() {
            @Override
            public boolean matches(final Object argument) {
                if (argument == null || !Map.class.isAssignableFrom(argument.getClass())) {
                    return false;
                }
                final Map<String, String> argumentMap = Maps.newHashMap();
                try {
                    argumentMap.putAll((Map)argument);
                } catch (ClassCastException e) {
                    return false;
                }

                for (final Map.Entry<String, String> entry : argumentMap.entrySet()) {
                    if (entry.getKey().equals(key) && entry.getValue().equals(value)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
