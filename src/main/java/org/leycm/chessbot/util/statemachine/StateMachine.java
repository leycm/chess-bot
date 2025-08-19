package org.leycm.chessbot.util.statemachine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
public class StateMachine<E, C> {

    @Getter
    protected E currentState;
    protected final BiConsumer<C, E> executor;
    protected final Map<E, C> stateLogic;

    public StateMachine(E initialState, BiConsumer<C, E> executor) {
        this.currentState = initialState;
        this.executor = executor;
        this.stateLogic = new HashMap<>();
    }

    public void execute() {
        C logic = stateLogic.get(currentState);
        if (logic != null) executor.accept(logic, currentState);
    }

    public void setLogic(E state, C logic) {
        if (state != null) stateLogic.put(state, logic);
    }

    public void setLogic(E @NotNull [] states, C logic) {
        for (E state : states) {
            if(state != null) stateLogic.put(state, logic);
        }
    }

    public void transition(E newState) {
        this.currentState = newState;
    }

}