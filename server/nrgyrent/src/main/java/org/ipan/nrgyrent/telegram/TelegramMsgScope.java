package org.ipan.nrgyrent.telegram;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

public class TelegramMsgScope implements Scope {
    public static final String TG_MESSAGE = "tg-message";

    private Map<Pair<Integer, String>, Object> scopedObjects = Collections.synchronizedMap(new HashMap<Pair<Integer, String>, Object>());
    private Map<Pair<Integer, String>, Runnable> destructionCallbacks = Collections.synchronizedMap(new HashMap<Pair<Integer, String>, Runnable>());

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        TgMessageMeta tgMessageMeta = TgMessageMetaHolder.getTgMessageMeta();

        Pair<Integer, String> key = new Pair<>(tgMessageMeta.getUpdateId(), name);

        if(!scopedObjects.containsKey(key)) {
            scopedObjects.put(key, objectFactory.getObject());
        }
        return scopedObjects.get(key);
    }

    @Override
    public Object remove(String name) {
        TgMessageMeta tgMessageMeta = TgMessageMetaHolder.getTgMessageMeta();
        Pair<Integer, String> key = new Pair<>(tgMessageMeta.getUpdateId(), name);

        destructionCallbacks.remove(key);
        return scopedObjects.remove(key);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        TgMessageMeta tgMessageMeta = TgMessageMetaHolder.getTgMessageMeta();
        Pair<Integer, String> key = new Pair<>(tgMessageMeta.getUpdateId(), name);
        destructionCallbacks.put(key, callback);
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return null;
    }


    private static record Pair<T1, T2>(T1 val1, T2 val2) {}
}
