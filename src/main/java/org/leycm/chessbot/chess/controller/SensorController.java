package org.leycm.chessbot.chess.controller;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessController;

public abstract class SensorController extends ChessController {

    public SensorController(String name, String type) {
        super(name, type);
    }

    public void sensorTick(boolean[] sensorFeedback, ChessBoard board) {
        onTick(board);
    }

    @Override
    public void tick(ChessBoard board) {
        System.err.println("This is an SensorController use SensorController#sensorTick");
        sensorTick(new boolean[64], board);
    }
}
