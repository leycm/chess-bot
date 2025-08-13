package org.leycm.chessbot.gui;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;
import org.leycm.chessbot.chess.pieces.*;
import org.leycm.chessbot.model.ChessModel;
import org.leycm.chessbot.model.ModelLoader;
import org.leycm.chessbot.model.MoveConverter;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.IOException;
import java.util.*;

public class ChessJFrameGame {

    private static DefaultListModel<String> listModel = new DefaultListModel<>();
    private static JFrame jFrame = new JFrame("Chess Board [UI]");
    private static JPanel jSidePanel = new JPanel(new BorderLayout());
    private static ChessBoard currentBoard = new ChessBoard();
    private static HashMap<String, Color> theme = new HashMap<>();
    private static ChessModel model = new ChessModel();

    private static JPanel boardPanel;

    public static void main(String[] args) throws IOException {

        currentBoard = new ChessBoard();

        theme.put("board.w", new Color(0xFFCE9E));
        theme.put("board.b", new Color(0xD18B47));
        theme.put("general.1", new Color(0x161B22));
        theme.put("general.2", new Color(0x0C0707));

        getNormalChessPieces(currentBoard).forEach((point, piece) -> {
            currentBoard.placePiece(piece, point.x, point.y);
        });

        currentBoard.movePiece(6, 1, 6, 2);

        model = ModelLoader.loadModel("model/trained/chess_model-1.1.2-R0-SNAPSHOT.model");

        currentBoard.setWhiteTurn(true);

        createChessBoardUI();
        new Timer(250, e -> {

            jFrame.setVisible(true);
            boolean team = true;

            int[] gameState = new int[65];
            int[] levelArray = currentBoard.getLevelArray();

            for (int i = 0; i < levelArray.length; i++) {
                gameState[i] = levelArray[i];
            }

            gameState[64] = currentBoard.isWhiteTurn() ? 1 : 0;

            int[] bestMove = MoveConverter.findBestMove(model, gameState);

            currentBoard.movePiece(bestMove[0], bestMove[1], bestMove[2], bestMove[3]);
            System.out.println(Arrays.toString(bestMove));

            /*or (int i = 0; i < 2; i++) {

                Piece piece = currentBoard.getPieces(team).get(new Random().nextInt(0, currentBoard.getPieces(team).size()));
                int[] cord = Arrays.stream(piece.getValidFields()).findAny().orElse(null);
                if (cord != null) {
                    System.out.println(piece.getName() + ": " + piece.getX() + " " + piece.getY() + " | " + cord[0] + " " + cord[1]);
                    currentBoard.movePiece(piece.getX(), piece.getY(), cord[0], cord[1]);
                } else {
                    System.out.println("can't Load Piece: " + piece.getName());
                }
                team = !team;
            }*/

            if (jFrame.getWidth() < 400 || jFrame.getHeight() < 250) {
                jFrame.setSize(400, 250);
            }

            int width = jFrame.getWidth() - jFrame.getHeight();
            jSidePanel.setPreferredSize(new Dimension(width, jFrame.getHeight()));
            jSidePanel.revalidate();
            if (jFrame.getHeight() > jFrame.getWidth()) {
                jFrame.setSize(new Dimension(jFrame.getWidth(), jFrame.getWidth()));
            }

            loadVisualBoard();

            listModel.removeAllElements();
            currentBoard.getMoveHistory().forEach(move -> {

                String movedColor = move.movedPiece().isWhite() ? "White" : "Black";

                String capturedStringEnd = "";
                if (move.capturedPiece() != null) {

                    String capturedColor = move.capturedPiece().isWhite() ? "White" : "Black";
                    capturedStringEnd = " " + move.capturedPiece().getName() + " [" + capturedColor + "] ";
                }

                String moveS = move.movedPiece().getName() + " [" + movedColor + "] " +
                        move.fromX() + ", " + move.fromY() + " ---> " +
                        move.toX() + ", " + move.toY() +
                        capturedStringEnd;

                addStringToHistory(moveS);
            });

        }).start();
    }

