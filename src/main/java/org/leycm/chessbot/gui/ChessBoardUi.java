package org.leycm.chessbot.gui;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.*;
import org.leycm.chessbot.chess.controller.VirtualAiController;
import org.leycm.chessbot.chess.controller.VirtualUiController;
import org.leycm.chessbot.chess.pieces.*;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChessBoardUi extends JFrame {

    private static final Map<String, ChessBoardUi> ACTIVE_BOARDS = new ConcurrentHashMap<>();

    private static final int SQUARE_SIZE = 70;
    private static final int BOARD_SIZE = SQUARE_SIZE * 8;
    private static final Color LIGHT_SQUARE = new Color(240, 217, 181);
    private static final Color DARK_SQUARE = new Color(181, 136, 99);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 0, 128);
    private static final Color LAST_MOVE_COLOR = new Color(155, 199, 0, 128);
    private static final Color CHECK_COLOR = new Color(255, 0, 0, 128);
    private static final Color VALID_MOVE_COLOR = new Color(0, 255, 0, 100);


    private final String boardId;
    private ChessBoard chessBoard;
    private ChessBoardPanel boardPanel;
    private JPanel moveHistoryPanel;
    private JLabel statusLabel;
    private JLabel turnLabel;

    private Point dragStart = null;
    private ChessPiece draggedPiece = null;
    private int draggedFromX = -1, draggedFromY = -1;

    private ChessMove lastMove = null;
    private final Set<Point> validMoves = new HashSet<>();
    private final Set<Point> validHitMoves = new HashSet<>();
    private Point kingInCheck = null;

    private ChessBoardUi(String id, ChessBoard board) {
        this.boardId = id;
        this.chessBoard = board;
        initializeUI();
        updateBoard();

        new Timer(100, e -> {
            if (chessBoard != null) {
                updateBoard();
            }
        }).start();
    }

    public static void streamBoard(String id, ChessBoard board) {
        SwingUtilities.invokeLater(() -> {
            ChessBoardUi existingUI = ACTIVE_BOARDS.get(id);
            if (existingUI != null) {
                existingUI.chessBoard = board;
                existingUI.updateBoard();
                existingUI.toFront();
                existingUI.requestFocus();
            } else {
                ChessBoardUi newUI = new ChessBoardUi(id, board);
                ACTIVE_BOARDS.put(id, newUI);
                newUI.setVisible(true);
            }
        });
    }

    private void initializeUI() {
        setTitle("Chess Board - " + boardId);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        boardPanel = new ChessBoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        JPanel sidePanel = createSidePanel();
        add(sidePanel, BorderLayout.EAST);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Game Status: Ready");
        turnLabel = new JLabel("Turn: White");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(turnLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ACTIVE_BOARDS.remove(boardId);
            }
        });
    }

    private @NotNull JPanel createSidePanel() {
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(250, BOARD_SIZE));
        sidePanel.setBackground(new Color(40, 40, 40));

        JLabel historyLabel = new JLabel("Move History");
        historyLabel.setForeground(Color.WHITE);
        historyLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        historyLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        moveHistoryPanel = new JPanel();
        moveHistoryPanel.setLayout(new BoxLayout(moveHistoryPanel, BoxLayout.Y_AXIS));
        moveHistoryPanel.setBackground(new Color(50, 50, 50));

        JScrollPane scrollPane = new JScrollPane(moveHistoryPanel);
        scrollPane.setPreferredSize(new Dimension(230, 400));
        scrollPane.setBackground(new Color(50, 50, 50));
        scrollPane.getVerticalScrollBar().setBackground(new Color(60, 60, 60));

        sidePanel.add(historyLabel, BorderLayout.NORTH);
        sidePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = getJPanel();
        sidePanel.add(buttonPanel, BorderLayout.SOUTH);

        return sidePanel;
    }

    private @NotNull JPanel getJPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(40, 40, 40));

        JButton startButton = new JButton("Start Game");
        JButton resetButton = new JButton("Reset");

        startButton.addActionListener(e -> {
            if (chessBoard != null) {
                chessBoard.start();
                updateBoard();
            }
        });

        resetButton.addActionListener(e -> {
            if (chessBoard != null) {
                chessBoard.clear();
                updateBoard();
            }
        });

        buttonPanel.add(startButton);
        buttonPanel.add(resetButton);
        return buttonPanel;
    }

    private void updateBoard() {
        if (chessBoard == null) return;

        SwingUtilities.invokeLater(() -> {
            turnLabel.setText("Turn: " + (chessBoard.isWhiteTurn() ? "White" : "Black"));

            String status = switch (chessBoard.getState()) {
                case START -> "Game Ready to Start";
                case PLAYING -> "Game in Progress";
                case CHECKMATE_WHITE_WINS -> "Checkmate! White Wins";
                case CHECKMATE_BLACK_WINS -> "Checkmate! Black Wins";
                case STALEMATE -> "Stalemate! Draw";
                case IDLE -> "Game Idle";
            };

            statusLabel.setText("Status: " + status);

            List<ChessMove> history = chessBoard.getMoveHistory();
            if (!history.isEmpty()) {
                lastMove = history.getLast();
            }

            kingInCheck = null;
            if (chessBoard.isKingInCheck(chessBoard.isWhiteTurn())) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        ChessPiece piece = chessBoard.getPiece(x, y);
                        if (piece instanceof KingChessPiece && piece.isWhite() == chessBoard.isWhiteTurn()) {
                            kingInCheck = new Point(x, y);
                            break;
                        }
                    }
                }
            }

            updateMoveHistory();

            boardPanel.repaint();
        });
    }

    private void updateMoveHistory() {
        moveHistoryPanel.removeAll();

        List<ChessMove> history = chessBoard.getMoveHistory();
        for (int i = 0; i < history.size(); i++) {
            ChessMove move = history.get(i);
            String moveText = formatMove(move, i + 1);

            JLabel moveLabel = new JLabel(moveText);
            moveLabel.setForeground(Color.WHITE);
            moveLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            moveLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

            if (i == history.size() - 1) {
                moveLabel.setOpaque(true);
                moveLabel.setBackground(new Color(100, 100, 150));
            }

            moveHistoryPanel.add(moveLabel);
        }

        moveHistoryPanel.revalidate();
        moveHistoryPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) moveHistoryPanel.getParent().getParent();
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        });
    }

    private @NotNull String formatMove(@NotNull ChessMove move, int moveNumber) {
        char fromFile = (char) ('a' + move.getFromX());
        char toFile = (char) ('a' + move.getToX());
        int fromRank = 8 - move.getFromY();
        int toRank = 8 - move.getToY();

        return String.format("%d. %s", moveNumber, fromFile, fromRank, toFile, toRank);
    }

    private class ChessBoardPanel extends JPanel {

        public ChessBoardPanel() {
            setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleMousePressed(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleMouseReleased(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    handleMouseDragged(e);
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawBoard(g2d);
            drawHighlights(g2d);
            drawPieces(g2d);

            g2d.dispose();
        }

        private void drawBoard(Graphics2D g2d) {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Color squareColor = (row + col) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE;
                    g2d.setColor(squareColor);
                    g2d.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
                }
            }

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

            for (int i = 0; i < 8; i++) {
                char file = (char) ('a' + i);
                g2d.drawString(String.valueOf(file), i * SQUARE_SIZE + 5, BOARD_SIZE - 5);
            }

            for (int i = 0; i < 8; i++) {
                int rank = 8 - i;
                g2d.drawString(String.valueOf(rank), 5, i * SQUARE_SIZE + 15);
            }
        }

        private void drawHighlights(Graphics2D g2d) {
            if (lastMove != null) {
                g2d.setColor(LAST_MOVE_COLOR);
                g2d.fillRect(lastMove.getFromX() * SQUARE_SIZE, lastMove.getFromY() * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
                g2d.fillRect(lastMove.getToX() * SQUARE_SIZE, lastMove.getToY() * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            }

            if (kingInCheck != null) {
                g2d.setColor(CHECK_COLOR);
                g2d.fillRect(kingInCheck.x * SQUARE_SIZE, kingInCheck.y * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            }

            g2d.setColor(VALID_MOVE_COLOR);
            for (Point move : validMoves) {
                int centerX = move.x * SQUARE_SIZE + SQUARE_SIZE / 2;
                int centerY = move.y * SQUARE_SIZE + SQUARE_SIZE / 2;
                g2d.fillOval(centerX - 15, centerY - 15, 30, 30);
            }

        }

        private void drawPieces(Graphics2D g2d) {
            if (chessBoard == null) return;

            g2d.setFont(new Font("Serif", Font.PLAIN, 48));

            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    ChessPiece piece = chessBoard.getPiece(col, row);
                    if (piece != null && piece != draggedPiece) {
                        drawPiece(g2d, piece, col * SQUARE_SIZE, row * SQUARE_SIZE);
                    }
                }
            }

            if (draggedPiece != null && dragStart != null) {
                Point mousePos = getMousePosition();
                if (mousePos != null) {
                    drawPiece(g2d, draggedPiece, mousePos.x - SQUARE_SIZE / 2, mousePos.y - SQUARE_SIZE / 2);
                }
            }
        }

        private void drawPiece(@NotNull Graphics2D g2d, @NotNull ChessPiece piece, int x, int y) {
            String symbol = String.valueOf(piece.getIco());
            g2d.setColor(piece.isWhite() ? Color.WHITE : Color.BLACK);

            g2d.setColor(Color.GRAY);
            g2d.drawString(symbol, x + 12, y + 52);

            g2d.setColor(piece.isWhite() ? Color.WHITE : Color.BLACK);
            g2d.drawString(symbol, x + 10, y + 50);
        }


        private void handleMousePressed(MouseEvent e) {
            if (chessBoard == null || chessBoard.getState() != ChessBoard.State.PLAYING) return;

            int col = e.getX() / SQUARE_SIZE;
            int row = e.getY() / SQUARE_SIZE;

            if (col >= 0 && col < 8 && row >= 0 && row < 8) {
                ChessPiece piece = chessBoard.getPiece(col, row);
                if (piece != null && piece.isWhite() == chessBoard.isWhiteTurn()) {
                    dragStart = e.getPoint();
                    draggedPiece = piece;
                    draggedFromX = col;
                    draggedFromY = row;

                    validMoves.clear();
                    ChessMove[] validFields = piece.getValidMoves();
                    for (ChessMove move : validFields) {
                        if (move.getCapturedPiece() != null) {
                            validHitMoves.add(new Point(move.getToX(), move.getToY()));
                        } else {
                            validMoves.add(new Point(move.getToX(), move.getToY()));
                        }
                    }

                    repaint();
                }
            }
        }

        private void handleMouseReleased(MouseEvent e) {
            if (draggedPiece == null) return;

            int col = e.getX() / SQUARE_SIZE;
            int row = e.getY() / SQUARE_SIZE;

            if (col >= 0 && col < 8 && row >= 0 && row < 8) {
                if (col != draggedFromX || row != draggedFromY) {
                    chessBoard.movePiece(draggedFromX, draggedFromY, col, row);
                }
            }

            dragStart = null;
            draggedPiece = null;
            draggedFromX = -1;
            draggedFromY = -1;
            validMoves.clear();
            repaint();
        }

        private void handleMouseDragged(MouseEvent e) {
            if (draggedPiece != null) {
                repaint();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessController whiteController = new VirtualUiController("Human Player");
            ChessController blackController = new VirtualAiController("Ai");

            ChessBoard board = new ChessBoard(whiteController, blackController);
            board.start();

            ChessBoardUi.streamBoard("test_game_1", board);

            ChessBoard board2 = new ChessBoard(whiteController, blackController);
            board2.start();
            ChessBoardUi.streamBoard("test_game_2", board2);
        });
    }
}