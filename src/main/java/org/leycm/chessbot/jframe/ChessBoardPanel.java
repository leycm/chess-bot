package org.leycm.chessbot.jframe;

import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessMove;
import org.leycm.chessbot.chess.ChessPiece;
import org.leycm.chessbot.chess.pieces.KingChessPiece;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom JPanel that draws the chess board and handles mouse interactions.
 */
public class ChessBoardPanel extends JPanel {
    private final ChessBoard chessBoard;
    private final NavigationController navigationController;
    private final MouseInteractionHandler mouseHandler;

    @Setter
    private Point dragStart = null;
    private ChessPiece draggedPiece = null;
    private int draggedFromX = -1, draggedFromY = -1;

    private ChessMove thisMove = null;
    private final Set<Point> validMoves = new HashSet<>();
    private final Set<Point> validHitMoves = new HashSet<>();
    private Point kingInCheck = null;

    public ChessBoardPanel(ChessBoard chessBoard, NavigationController navigationController) {
        this.chessBoard = chessBoard;
        this.navigationController = navigationController;
        this.mouseHandler = new MouseInteractionHandler(this);

        setPreferredSize(new Dimension(ChessConstants.BOARD_SIZE, ChessConstants.BOARD_SIZE));
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void updateBoard() {
        updateKingInCheckStatus();
        repaint();
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
                Color squareColor = (row + col) % 2 == 0 ?
                        ChessConstants.LIGHT_SQUARE : ChessConstants.DARK_SQUARE;
                g2d.setColor(squareColor);
                g2d.fillRect(col * ChessConstants.SQUARE_SIZE, row * ChessConstants.SQUARE_SIZE,
                        ChessConstants.SQUARE_SIZE, ChessConstants.SQUARE_SIZE);
            }
        }

        g2d.setColor(Color.BLACK);
        g2d.setFont(ChessConstants.COORDINATE_FONT);

        for (int i = 0; i < 8; i++) {
            char file = (char) ('a' + i);
            g2d.drawString(String.valueOf(file),
                    i * ChessConstants.SQUARE_SIZE + 5,
                    ChessConstants.BOARD_SIZE - 5);
        }

