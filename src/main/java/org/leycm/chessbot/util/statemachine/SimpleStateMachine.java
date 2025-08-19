package org.leycm.chessbot.util.statemachine;

import java.util.function.Consumer;

public class SimpleStateMachine<E extends Enum<?>> extends StateMachine<E, Consumer<E>> {

    public SimpleStateMachine(E initialState) {
        super(initialState, Consumer::accept);
    }

}
