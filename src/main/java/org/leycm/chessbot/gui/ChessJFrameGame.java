package org.leycm.chessbot.gui;

import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessPiece;
import org.leycm.chessbot.chess.pieces.*;
import org.leycm.chessbot.model.ChessModel;
import org.leycm.chessbot.model.ModelLoader;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.IOException;
import java.util.*;

public class ChessJFrameGame {

    private static DefaultListModel<String> listModel = new DefaultListModel<>();
    private static JFrame jFrame = new JFrame("Chess Board [UI]");
    private static JPanel jSidePanel = new JPanel(new BorderLayout());
    private static ChessBoard currentBoard = new ChessBoard(true);
    private static HashMap<String, Color> theme = new HashMap<>();
    private static ChessModel model = new ChessModel();
    private static volatile boolean leftMousePressed = false;
    private static volatile boolean rightMousePressed = false;
    private static int fromX = -1;
    private static int fromY = -1;
    private static boolean blackAI = true;
    private static boolean whiteAI = false;

    private static JPanel boardPanel;

    public static void main(String[] args) throws IOException {

        currentBoard = new ChessBoard(true);

        theme.put("board.w", new Color(0xFFCE9E));
        theme.put("board.b", new Color(0xD18B47));
        theme.put("general.1", new Color(0x161B22));
        theme.put("general.2", new Color(0x0C0707));

        model = ModelLoader.loadModel("model/trained/chess_model-1.1.3-R0-SNAPSHOT.model");

        currentBoard.setWhiteTurn(true);

        setupGlobalMouseTracker(jFrame);

        // Ai Move
        new Timer(501, actionEvent -> {

            boolean team = true;

            /*int[] gameState = new int[65];
            int[] levelArray = currentBoard.getLevelArray();

            for (int i = 0; i < levelArray.length; i++) {
                gameState[i] = levelArray[i];
            }

            gameState[64] = currentBoard.isWhiteTurn() ? 1 : 0;

            int[] bestMove = MoveConverter.findBestMove(model, gameState);

            currentBoard.movePiece(bestMove[0], bestMove[1], bestMove[2], bestMove[3]);
            System.out.println(Arrays.toString(bestMove));*/

            /*for (int i = 0; i < 2; i++) {

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

            if (!currentBoard.isWhiteTurn() && blackAI) {
                try {
                    ModelLoader.makeBestMove(currentBoard);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (whiteAI) {
                try {
                    ModelLoader.makeBestMove(currentBoard);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();

        createChessBoardUI();

        //Update Visual

        new Timer(50, actionEvent -> {

            jFrame.setVisible(true);

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

                String movedColor = move.getMovedPiece().isWhite() ? "White" : "Black";

                String capturedStringEnd = "";
                if (move.getCapturedPiece() != null) {

                    String capturedColor = move.getCapturedPiece().isWhite() ? "White" : "Black";
                    capturedStringEnd = " " + move.getCapturedPiece().getName() + " [" + capturedColor + "] ";
                }

                String moveS = move.getMovedPiece().getName() + " [" + movedColor + "] " +
                        move.getFromX() + ", " + move.getFromY() + " ---> " +
                        move.getToX() + ", " + move.getToY() +
                        capturedStringEnd;

                addStringToHistory(moveS);
            });

        }).start();
    }

    public static HashMap<Point, ChessPiece> getNormalChessPieces(ChessBoard board) {

        HashMap<Point, ChessPiece> pieces = new HashMap<>();

        
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

    private static Point getHoveredSquareCoords() {
        try {
            Point mouseScreen = MouseInfo.getPointerInfo().getLocation();

            Point boardScreen = boardPanel.getLocationOnScreen();

            int relX = mouseScreen.x - boardScreen.x;
            int relY = mouseScreen.y - boardScreen.y;

            int squareSize = boardPanel.getWidth() / 8;

            if (relX < 0 || relY < 0 || relX >= boardPanel.getWidth() || relY >= boardPanel.getHeight()) {
                return null;
            }

            int col = relX / squareSize;
            int row = relY / squareSize;

            return new Point(col, row);
        } catch (IllegalComponentStateException e) {
            return null;
        }
    }

    private static void loadVisualBoard() {
        Point hovered = getHoveredSquareCoords();

        boardPanel.removeAll();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JPanel square = new JPanel(new BorderLayout());
                boolean isBlack = (row + col) % 2 != 0;
                square.setBackground(isBlack ? theme.get("board.w") : theme.get("board.b"));

                HashMap<Point, String> points = new HashMap<>();
                currentBoard.getPieces(true).forEach(piece ->
                        points.put(new Point(piece.getX(), piece.getY()), piece.getIco() + "")
                );

                currentBoard.getPieces(false).forEach(piece ->
                        points.put(new Point(piece.getX(), piece.getY()), piece.getIco() + "")
                );

                String text = points.get(new Point(col, row));
                if (text != null) {
                    JLabel label = new JLabel(text, SwingConstants.CENTER);
                    ChessPiece piece = currentBoard.getPiece(col, row);
                    label.setForeground(piece != null && piece.isWhite() ? Color.WHITE : Color.BLACK);
                    int size = jFrame.getHeight() / 8;
                    label.setFont(label.getFont().deriveFont((float) size * 0.7f));
                    square.add(label, BorderLayout.CENTER);
                }

                ChessPiece piece = currentBoard.getPiece(fromX, fromY);
                if (piece != null) {
                    for (int[] validField : piece.getValidFields()) {
                        if (validField[0] == col && validField[1] == row) {
                            square.setBackground(Color.lightGray);
                            if (currentBoard.getPiece(col, row) != null) {
                                square.setBackground(Color.darkGray);
                            }
                        }
                    }
                }

                if (leftMousePressed && currentBoard.getPiece(hovered.x, hovered.y) != null) {
                    if (currentBoard.getPiece(hovered.x, hovered.y).isWhite() == currentBoard.isWhiteTurn()) {
                        fromX = hovered.x;
                        fromY = hovered.y;
                    }
                } else if (rightMousePressed) {
                    if (piece != null && fromX != -1 && piece.isValidMove(hovered.x, hovered.y)) {
                        System.out.println("Moving " + piece.getName());
                        System.out.println("   [" + fromX + ", " + fromY + " --> " + hovered.x + ", " + hovered.y + "]");
                        currentBoard.movePiece(fromX, fromY, hovered.x, hovered.y);
                    } else if (fromX != -1 && fromY != -1){
                        System.out.println("Can't go there");
                    }
                    fromX = -1;
                    fromY = -1;
                }





                if (hovered != null && hovered.x == col && hovered.y == row) {
                    if (fromX != -1 && fromY != -1) {
                        square.setBorder(BorderFactory.createLineBorder(Color.blue, 3));
                    } else {
                        square.setBorder(BorderFactory.createLineBorder(Color.green, 3));
                    }
                } else {
                    square.setBorder(BorderFactory.createEmptyBorder());
                }

                if (col == fromX && row == fromY) {
                    square.setBorder(BorderFactory.createLineBorder(Color.green, 3));
                }

                boardPanel.add(square);
            }
        }
    }

    public static void setupGlobalMouseTracker(Component c) {
        c.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    leftMousePressed = true;
                }
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    rightMousePressed = true;
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    leftMousePressed = false;
                }
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    rightMousePressed = false;
                }
            }
        });
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
