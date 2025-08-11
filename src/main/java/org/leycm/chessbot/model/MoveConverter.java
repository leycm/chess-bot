package org.leycm.chessbot.model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.Piece;

public class MoveConverter {

    @Contract("null, _ -> new")
    public static int @NotNull [] moveStringToArray(String moveStr, ChessBoard board) {
        if (moveStr == null || moveStr.length() < 2) return new int[]{-1, -1, -1, -1};

        moveStr = moveStr.replaceAll("[+#=NBRQ]", "").trim();

        try {
            if (moveStr.length() >= 4 && moveStr.charAt(1) >= '1' && moveStr.charAt(1) <= '8') {
                int fromX = moveStr.charAt(0) - 'a';
                int fromY = 8 - Character.getNumericValue(moveStr.charAt(1));
                int toX = moveStr.charAt(2) - 'a';
                int toY = 8 - Character.getNumericValue(moveStr.charAt(3));

                if (board.isValidCoord(fromX, fromY) && board.isValidCoord(toX, toY)) {
                    return new int[]{fromX, fromY, toX, toY};
                }
            }

            return parseAlgebraicNotation(moveStr, board);

        } catch (Exception e) {
            return new int[]{-1, -1, -1, -1};
        }
    }

    @Contract("_, _ -> new")
    private static int @NotNull [] parseAlgebraicNotation(String move, ChessBoard board) {
        move = move.replaceAll("x", "").replaceAll("[+#]", "").trim();

        if (move.length() < 2) return new int[]{-1, -1, -1, -1};

        char targetFile = move.charAt(move.length() - 2);
        char targetRank = move.charAt(move.length() - 1);

        if (targetFile < 'a' || targetFile > 'h' || targetRank < '1' || targetRank > '8') {
            return new int[]{-1, -1, -1, -1};
        }

        int toX = targetFile - 'a';
        int toY = 8 - Character.getNumericValue(targetRank);

        char pieceType = move.length() > 2 && Character.isUpperCase(move.charAt(0)) ? move.charAt(0) : 'P';

        for (int fromY = 0; fromY < 8; fromY++) {
            for (int fromX = 0; fromX < 8; fromX++) {
                Piece piece = board.getPiece(fromX, fromY);
                if (piece == null) continue;

                if (matchesPieceType(piece, pieceType) &&
                        piece.isWhite() == board.isWhiteOnMove() &&
                        canMoveTo(piece, fromX, fromY, toX, toY, board)) {

                    if (move.length() > 2 && Character.isLowerCase(move.charAt(1))) {
                        char sourceFile = move.charAt(1);
                        if (fromX != sourceFile - 'a') continue;
                    }

                    return new int[]{fromX, fromY, toX, toY};
                }
            }
        }

        return new int[]{-1, -1, -1, -1};
    }

    private static boolean matchesPieceType(@NotNull Piece piece, char pieceType) {
        String pieceName = piece.getId();
        return switch (pieceType) {
            case 'P' -> pieceName.contains("pawn");
            case 'N' -> pieceName.contains("knight");
            case 'B' -> pieceName.contains("bishop");
            case 'R' -> pieceName.contains("rook");
            case 'Q' -> pieceName.contains("queen");
            case 'K' -> pieceName.contains("king");
            default -> false;
        };
    }

    private static boolean canMoveTo(Piece piece, int fromX, int fromY, int toX, int toY, @NotNull ChessBoard board) {
        if (!board.isValidCoord(toX, toY)) return false;

        Piece targetPiece = board.getPiece(toX, toY);
        if (targetPiece != null && targetPiece.isWhite() == piece.isWhite()) return false;

        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);

        String pieceName = piece.getClass().getSimpleName().toLowerCase();

        if (pieceName.contains("pawn")) {
            int direction = piece.isWhite() ? -1 : 1;
            if (targetPiece == null) {
                return (toX == fromX && toY == fromY + direction) ||
                        (toX == fromX && toY == fromY + 2 * direction && !piece.hasMovedJet);
            } else {
                return dx == 1 && toY == fromY + direction;
            }
        } else if (pieceName.contains("rook")) {
            return (dx == 0 || dy == 0) && isPathClear(fromX, fromY, toX, toY, board);
        } else if (pieceName.contains("bishop")) {
            return dx == dy && isPathClear(fromX, fromY, toX, toY, board);
        } else if (pieceName.contains("queen")) {
            return (dx == 0 || dy == 0 || dx == dy) && isPathClear(fromX, fromY, toX, toY, board);
        } else if (pieceName.contains("knight")) {
            return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
        } else if (pieceName.contains("king")) {
            return dx <= 1 && dy <= 1;
        }

        return false;
    }

    private static boolean isPathClear(int fromX, int fromY, int toX, int toY, ChessBoard board) {
        int dx = Integer.signum(toX - fromX);
        int dy = Integer.signum(toY - fromY);

        int x = fromX + dx;
        int y = fromY + dy;

        while (x != toX || y != toY) {
            if (board.getPiece(x, y) != null) return false;
            x += dx;
            y += dy;
        }

        return true;
    }

    @Contract(pure = true)
    public static @NotNull String arrayToMoveString(int @NotNull [] move) {
        if (move.length < 4 || move[0] < 0) return "";

        char fromFile = (char) ('a' + move[0]);
        char fromRank = (char) ('1' + (7 - move[1]));
        char toFile = (char) ('a' + move[2]);
        char toRank = (char) ('1' + (7 - move[3]));

        return "" + fromFile + fromRank + toFile + toRank;
    }

    public static int @NotNull [] findBestMove(@NotNull ChessModel model, int[] boardState) {
        double[] predictions = model.predict(boardState);

        int bestMoveIndex = 0;
        double bestScore = predictions[0];

        for (int i = 1; i < predictions.length; i++) {
            if (predictions[i] > bestScore) {
                bestScore = predictions[i];
                bestMoveIndex = i;
            }
        }

        return indexToMove(bestMoveIndex);
    }

    @Contract(value = "_ -> new", pure = true)
    private static int @NotNull [] indexToMove(int index) {
        int fromX = index / 512;
        int fromY = (index % 512) / 64;
        int toX = (index % 64) / 8;
        int toY = index % 8;

        return new int[]{fromX, fromY, toX, toY};
    }
}