package org.leycm.chessbot.chess.controller;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessController;

public class VirtualUiController extends ChessController {
    // impl for human on ui
    public VirtualUiController(String name) {
        super(name, "UserInterface");
    }

    @Override
    public void onTick(ChessBoard board) {

    }

    @Override
    protected ChessController onClone() {
        return new VirtualUiController(getName());
    }

}
