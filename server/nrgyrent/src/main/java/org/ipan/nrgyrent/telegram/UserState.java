package org.ipan.nrgyrent.telegram;

import lombok.Data;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
public class UserState {
    private Long telegramId;
    private States currentState;
    private Long chatId;
    private Integer menuMessageId;

//    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
//    private final Lock r = rwl.readLock();
//    private final Lock w = rwl.writeLock();
}
