package org.leycm.chessbot.jframe;

import org.leycm.chessbot.chess.ChessBoard;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that displays the current game status and turn information.
 */
public class GameStateDisplay extends JPanel {
    private final JLabel statusLabel;
    private final JLabel turnLabel;

    public GameStateDisplay() {
        setLayout(new BorderLayout());

        statusLabel = new JLabel("Game Status: Ready");
        turnLabel = new JLabel("Turn: White");

        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        turnLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        add(statusLabel, BorderLayout.WEST);
        add(turnLabel, BorderLayout.EAST);

        setBorder(BorderFactory.createEtchedBorder());
    }

    /**
     * Update the display with current game state and navigation information.
     */
    public void updateDisplay(ChessBoard chessBoard, NavigationController navigationController) {
        SwingUtilities.invokeLater(() -> {
            turnLabel.setText(navigationController.getTurnStatus());
            statusLabel.setText(navigationController.getGameStatus());
        });
    }
}