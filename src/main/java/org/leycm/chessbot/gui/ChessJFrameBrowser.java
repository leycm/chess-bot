package org.leycm.chessbot.gui;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessBoard.Move;
import org.leycm.chessbot.chess.ChessPiece;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ChessJFrameBrowser extends JFrame {
    private static ChessJFrameBrowser instance;
    private JTabbedPane tabbedPane;
    private Map<String, ChessTab> tabs = new HashMap<>();

    private ChessJFrameBrowser() {
        initializeMainWindow();
    }

    public static void openTab(String id, ChessBoard board) {
        if (instance == null) {
            instance = new ChessJFrameBrowser();
            instance.setVisible(true);
        }

        instance.addOrUpdateTab(id, board);
        instance.toFront();
        instance.requestFocus();
    }

    private void initializeMainWindow() {
        setTitle("Chess Browser");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void addOrUpdateTab(String id, ChessBoard board) {
        if (tabs.containsKey(id)) {
            // Update existing tab
            ChessTab existingTab = tabs.get(id);
            existingTab.updateBoard(board);
            tabbedPane.setSelectedComponent(existingTab);
        } else {
            // Create new tab
            ChessTab newTab = new ChessTab(id, board);
            tabs.put(id, newTab);

            // Create closeable tab
            int index = tabbedPane.getTabCount();
            tabbedPane.addTab(id, newTab);
            tabbedPane.setTabComponentAt(index, createTabComponent(id));
            tabbedPane.setSelectedIndex(index);
        }
    }

    private JPanel createTabComponent(String tabId) {
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tabPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(tabId);
        titleLabel.setBorder(new EmptyBorder(0, 0, 0, 5));

        JButton closeButton = new JButton("×");
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setFocusable(false);
        closeButton.setFont(new Font("SansSerif", Font.BOLD, 12));

        closeButton.addActionListener(e -> closeTab(tabId));

        tabPanel.add(titleLabel);
        tabPanel.add(closeButton);

        return tabPanel;
    }

    private void closeTab(String tabId) {
        ChessTab tab = tabs.get(tabId);
        if (tab != null) {
            int index = tabbedPane.indexOfComponent(tab);
            if (index >= 0) {
                tabbedPane.removeTabAt(index);
                tabs.remove(tabId);

                // Close window if no tabs left
                if (tabs.isEmpty()) {
                    dispose();
                    instance = null;
                }
            }
        }
    }

    // Inner class for individual chess tab content
    private static class ChessTab extends JPanel {
        private ChessBoard board;
        private String tabId;
        private JPanel chessBoardPanel;
        private JList<String> moveHistoryList;
        private DefaultListModel<String> historyModel;
        private JLabel statusLabel;
        private JButton prevButton, nextButton, firstButton, lastButton;
        private int currentMoveIndex = -1;
        private ChessBoard currentDisplayBoard;

        public ChessTab(String tabId, ChessBoard board) {
            this.tabId = tabId;
            this.board = board;
            this.currentDisplayBoard = copyBoard(board);
            this.currentMoveIndex = board.getMoveHistory().size() - 1;

            initializeUI();
            updateDisplay();
        }

        public void updateBoard(ChessBoard newBoard) {
            this.board = newBoard;
            this.currentDisplayBoard = copyBoard(newBoard);
            this.currentMoveIndex = board.getMoveHistory().size() - 1;
            updateDisplay();
        }

        private void initializeUI() {
            setLayout(new BorderLayout());

            chessBoardPanel = new JPanel(new GridLayout(8, 8));
            chessBoardPanel.setPreferredSize(new Dimension(400, 400));
            chessBoardPanel.setBorder(BorderFactory.createTitledBorder("Chess Board"));

            JPanel historyPanel = new JPanel(new BorderLayout());
            historyPanel.setPreferredSize(new Dimension(400, 400));
            historyPanel.setBorder(BorderFactory.createTitledBorder("Move History"));

            historyModel = new DefaultListModel<>();
            moveHistoryList = new JList<>(historyModel);
            moveHistoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            moveHistoryList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedIndex = moveHistoryList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        navigateToMove(selectedIndex);
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(moveHistoryList);
            historyPanel.add(scrollPane, BorderLayout.CENTER);

            JPanel navPanel = new JPanel(new FlowLayout());
            firstButton = new JButton("⏮");
            prevButton = new JButton("⏪");
            nextButton = new JButton("⏩");
            lastButton = new JButton("⏭");

            firstButton.setToolTipText("First Move");
            prevButton.setToolTipText("Previous Move");
            nextButton.setToolTipText("Next Move");
            lastButton.setToolTipText("Last Move");

            firstButton.addActionListener(e -> navigateToMove(-1));
            prevButton.addActionListener(e -> navigateToMove(Math.max(-1, currentMoveIndex - 1)));
            nextButton.addActionListener(e -> navigateToMove(Math.min(board.getMoveHistory().size() - 1, currentMoveIndex + 1)));
            lastButton.addActionListener(e -> navigateToMove(board.getMoveHistory().size() - 1));

            navPanel.add(firstButton);
            navPanel.add(prevButton);
            navPanel.add(nextButton);
            navPanel.add(lastButton);

            historyPanel.add(navPanel, BorderLayout.SOUTH);

            // Status label
            statusLabel = new JLabel("Ready");
            statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

            // Layout
            add(chessBoardPanel, BorderLayout.WEST);
            add(historyPanel, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.SOUTH);
        }

        private void updateDisplay() {
            updateChessBoard();
            updateMoveHistory();
            updateNavigationButtons();
            updateStatus();
        }

        private void updateChessBoard() {
            chessBoardPanel.removeAll();

            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    JPanel square = new JPanel(new BorderLayout());
                    square.setPreferredSize(new Dimension(50, 50));

                    if ((row + col) % 2 == 0) {
                        square.setBackground(new Color(240, 217, 181));
                    } else {
                        square.setBackground(new Color(181, 136, 99));
                    }

                    if (col == 0) {
                        JLabel rowLabel = new JLabel(String.valueOf(8 - row));
                        rowLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
                        rowLabel.setHorizontalAlignment(SwingConstants.LEFT);
                        rowLabel.setVerticalAlignment(SwingConstants.TOP);
                        square.add(rowLabel, BorderLayout.NORTH);
                    }
                    if (row == 7) {
                        JLabel colLabel = new JLabel(String.valueOf((char)('a' + col)));
                        colLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
                        colLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                        colLabel.setVerticalAlignment(SwingConstants.BOTTOM);
                        square.add(colLabel, BorderLayout.SOUTH);
                    }

                    ChessPiece piece = currentDisplayBoard.getPiece(col, row);
                    if (piece != null) {
                        JLabel pieceLabel = new JLabel(String.valueOf(piece.getIco()), SwingConstants.CENTER);
                        pieceLabel.setFont(new Font("SansSerif", Font.PLAIN, 28));
                        pieceLabel.setForeground(piece.isWhite() ? Color.WHITE : Color.BLACK);

                        pieceLabel.setBorder(BorderFactory.createEmptyBorder());
                        square.add(pieceLabel, BorderLayout.CENTER);
                    }

                    if (currentMoveIndex >= 0 && currentMoveIndex < board.getMoveHistory().size()) {
                        Move lastMove = board.getMoveHistory().get(currentMoveIndex);
                        if ((col == lastMove.fromX() && row == lastMove.fromY()) ||
                                (col == lastMove.toX() && row == lastMove.toY())) {
                            square.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                        }
                    }

                    chessBoardPanel.add(square);
                }
            }

            chessBoardPanel.revalidate();
            chessBoardPanel.repaint();
        }

        private void updateMoveHistory() {
            historyModel.clear();
            List<Move> moves = board.getMoveHistory();

            for (int i = 0; i < moves.size(); i++) {
                String moveStr = (i + 1) + ". " + moves.get(i).toString();
                historyModel.addElement(moveStr);
            }

            if (currentMoveIndex >= 0 && currentMoveIndex < moves.size()) {
                moveHistoryList.setSelectedIndex(currentMoveIndex);
                moveHistoryList.ensureIndexIsVisible(currentMoveIndex);
            } else {
                moveHistoryList.clearSelection();
            }
        }

        private void updateNavigationButtons() {
            List<Move> moves = board.getMoveHistory();
            firstButton.setEnabled(currentMoveIndex >= 0);
            prevButton.setEnabled(currentMoveIndex >= 0);
            nextButton.setEnabled(currentMoveIndex < moves.size() - 1);
            lastButton.setEnabled(currentMoveIndex < moves.size() - 1);
        }

        private void updateStatus() {
            List<Move> moves = board.getMoveHistory();
            if (moves.isEmpty()) {
                statusLabel.setText("Game start - No moves yet");
            } else if (currentMoveIndex < 0) {
                statusLabel.setText("Viewing initial position");
            } else if (currentMoveIndex == moves.size() - 1) {
                statusLabel.setText("Current position - Move " + (currentMoveIndex + 1) + "/" + moves.size());
            } else {
                statusLabel.setText("Viewing move " + (currentMoveIndex + 1) + "/" + moves.size());
            }
        }

        private void navigateToMove(int moveIndex) {
            List<Move> moves = board.getMoveHistory();

            if (moveIndex < -1 || moveIndex >= moves.size()) {
                return;
            }

            currentMoveIndex = moveIndex;

            currentDisplayBoard = new ChessBoard();

            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    ChessPiece piece = board.getPiece(x, y);
                    if (piece != null && !hasMoved(piece, moves)) {
                        currentDisplayBoard.placePiece(piece, x, y);
                    }
                }
            }

            for (int i = 0; i <= currentMoveIndex; i++) {
                Move move = moves.get(i);
                currentDisplayBoard.movePiece(move.fromX(), move.fromY(), move.toX(), move.toY());
            }

            updateDisplay();
        }

        @Contract(pure = true)
        private boolean hasMoved(ChessPiece piece, @NotNull List<Move> moves) {
            for (Move move : moves) {
                if (move.movedPiece() == piece) {
                    return true;
                }
            }
            return false;
        }

        private @NotNull ChessBoard copyBoard(ChessBoard original) {
            ChessBoard copy = new ChessBoard();

            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    ChessPiece piece = original.getPiece(x, y);
                    if (piece != null) {
                        copy.placePiece(piece, x, y);
                    }
                }
            }

            return copy;
        }
    }

}