package org.leycm.chessbot.chess.controller;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessController;
import org.leycm.chessbot.model.ModelLoader;

import java.util.concurrent.CompletableFuture;

public class VirtualAiController extends ChessController {

    public VirtualAiController(String name) {
        super(name, "AiController");
    }

    private boolean aiBusy = false;

    @Override
    public void onTick(ChessBoard board) {
        if (aiBusy) return;
        aiBusy = true;

        CompletableFuture.runAsync(() -> {
            try {ModelLoader.makeBestMove(board);
            } finally {aiBusy = false;}
        });
    }

}