    public static HashMap<Point, Piece> getNormalChessPieces(ChessBoard board) {

        HashMap<Point, Piece> pieces = new HashMap<>();

        // White
        pieces.put(new Point(0, 7), new RookChessPiece(true, board));
        pieces.put(new Point(1, 7), new KnightChessPiece(true, board));
        pieces.put(new Point(2, 7), new BishopChessPiece(true, board));
        pieces.put(new Point(3, 7), new KingChessPiece(true, board));
        pieces.put(new Point(4, 7), new QueenChessPiece(true, board));
        pieces.put(new Point(5, 7), new BishopChessPiece(true, board));
        pieces.put(new Point(6, 7), new KnightChessPiece(true, board));
        pieces.put(new Point(7, 7), new RookChessPiece(true, board));

        for (int y = 0; y < 8; y++) {
            pieces.put(new Point(y, 6), new PawnChessPiece(true, board));
        }

        // Black
        pieces.put(new Point(0, 0), new RookChessPiece(false, board));
        pieces.put(new Point(1, 0), new KnightChessPiece(false, board));
        pieces.put(new Point(2, 0), new BishopChessPiece(false, board));
        pieces.put(new Point(3, 0), new KingChessPiece(false, board));
        pieces.put(new Point(4, 0), new QueenChessPiece(false, board));
        pieces.put(new Point(5, 0), new BishopChessPiece(false, board));
        pieces.put(new Point(6, 0), new KnightChessPiece(false, board));
        pieces.put(new Point(7, 0), new RookChessPiece(false, board));

        for (int y = 0; y < 8; y++) {
            pieces.put(new Point(y, 1), new PawnChessPiece(false, board));
        }

        return pieces;
    }


    public static void createChessBoardUI() {
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setSize(900, 600);
        jFrame.setLayout(new BorderLayout());
        jFrame.setResizable(true);

        boardPanel = new JPanel(new GridLayout(8, 8)) {
            @Override
            public Dimension getPreferredSize() {
                int size = computeSquareSize(this);
                return new Dimension(size, size);
            }
        };

        loadVisualBoard();

        JList<String> list = new JList<>(listModel);
        JScrollPane listScrollPane = new JScrollPane(list);
        JLabel title = new JLabel("Match History", SwingConstants.CENTER);

        jSidePanel.add(title, BorderLayout.NORTH);
        jSidePanel.add(listScrollPane, BorderLayout.CENTER);
        list.setBackground(theme.get("general.1"));
        jSidePanel.setBackground(theme.get("general.2"));
        list.setForeground(Color.white);
        title.setForeground(Color.WHITE);

        jFrame.add(boardPanel, BorderLayout.CENTER);
        jFrame.add(jSidePanel, BorderLayout.EAST);
    }

    private static void loadVisualBoard() {

        boardPanel.removeAll();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JPanel square = new JPanel(new BorderLayout());
                boolean isBlack = (row + col) % 2 != 0;
                square.setBackground(isBlack ? theme.get("board.w") : theme.get("board.b"));

                HashMap<Point, String> points = new HashMap<>();

                currentBoard.getPieces(true).forEach(piece -> {
                    points.put(new Point(piece.getX(), piece.getY()), piece.getIco() + "");
                });
                currentBoard.getPieces(false).forEach(piece -> {
                    points.put(new Point(piece.getX(), piece.getY()), piece.getIco() + "");
                });

                String text = points.get(new Point(col, row));
                if (text != null) {
                    JLabel label = new JLabel(text, SwingConstants.CENTER);
                    Piece piece = currentBoard.getPiece(col, row);
                    if (piece != null) {
                        label.setForeground(piece.isWhite() ? Color.WHITE : Color.BLACK);
                    } else {
                        label.setForeground(Color.RED);
                    }
                    int size = jFrame.getHeight() / 8;
                    label.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
                    label.setFont(label.getFont().deriveFont((float) size * 0.7f));
                    square.add(label, BorderLayout.CENTER);
                }

                boardPanel.add(square);
            }
        }
    }

    private static int computeSquareSize(JPanel panel) {
        int width = panel.getParent() != null ? panel.getParent().getWidth() : panel.getWidth();
        int height = panel.getParent() != null ? panel.getParent().getHeight() : panel.getHeight();
        if (width == 0 || height == 0) {
            return 400;
        }
        return Math.min(width, height);
    }

    public static void addStringToHistory(String text) {
        listModel.addElement(text);
    }

    public static void addJPanelToSideBar(JPanel panel) {
        jSidePanel.add(panel);
    }
}
