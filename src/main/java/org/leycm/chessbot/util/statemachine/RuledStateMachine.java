package org.leycm.chessbot.util.statemachine;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class RuledStateMachine<E, C> extends StateMachine<E, C> {

    protected final Map<E, Predicate<E>> transitionLogic;

    public RuledStateMachine(E initialState, BiConsumer<C, E> executor) {
        super(initialState, executor);
        this.transitionLogic = new HashMap<>();
    }

    public void setRule(E state, Predicate<E> rule) {
        if (state != null) transitionLogic.put(state, rule);
    }

    public void setRule(E @NotNull [] states, Predicate<E> rule) {
        for (E state : states) {
            if(state != null) transitionLogic.put(state, rule);
        }
    }

    @Override
    public void transition(E newState) {

        boolean canceled = false;

        Predicate<E> rule = transitionLogic.get(newState);
        if (rule != null) canceled = !rule.test(newState);

        if (canceled) {
            System.err.println("Switching form state \"" + currentState + "\" to \"" + newState + "\" was canceled");
            System.out.println("Switching form state \"" + currentState + "\" to \"" + newState + "\" was canceled");
            return;
        }

        super.transition(newState);
    }
}
