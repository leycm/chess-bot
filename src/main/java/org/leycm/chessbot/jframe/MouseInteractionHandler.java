package org.leycm.chessbot.jframe;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Handles mouse interactions for the chess board panel.
 */
public class MouseInteractionHandler extends MouseAdapter {
    private final ChessBoardPanel boardPanel;

    public MouseInteractionHandler(ChessBoardPanel boardPanel) {
        this.boardPanel = boardPanel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!boardPanel.canInteract()) return;

        int col = e.getX() / ChessConstants.SQUARE_SIZE;
        int row = e.getY() / ChessConstants.SQUARE_SIZE;

        if (isValidSquare(col, row)) {
            boardPanel.setDragStart(e.getPoint());
            boardPanel.handlePieceSelection(col, row);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!boardPanel.isDragging()) return;

        int col = e.getX() / ChessConstants.SQUARE_SIZE;
        int row = e.getY() / ChessConstants.SQUARE_SIZE;

        if (isValidSquare(col, row)) {
            boardPanel.handlePieceMove(col, row);
        } else {
            boardPanel.clearDragState();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (boardPanel.isDragging()) {
            boardPanel.repaint();
        }
    }

    private boolean isValidSquare(int col, int row) {
        return col >= 0 && col < 8 && row >= 0 && row < 8;
    }
}
