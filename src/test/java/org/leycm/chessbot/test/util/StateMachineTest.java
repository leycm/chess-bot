package org.leycm.chessbot.test.util;

import org.leycm.chessbot.util.statemachine.RuledStateMachine;
import org.leycm.chessbot.util.statemachine.RunThroughStateMachine;
import org.leycm.chessbot.util.statemachine.SimpleStateMachine;
import org.leycm.chessbot.util.statemachine.StateMachine;

import java.util.function.Consumer;

public class StateMachineTest {

    public static void main(String[] args) {
        simple(args);
        System.out.println("\n");
        enhanced(args);
        System.out.println("\n");
        runThrough(args);
        System.out.println("\n");
        normal(args);
        System.out.println("\n");
        string(args);
        System.out.println("\n");
    }

    public static void enhanced(String[] args) {
        RuledStateMachine<GameState, Consumer<GameState>> game = new RuledStateMachine<>(GameState.START, Consumer::accept);

        game.setLogic(GameState.values(), s -> System.out.println("Standart ausgabe fuer " + s));

        game.setLogic(GameState.START, s -> System.out.println("Spiel wird initialisiert..."));
        game.setLogic(GameState.PLAYING, s -> System.out.println("Spiel laeuft jetzt!"));
        game.setLogic(GameState.PAUSED, s -> System.out.println("Spiel pausiert."));
        game.setLogic(GameState.ENDE, s -> System.out.println("Spiel beendet."));


        game.setRule(GameState.ENDE, newState -> { // hier ist klar newState immer = ENDE
            return game.getCurrentState() != GameState.START;
        });

        game.transition(GameState.START);
        game.execute();
        game.transition(GameState.PLAYING);
        game.execute();
        game.transition(GameState.PAUSED);
        game.execute();
        game.transition(GameState.START);
        game.execute();
        game.transition(GameState.ENDE);
        game.execute();
    }

    public static void runThrough(String[] args) {
        RunThroughStateMachine<GameState, Consumer<GameState>> game = new RunThroughStateMachine<>(GameState.START, Consumer::accept);

        game.setLogic(GameState.START, s -> System.out.println("Spiel wird initialisiert..."));
        game.setLogic(GameState.PLAYING, s -> System.out.println("Spiel laeuft jetzt!"));
        game.setLogic(GameState.PAUSED, s -> System.out.println("Spiel pausiert."));
        game.setLogic(GameState.ENDE, s -> System.out.println("Spiel beendet."));

        game.transition(GameState.START);
        game.transition(GameState.PLAYING);
        game.transition(GameState.PAUSED);
        game.transition(GameState.ENDE);
    }

    public static void normal(String[] args) {
        StateMachine<GameState, Consumer<GameState>> game = new StateMachine<>(GameState.START, Consumer::accept);

        game.setLogic(GameState.START, s -> System.out.println("Spiel wird initialisiert..."));
        game.setLogic(GameState.PLAYING, s -> System.out.println("Spiel laeuft jetzt!"));
        game.setLogic(GameState.PAUSED, s -> System.out.println("Spiel pausiert."));
        game.setLogic(GameState.ENDE, s -> System.out.println("Spiel beendet."));

        game.transition(GameState.START);
        game.execute();
        game.transition(GameState.PLAYING);
        game.execute();
        game.transition(GameState.PAUSED);
        game.execute();
        game.transition(GameState.ENDE);
        game.execute();
    }

    public static void simple(String[] args) {
        SimpleStateMachine<GameState> game = new SimpleStateMachine<>(GameState.START);

        game.setLogic(GameState.START, s -> System.out.println("Spiel wird initialisiert..."));
        game.setLogic(GameState.PLAYING, s -> System.out.println("Spiel laeuft jetzt!"));
        game.setLogic(GameState.PAUSED, s -> System.out.println("Spiel pausiert."));
        game.setLogic(GameState.ENDE, s -> System.out.println("Spiel beendet."));

        game.transition(GameState.START);
        game.execute();
        game.transition(GameState.PLAYING);
        game.execute();
        game.transition(GameState.PAUSED);
        game.execute();
        game.transition(GameState.ENDE);
        game.execute();
    }

    public static void string(String[] args) {
        StateMachine<String, Consumer<String>> game = new StateMachine<>("START", Consumer::accept);

        game.setLogic("START", s -> System.out.println("Spiel wird initialisiert..."));
        game.setLogic("PLAYING", s -> System.out.println("Spiel laeuft jetzt!"));
        game.setLogic("PAUSED", s -> System.out.println("Spiel pausiert."));
        game.setLogic("ENDE", s -> System.out.println("Spiel beendet."));

        game.transition("START");
        game.execute();
        game.transition("PLAYING");
        game.execute();
        game.transition("PAUSED");
        game.execute();
        game.transition("ENDE");
        game.execute();
    }


    public enum GameState {
        START,
        PLAYING,
        PAUSED,
        ENDE
    }


}
