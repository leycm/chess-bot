package org.leycm.chessbot.jframe;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessMove;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel that displays the move history with clickable move buttons.
 */
public class MoveHistoryPanel extends JPanel {
    private final NavigationController navigationController;
    private final JPanel moveListPanel;

    public MoveHistoryPanel(NavigationController navigationController) {
        this.navigationController = navigationController;

        setLayout(new BorderLayout());
        setBackground(ChessConstants.HISTORY_BACKGROUND);

        moveListPanel = new JPanel();
        moveListPanel.setLayout(new BoxLayout(moveListPanel, BoxLayout.Y_AXIS));
        moveListPanel.setBackground(ChessConstants.HISTORY_BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(moveListPanel);
        scrollPane.setPreferredSize(new Dimension(260, 350));
        scrollPane.setBackground(ChessConstants.HISTORY_BACKGROUND);
        scrollPane.getVerticalScrollBar().setBackground(new Color(60, 60, 60));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    public JLabel createHistoryHeaderLabel() {
        JLabel historyLabel = new JLabel("Move History");
        historyLabel.setForeground(Color.WHITE);
        historyLabel.setFont(ChessConstants.LABEL_FONT);
        historyLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        return historyLabel;
    }

    public void updateMoveHistory(ChessBoard chessBoard) {
        SwingUtilities.invokeLater(() -> {
            moveListPanel.removeAll();

            List<ChessMove> history = chessBoard.getMoveHistory();

            for (int i = 0; i < history.size(); i += 2) {
                JPanel rowPanel = createMoveRowPanel(history, i);
                moveListPanel.add(rowPanel);
            }

            moveListPanel.revalidate();
            moveListPanel.repaint();

            // Auto-scroll to bottom
            SwingUtilities.invokeLater(() -> {
                JScrollPane scrollPane = (JScrollPane) moveListPanel.getParent().getParent();
                JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
                verticalScrollBar.setValue(verticalScrollBar.getMaximum());
            });
        });
    }

    private JPanel createMoveRowPanel(List<ChessMove> history, int startIndex) {
        JPanel rowPanel = new JPanel(new BorderLayout());
        rowPanel.setBackground(ChessConstants.HISTORY_BACKGROUND);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        // Move number
        int moveNumber = (startIndex / 2) + 1;
        JLabel moveNumberLabel = createMoveNumberLabel(moveNumber);
        rowPanel.add(moveNumberLabel, BorderLayout.WEST);

        // Move buttons
        JPanel movesPanel = createMovesPanel(history, startIndex);
        rowPanel.add(movesPanel, BorderLayout.CENTER);

        return rowPanel;
    }

    private JLabel createMoveNumberLabel(int moveNumber) {
        JLabel label = new JLabel(moveNumber + ".");
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(ChessConstants.HISTORY_NUMBER_FONT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(25, 25));
        return label;
    }

    private JPanel createMovesPanel(List<ChessMove> history, int startIndex) {
        JPanel movesPanel = new JPanel(new GridLayout(1, 2, 2, 0));
        movesPanel.setBackground(ChessConstants.HISTORY_BACKGROUND);

        // White move
        ChessMove whiteMove = history.get(startIndex);
        JButton whiteMoveButton = createMoveButton(whiteMove, startIndex, true);
        movesPanel.add(whiteMoveButton);

        // Black move (if exists)
        if (startIndex + 1 < history.size()) {
            ChessMove blackMove = history.get(startIndex + 1);
            JButton blackMoveButton = createMoveButton(blackMove, startIndex + 1, false);
            movesPanel.add(blackMoveButton);
        } else {
            movesPanel.add(new JLabel()); // Empty space
        }

        return movesPanel;
    }

    private JButton createMoveButton(ChessMove move, int moveIndex, boolean isWhite) {
        String moveText = MoveFormatter.formatMoveShort(move);
        JButton button = new JButton(moveText);

        button.setFont(ChessConstants.HISTORY_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(ChessConstants.BUTTON_BACKGROUND);
        button.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        button.setFocusPainted(false);
        button.setMargin(new Insets(1, 3, 1, 3));

        // Highlight current move
        if (moveIndex == navigationController.getCurrentMoveIndex()) {
            button.setBackground(ChessConstants.SELECTED_MOVE_COLOR);
        }

        // Add hover effects
        button.addMouseListener(new MoveButtonMouseListener(button, moveIndex));

        // Add click handler
        button.addActionListener(e -> navigationController.navigateToMove(moveIndex));

        return button;
    }

    /**
     * Mouse listener for move buttons to handle hover effects.
     */
    private class MoveButtonMouseListener extends MouseAdapter {
        private final JButton button;
        private final int moveIndex;

        public MoveButtonMouseListener(JButton button, int moveIndex) {
            this.button = button;
            this.moveIndex = moveIndex;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (moveIndex != navigationController.getCurrentMoveIndex()) {
                button.setBackground(ChessConstants.HOVER_MOVE_COLOR);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (moveIndex != navigationController.getCurrentMoveIndex()) {
                button.setBackground(ChessConstants.BUTTON_BACKGROUND);
            } else {
                button.setBackground(ChessConstants.SELECTED_MOVE_COLOR);
            }
        }
    }
}