        for (int i = 0; i < 8; i++) {
            int rank = 8 - i;
            g2d.drawString(String.valueOf(rank), 5, i * ChessConstants.SQUARE_SIZE + 15);
        }
    }

    private void drawHighlights(Graphics2D g2d) {
        ChessMove lastMove = navigationController.getLastMove();
        if (lastMove != null) {
            g2d.setColor(ChessConstants.LAST_MOVE_COLOR);
            drawSquareHighlight(g2d, lastMove.getFromX(), lastMove.getFromY());
            drawSquareHighlight(g2d, lastMove.getToX(), lastMove.getToY());
        }

        if (thisMove != null) { // try 0  && navigationController.isAtLatestPosition()
            g2d.setColor(ChessConstants.VALID_MOVE_COLOR);
            drawSquareHighlight(g2d, thisMove.getFromX(), thisMove.getFromY());
        }

        if (kingInCheck != null) {
            g2d.setColor(ChessConstants.CHECK_COLOR);
            drawSquareHighlight(g2d, kingInCheck.x, kingInCheck.y);
        }

        if (true) { // try 0  navigationController.isAtLatestPosition()
            g2d.setColor(ChessConstants.VALID_MOVE_COLOR);
            drawValidMoves(g2d);
        }
    }

    private void drawSquareHighlight(Graphics2D g2d, int x, int y) {
        g2d.fillRect(x * ChessConstants.SQUARE_SIZE, y * ChessConstants.SQUARE_SIZE,
                ChessConstants.SQUARE_SIZE, ChessConstants.SQUARE_SIZE);
    }

    private void drawValidMoves(Graphics2D g2d) {
        for (Point move : validMoves) {
            int centerX = move.x * ChessConstants.SQUARE_SIZE + ChessConstants.SQUARE_SIZE / 2;
            int centerY = move.y * ChessConstants.SQUARE_SIZE + ChessConstants.SQUARE_SIZE / 2;
            g2d.fillOval(centerX - ChessConstants.MOVE_DOT_RADIUS,
                    centerY - ChessConstants.MOVE_DOT_RADIUS,
                    ChessConstants.MOVE_DOT_SIZE, ChessConstants.MOVE_DOT_SIZE);
        }

        for (Point move : validHitMoves) {
            int centerX = move.x * ChessConstants.SQUARE_SIZE + ChessConstants.SQUARE_SIZE / 2;
            int centerY = move.y * ChessConstants.SQUARE_SIZE + ChessConstants.SQUARE_SIZE / 2;

            g2d.fillRect(centerX - ChessConstants.SQUARE_SIZE / 2,
                    centerY - ChessConstants.SQUARE_SIZE / 2,
                    ChessConstants.SQUARE_SIZE, ChessConstants.SQUARE_SIZE);

            boolean isLight = (move.x + move.y) % 2 == 0;
            g2d.setColor(isLight ? ChessConstants.LIGHT_SQUARE : ChessConstants.DARK_SQUARE);
            g2d.fillOval(centerX - ChessConstants.SQUARE_SIZE / 2,
                    centerY - ChessConstants.SQUARE_SIZE / 2,
                    ChessConstants.SQUARE_SIZE, ChessConstants.SQUARE_SIZE);

            g2d.setColor(ChessConstants.VALID_MOVE_COLOR);
        }
    }

    private void drawPieces(Graphics2D g2d) {
        navigationController.navigateToMove(navigationController.getCurrentMoveIndex());
        ChessBoard displayBoard = navigationController.getDisplayBoard();
        if (displayBoard == null) return;
        displayBoard.start();

        g2d.setFont(ChessConstants.PIECE_FONT);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = displayBoard.getPiece(col, row);
                if (piece != null && piece != draggedPiece) {
                    drawPiece(g2d, piece, col * ChessConstants.SQUARE_SIZE, row * ChessConstants.SQUARE_SIZE);
                }
            }
        }

        if (draggedPiece != null && dragStart != null) { // try 0 && navigationController.isAtLatestPosition()
            Point mousePos = getMousePosition();
            if (mousePos != null) {
                drawPiece(g2d, draggedPiece,
                        mousePos.x - ChessConstants.SQUARE_SIZE / 2,
                        mousePos.y - ChessConstants.SQUARE_SIZE / 2);
            }
        }
    }

    private void drawPiece(@NotNull Graphics2D g2d, @NotNull ChessPiece piece, int x, int y) {
        String symbol = String.valueOf(piece.getIco());

        g2d.setColor(Color.GRAY);
        g2d.drawString(symbol, x + 12, y + 52);

        g2d.setColor(piece.isWhite() ? Color.WHITE : Color.BLACK);
        g2d.drawString(symbol, x + 10, y + 50);
    }

    private void updateKingInCheckStatus() {
        kingInCheck = null;
        ChessBoard displayBoard = navigationController.getDisplayBoard();

        if (displayBoard != null) {
            boolean currentTurn = displayBoard.isWhiteTurn();
            if (displayBoard.isKingInCheck(currentTurn)) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        ChessPiece piece = displayBoard.getPiece(x, y);
                        if (piece instanceof KingChessPiece && piece.isWhite() == currentTurn) {
                            kingInCheck = new Point(x, y);
                            return;
                        }
                    }
                }
            }
        }
    }

    public void handlePieceSelection(int col, int row) {
        ChessPiece piece = chessBoard.getPiece(col, row);
        if (piece != null && piece.isWhite() == chessBoard.isWhiteTurn()) {
            draggedPiece = piece;
            draggedFromX = col;
            draggedFromY = row;

            thisMove = new ChessMove(piece.getX(), piece.getY(), -1, -1, chessBoard);

            validMoves.clear();
            validHitMoves.clear();
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

    public void handlePieceMove(int toCol, int toRow) {
        if (!navigationController.isAtLatestPosition()) {
            ChessBoardUi.streamBoard(navigationController.getId() + "->" +
                    navigationController.getCurrentMoveIndex() + "clone", navigationController.getDisplayBoard());
            navigationController.navigateToLast();
            navigationController.navigateToNext();
        }

        if (toCol != draggedFromX || toRow != draggedFromY) {
            chessBoard.movePiece(draggedFromX, draggedFromY, toCol, toRow);
        }
        clearDragState();
    }

    public void clearDragState() {
        dragStart = null;
        draggedPiece = null;
        draggedFromX = -1;
        draggedFromY = -1;
        thisMove = null;
        validMoves.clear();
        validHitMoves.clear();
        repaint();
    }

    public boolean canInteract() {
        return chessBoard != null &&
                chessBoard.getState() == ChessBoard.State.PLAYING && // try 0 navigationController.isAtLatestPosition() &&
                !chessBoard.isAiTurn();
    }

    public boolean isDragging() {
        return draggedPiece != null;
    }
}
