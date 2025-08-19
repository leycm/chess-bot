package org.leycm.chessbot.util.statemachine;

import java.util.function.BiConsumer;

public class RunThroughStateMachine<E, C> extends StateMachine<E, C>{

    public RunThroughStateMachine(E initialState, BiConsumer<C, E> executor) {
        super(initialState, executor);
    }

    @Override
    public void transition(E newState) {
        super.transition(newState);
        super.execute();
    }
}
