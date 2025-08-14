package org.leycm.chessbot.model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.leycm.chessbot.chess.ChessBoard;
import org.leycm.chessbot.chess.ChessPiece;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoveConverter {

    private static final Pattern UCI_WITH_PROMO = Pattern.compile("^([a-h][1-8])([a-h][1-8])([nbrqNBRQ])?$");
    private static final Pattern ALG_PROMO = Pattern.compile("=([NBRQ])");
    private static final Pattern CLEAN_SUFFIX = Pattern.compile("[+#]+$");

    /**
     * Versucht in dieser Reihenfolge:
     * 1) UCI (e2e4, e7e8q)
     * 2) Rochade (O-O, O-O-O, 0-0, 0-0-0)
     * 3) Algebraische Notation mit Disambiguation, Captures, Promotions
     */
    @Contract("null, _ -> new")
    public static int @NotNull [] moveStringToArray(String moveStr, ChessBoard board) {
        if (moveStr == null) return invalid();
        String s = moveStr.trim();

        int[] uci = tryParseUci(s, board);
        if (uci[0] != -1) return uci;

        int[] castle = tryParseCastling(s, board);
        if (castle[0] != -1) return castle;

        int[] alg = tryParseAlgebraic(s, board);
        if (alg[0] != -1) return alg;

        return invalid();
    }

    /* ===================== UCI ===================== */

    private static int @NotNull [] tryParseUci(@NotNull String s, ChessBoard board) {
        Matcher m = UCI_WITH_PROMO.matcher(s.toLowerCase(Locale.ROOT));
        if (!m.matches()) return invalid();

        int[] from = squareToXY(m.group(1));
        int[] to = squareToXY(m.group(2));
        if (!validSquare(from) || !validSquare(to)) return invalid();

        return new int[]{from[0], from[1], to[0], to[1]};
    }

    /* ===================== Castling ===================== */

    private static int @NotNull [] tryParseCastling(@NotNull String s, ChessBoard board) {
        String t = s.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        boolean kingSide = t.equals("O-O") || t.equals("0-0");
        boolean queenSide = t.equals("O-O-O") || t.equals("0-0-0");

        if (!kingSide && !queenSide) return invalid();

        boolean white = board.isWhiteTurn();
        int y = white ? 7 : 0;

        int fromX = 4; // e
        int toX = kingSide ? 6 : 2; // g oder c

        ChessPiece k = board.getPiece(fromX, y);
        if (k == null || !k.getId().toLowerCase(Locale.ROOT).contains("king") || k.isWhite() != white) {
            return invalid();
        }

        return new int[]{fromX, y, toX, y};
    }

    /* ===================== Algebraisch ===================== */

    private static int[] tryParseAlgebraic(String s, ChessBoard board) {
        s = CLEAN_SUFFIX.matcher(s).replaceAll("").trim();

        Character promo = null;
        Matcher pm = ALG_PROMO.matcher(s);
        if (pm.find()) {
            promo = pm.group(1).charAt(0);
            s = s.replace(pm.group(0), "");
        }

        boolean isCapture = s.contains("x");
        s = s.replace("x", "");

        s = s.replace("e.p.", "").replace("ep", "");

        if (s.length() < 2) return invalid();
        String targetSq = s.substring(s.length() - 2);
        int[] to = squareToXY(targetSq);
        if (!validSquare(to)) return invalid();

        char first = s.charAt(0);
        char pieceType = (Character.isUpperCase(first) && "KQRBN".indexOf(first) >= 0) ? first : 'P';

        String middle = s.substring(pieceType == 'P' ? 0 : 1, s.length() - 2);

        Character disFile = null;
        Integer disRank = null;

        for (char c : middle.toCharArray()) {
            if (c >= 'a' && c <= 'h') disFile = c;
            else if (c >= '1' && c <= '8') disRank = c - '0';
        }

        boolean white = board.isWhiteTurn();

        List<int[]> candidates = new ArrayList<>();
        for (int fromY = 0; fromY < 8; fromY++) {
            for (int fromX = 0; fromX < 8; fromX++) {
                ChessPiece p = board.getPiece(fromX, fromY);
                if (p == null) continue;
                if (p.isWhite() != white) continue;
                if (!matchesPieceType(p, pieceType)) continue;

                if (disFile != null && fromX != (disFile - 'a')) continue;
                if (disRank != null) {
                    int yByRank = 8 - disRank; // invertierte Y
                    if (fromY != yByRank) continue;
                }

                if (p.isValidMove(to[0], to[1])) {
                    candidates.add(new int[]{fromX, fromY, to[0], to[1]});
                } else if (pieceType == 'P' && isPawnDiagonalToEmptyCapture(board, fromX, fromY, to[0], to[1])) {
                    if (isEnPassantPossible(board, fromX, fromY, to[0], to[1])) {
                        candidates.add(new int[]{fromX, fromY, to[0], to[1]});
                    }
                }
            }
        }

        if (candidates.isEmpty()) return invalid();

        return candidates.getFirst();
    }

    /* ===================== Helpers ===================== */

    private static boolean matchesPieceType(@NotNull ChessPiece piece, char pieceType) {
        String id = piece.getId().toLowerCase(Locale.ROOT);
        return switch (pieceType) {
            case 'P' -> id.contains("pawn");
            case 'N' -> id.contains("knight");
            case 'B' -> id.contains("bishop");
            case 'R' -> id.contains("rook");
            case 'Q' -> id.contains("queen");
            case 'K' -> id.contains("king");
            default -> false;
        };
    }

    @Contract(value = " -> new", pure = true)
    private static int @NotNull [] invalid() {
        return new int[]{-1, -1, -1, -1};
    }

    @Contract(pure = true)
    public static @NotNull String arrayToMoveString(int @NotNull [] move) {
        if (move.length < 4 || move[0] < 0) return "";
        char fromFile = (char) ('a' + move[0]);
        char fromRank = (char) ('1' + (7 - move[1])); // invertierte Y
        char toFile = (char) ('a' + move[2]);
        char toRank = (char) ('1' + (7 - move[3]));
        return "" + fromFile + fromRank + toFile + toRank;
    }

    @Contract(pure = true)
    private static boolean validSquare(int @NotNull [] xy) {
        return xy[0] >= 0 && xy[0] < 8 && xy[1] >= 0 && xy[1] < 8;
    }

    /** "e4" -> [4, 4] (y invertiert: 8-4 = 4) */
    private static int @NotNull [] squareToXY(String sq) {
        if (sq == null || sq.length() != 2) return invalid();
        char f = Character.toLowerCase(sq.charAt(0));
        char r = sq.charAt(1);
        if (f < 'a' || f > 'h' || r < '1' || r > '8') return invalid();
        int x = f - 'a';
        int y = 8 - (r - '0'); // y invertiert
        return new int[]{x, y};
    }

    /** Pawn-diagonal auf leeres Zielfeld → potentiell en-passant. */
    private static boolean isPawnDiagonalToEmptyCapture(@NotNull ChessBoard board, int fromX, int fromY, int toX, int toY) {
        ChessPiece p = board.getPiece(fromX, fromY);
        if (p == null) return false;
        if (!p.getId().toLowerCase(Locale.ROOT).contains("pawn")) return false;

        if (board.getPiece(toX, toY) != null) return false;

        int dx = Math.abs(toX - fromX);
        int dy = toY - fromY;
        if (dx != 1) return false;

        if (p.isWhite() && dy != -1) return false;
        if (!p.isWhite() && dy != 1) return false;

        return true;
    }

    /**
     * Prüft einfache En-Passant-Heuristik:
     * - Letzter gegnerischer Zug war Doppelzug eines Bauern
     * - Der überquerte Square ist unser Zielfeld (toX,toY)
     */
    private static boolean isEnPassantPossible(@NotNull ChessBoard board, int fromX, int fromY, int toX, int toY) {
        List<ChessBoard.Move> hist = board.getMoveHistory();
        if (hist.isEmpty()) return false;

        ChessBoard.Move last = hist.get(hist.size() - 1);
        ChessPiece moved = last.movedPiece();
        if (moved == null) return false;
        if (!moved.getId().toLowerCase(Locale.ROOT).contains("pawn")) return false;

        int dy = last.toY() - last.fromY();
        if (Math.abs(dy) != 2) return false;

        int passedY = last.fromY() + (dy > 0 ? 1 : -1);
        int passedX = last.fromX();

        if (toX != passedX || toY != passedY) return false;

        return true;
    }

    /* ===================== Best-Move & Index-Helfer (unverändert) ===================== */

    public static int @NotNull [] findBestMove(@NotNull ChessModel model, int[] boardState) {
        double[] predictions = model.predict(boardState);

        // Schutz gegen NaNs/Inf
        int bestMoveIndex = 0;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < predictions.length; i++) {
            double v = predictions[i];
            if (Double.isNaN(v) || Double.isInfinite(v)) continue;
            if (v > bestScore) {
                bestScore = v;
                bestMoveIndex = i;
            }
        }

        return indexToMove(bestMoveIndex);
    }

    @Contract(value = "_ -> new", pure = true)
    private static int @NotNull [] indexToMove(int index) {
        int fromX = index / 512;
        int fromY = (index % 512) / 64;
        int toX = ((index % 512) % 64) / 8;
        int toY = ((index % 512) % 64) % 8;
        return new int[]{fromX, fromY, toX, toY};
    }
}
