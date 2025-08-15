package org.leycm.chessbot.jframe;

import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.*;
import org.leycm.chessbot.chess.controller.VirtualAiController;
import org.leycm.chessbot.chess.controller.VirtualUiController;
import org.leycm.chessbot.chess.pieces.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Main Chess Board UI class that manages the chess game display and user interactions.
 */
public class ChessBoardUi extends JFrame {
    private static final Map<String, ChessBoardUi> ACTIVE_BOARDS = new ConcurrentHashMap<>();

    private final String boardId;
    private ChessBoard chessBoard;
    private ChessBoardPanel boardPanel;
    private MoveHistoryPanel moveHistoryPanel;
    private NavigationController navigationController;
    private GameStateDisplay gameStateDisplay;

    private ChessBoardUi(String id, ChessBoard board) {
        this.boardId = id;
        this.chessBoard = board;
        this.navigationController = new NavigationController(id, board);
        initializeUI();
        updateBoard();
        startUpdateTimer();
    }

    /**
     * Static method to create or update a chess board UI.
     */
    public static void streamBoard(String id, ChessBoard board) {
        SwingUtilities.invokeLater(() -> {
            ChessBoardUi existingUI = ACTIVE_BOARDS.get(id);
            if (existingUI != null) {
                existingUI.updateChessBoard(board);
                existingUI.toFront();
                existingUI.requestFocus();
            } else {
                ChessBoardUi newUI = new ChessBoardUi(id, board);
                ACTIVE_BOARDS.put(id, newUI);
                newUI.setVisible(true);
            }
        });
    }

    private void updateChessBoard(ChessBoard board) {
        this.chessBoard = board;
        this.navigationController.setChessBoard(board);
        if (navigationController.isAtLatestPosition()) {
            navigationController.setDisplayBoard(board);
        }
        updateBoard();
    }

    private void initializeUI() {
        setTitle("Chess Board - " + boardId);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        boardPanel = new ChessBoardPanel(chessBoard, navigationController);
        moveHistoryPanel = new MoveHistoryPanel(navigationController);
        gameStateDisplay = new GameStateDisplay();

        add(boardPanel, BorderLayout.CENTER);
        add(createSidePanel(), BorderLayout.EAST);
        add(gameStateDisplay, BorderLayout.SOUTH);

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
        sidePanel.setPreferredSize(new Dimension(280, ChessConstants.BOARD_SIZE));
        sidePanel.setBackground(ChessConstants.PANEL_BACKGROUND);

        sidePanel.add(moveHistoryPanel.createHistoryHeaderLabel(), BorderLayout.NORTH);
        sidePanel.add(moveHistoryPanel, BorderLayout.CENTER);
        sidePanel.add(new GameControlPanel(chessBoard, navigationController), BorderLayout.SOUTH);

        return sidePanel;
    }

    private void startUpdateTimer() {
        new Timer(100, e -> {
            if (chessBoard != null) {
                updateBoard();
            }
        }).start();
    }

    public void updateBoard() {
        if (chessBoard == null) return;

        SwingUtilities.invokeLater(() -> {
            gameStateDisplay.updateDisplay(chessBoard, navigationController);
            moveHistoryPanel.updateMoveHistory(chessBoard);
            boardPanel.updateBoard();
        });
    }

    /**
     * Main method for testing the chess UI.
     */
    public static void main(String[] args) {
        ChessController whiteController = new VirtualUiController("Human Player");
        //ChessController whiteController = new VirtualAiController("AI");
        ChessController blackController = new VirtualUiController("Human Player");
        //ChessController blackController = new VirtualAiController("AI");

        ChessBoard board = new ChessBoard(ChessBoardUi::setupStandardChessBoard, whiteController, blackController);

        board.start();

        new Timer(20, _ -> board.tick()).start();

        SwingUtilities.invokeLater(() -> {
            ChessBoardUi.streamBoard("test_game_1", board);
        });
    }

    private static void setupStandardChessBoard(ChessBoard board) {
        // Pawns
        for (int x = 0; x < 8; x++) {
            board.placePiece(new PawnChessPiece(false, board), x, 1);
            board.placePiece(new PawnChessPiece(true, board), x, 6);
        }

        // Black pieces (top row)
        board.placePiece(new RookChessPiece(false, board), 0, 0);
        board.placePiece(new KnightChessPiece(false, board), 1, 0);
        board.placePiece(new BishopChessPiece(false, board), 2, 0);
        board.placePiece(new KingChessPiece(false, board), 3, 0);
        board.placePiece(new QueenChessPiece(false, board), 4, 0);
        board.placePiece(new BishopChessPiece(false, board), 5, 0);
        board.placePiece(new KnightChessPiece(false, board), 6, 0);
        board.placePiece(new RookChessPiece(false, board), 7, 0);

        // White pieces (bottom row)
        board.placePiece(new RookChessPiece(true, board), 0, 7);
        board.placePiece(new KnightChessPiece(true, board), 1, 7);
        board.placePiece(new BishopChessPiece(true, board), 2, 7);
        board.placePiece(new KingChessPiece(true, board), 3, 7);
        board.placePiece(new QueenChessPiece(true, board), 4, 7);
        board.placePiece(new BishopChessPiece(true, board), 5, 7);
        board.placePiece(new KnightChessPiece(true, board), 6, 7);
        board.placePiece(new RookChessPiece(true, board), 7, 7);
    }
}