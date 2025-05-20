package org.ipan.nrgyrent.telegram.statetransitions;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
public class TransitionMatcher {
    private final Predicate<Update> test;
    @Getter
    private final Object bean;
    @Getter
    private final Method method;

    public boolean matches(Update update) {
        return test.test(update);
    }
}