package org.ipan.nrgyrent.telegram.statetransitions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.ipan.nrgyrent.telegram.States;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class StateHandlerRegistry {
    private final Map<Tuple<States, Integer>, List<TransitionMatcher>> handlers = new HashMap<>();
    // private final Map<Tuple<States, Integer>, List<Tuple<Object, Method>>> handlers = new HashMap<>();

    public void register(MatchState state, Method method, Object bean) {
        int updateType = state.updateTypes();

        if (state.callbackData() != null && !state.callbackData().isEmpty()) {
            updateType = UpdateType.CALLBACK_QUERY;
        }

        Tuple<States, Integer> key = new Tuple<>(state.state(), updateType);
        TransitionMatcher matcher = new TransitionMatcher(createPredicate(state), bean, method);
        handlers.computeIfAbsent(key, k -> new ArrayList<>()).add(matcher);
    }

    public List<TransitionMatcher> getHandlers(States state, int updateType) {
        return handlers.getOrDefault(new Tuple<>(state, updateType), new ArrayList<>());
    }


    private Predicate<Update> createPredicate(MatchState state) {
        // Match by callback data
        if (state.callbackData() != null && !state.callbackData().isEmpty()) {
            return update -> {
                if (update.hasCallbackQuery()) {
                    String callbackData = update.getCallbackQuery().getData();
                    return callbackData.equals(state.callbackData());
                }
                return false;
            };
        }

        // Match by update type
        if (state.updateTypes() == UpdateType.CALLBACK_QUERY) {
            return update -> update.hasCallbackQuery();
        } else if (state.updateTypes() == UpdateType.MESSAGE) {
            return update -> update.hasMessage();
        }

        return update -> {
            // Default case, return false for all updates
            return false;
        };
    }
}