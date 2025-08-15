package org.leycm.chessbot.jframe;

import org.leycm.chessbot.chess.ChessBoard;

import javax.swing.*;
import java.awt.*;

/**
 * Panel containing game control and navigation buttons.
 */
public class GameControlPanel extends JPanel {
    private final ChessBoard chessBoard;
    private final NavigationController navigationController;

    public GameControlPanel(ChessBoard chessBoard, NavigationController navigationController) {
        this.chessBoard = chessBoard;
        this.navigationController = navigationController;

        setLayout(new BorderLayout());
        setBackground(ChessConstants.PANEL_BACKGROUND);

        add(createNavigationPanel(), BorderLayout.NORTH);
        add(createGameControlPanel(), BorderLayout.SOUTH);
    }

    private JPanel createNavigationPanel() {
        JPanel navPanel = new JPanel(new FlowLayout());
        navPanel.setBackground(ChessConstants.PANEL_BACKGROUND);

        JButton firstButton = createNavigationButton("⏮", "Zum Anfang",
                e -> navigationController.navigateToFirst());
        JButton prevButton = createNavigationButton("◀", "Zug zurück",
                e -> navigationController.navigateToPrevious());
        JButton nextButton = createNavigationButton("▶", "Zug vor",
                e -> navigationController.navigateToNext());
        JButton lastButton = createNavigationButton("⏭", "Zum Ende",
                e -> navigationController.navigateToLast());

        navPanel.add(firstButton);
        navPanel.add(prevButton);
        navPanel.add(nextButton);
        navPanel.add(lastButton);

        return navPanel;
    }

    private JButton createNavigationButton(String text, String tooltip,
                                           java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(action);
        return button;
    }

    private JPanel createGameControlPanel() {
        JPanel gamePanel = new JPanel(new FlowLayout());
        gamePanel.setBackground(ChessConstants.PANEL_BACKGROUND);

        JButton startButton = new JButton("Start Game");
        JButton resetButton = new JButton("Reset");

        startButton.addActionListener(e -> {
            if (chessBoard != null) {
                chessBoard.start();
                navigationController.navigateToLast(); // Go to latest position
            }
        });

        resetButton.addActionListener(e -> {
            if (chessBoard != null) {
                chessBoard.restart();
                navigationController.reset();
            }
        });

        gamePanel.add(startButton);
        gamePanel.add(resetButton);

        return gamePanel;
    }
}