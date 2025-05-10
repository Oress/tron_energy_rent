package org.ipan.nrgyrent.telegram;

public interface UserState {
    Long getTelegramId();
    States getState();
    Long getChatId();
    Integer getMenuMessageId();

    UserState withTelegramId(Long value);
    UserState withState(States value);
    UserState withChatId(Long value);
    UserState withMenuMessageId(Integer value);

//    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
//    private final Lock r = rwl.readLock();
//    private final Lock w = rwl.writeLock();
}
