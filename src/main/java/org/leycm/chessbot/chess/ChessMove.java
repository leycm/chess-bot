package org.leycm.chessbot.chess;

import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.leycm.chessbot.chess.pieces.PawnChessPiece;

import java.util.Arrays;
import java.util.List;

@Data
public class ChessMove {
    private final ChessPiece movedPiece;
    private final ChessPiece capturedPiece;
    private final int fromX, fromY;
    private final int toX, toY;
    private final boolean isPromotion;
    private final char promotionPiece;
    private final boolean isCastling;
    private final boolean isEnPassant;

    private final ChessBoard board;

    @Contract("_, _ -> new")
    public static @NotNull ChessMove algebraic(@NotNull String algebraic, ChessBoard board) {
        
        if (algebraic.endsWith("+") || algebraic.endsWith("#")) {
            algebraic = algebraic.substring(0, algebraic.length() - 1);
        }

        
        if (algebraic.equals("O-O") || algebraic.equals("0-0")) {
            return castling(true, board.isWhiteTurn(), board);
        }
        if (algebraic.equals("O-O-O") || algebraic.equals("0-0-0")) {
            return castling(false, board.isWhiteTurn(), board);
        }

        
        ChessMove move = tryParseMove(algebraic, board);
        if (move.isValid()) {
            return move;
        }

        
        return new ChessMove(-1, -1, -1, -1, board);
    }

    private static @NotNull ChessMove tryParseMove(@NotNull String algebraic, @NotNull ChessBoard board) {
        boolean whiteTurn = board.isWhiteTurn();
        
        
        if (algebraic.length() == 2 && Character.isLowerCase(algebraic.charAt(0))) {
            return tryPawnMove(algebraic, whiteTurn, board);
        }

        
        if (algebraic.length() == 4 && Character.isLowerCase(algebraic.charAt(0)) && algebraic.charAt(1) == 'x') {
            return tryPawnCapture(algebraic, whiteTurn, board);
        }

        
        if (algebraic.contains("=")) {
            return tryPawnPromotion(algebraic, whiteTurn, board);
        }

        
        if (algebraic.length() == 3) {
            return tryPieceMove(algebraic, whiteTurn, board);
        }

        
        if (algebraic.length() == 4 && algebraic.charAt(1) == 'x') {
            return tryPieceCapture(algebraic, whiteTurn, board);
        }

        
        if (algebraic.length() == 4 && algebraic.charAt(1) != 'x') {
            return tryDisambiguatedMove(algebraic, whiteTurn, board);
        }

        
        if (algebraic.length() == 5 && algebraic.charAt(2) == 'x') {
            return tryDisambiguatedCapture(algebraic, whiteTurn, board);
        }

        
        if (algebraic.length() == 5 && algebraic.charAt(2) != 'x') {
            return tryDisambiguatedMove(algebraic, whiteTurn, board);
        }

        
        if (algebraic.length() == 6) {
            return tryFullNotationCapture(algebraic, whiteTurn, board);
        }

        return new ChessMove(-1, -1, -1, -1, board);
    }

    private static @NotNull ChessMove tryPawnMove(@NotNull String algebraic, boolean whiteTurn, ChessBoard board) {
        int toX = algebraic.charAt(0) - 'a';
        int toY = 8 - (algebraic.charAt(1) - '0');
        
        
        ChessPiece pawn = findPawnForMoveAlternative(toX, toY, whiteTurn, board);
        if (pawn != null) {
            return new ChessMove(pawn.getX(), pawn.getY(), toX, toY, board);
        }
        
        pawn = findPawnForMoveAggressive(toX, toY, whiteTurn, board);
        if (pawn != null) {
            return new ChessMove(pawn.getX(), pawn.getY(), toX, toY, board);
        }
        
        return new ChessMove(-1, -1, -1, -1, board);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove tryPawnCapture(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        int fromX = algebraic.charAt(0) - 'a';
        int toX = algebraic.charAt(2) - 'a';
        int toY = 8 - (algebraic.charAt(3) - '0');
        
        
        List<ChessPiece> pawns = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getName().equalsIgnoreCase("Pawn"))
                .filter(piece -> piece.getX() == fromX)
                .toList();

        for (ChessPiece pawn : pawns) {
            if (canPawnCaptureTo(pawn, toX, toY, board)) {
                return new ChessMove(pawn.getX(), pawn.getY(), toX, toY, board);
            }
        }
        
        
        ChessPiece pawn = findPawnForCaptureAggressive(fromX, toX, toY, whiteTurn, board);
        if (pawn != null) {
            return new ChessMove(pawn.getX(), pawn.getY(), toX, toY, board);
        }
        
        return new ChessMove(-1, -1, -1, -1, board);
    }

    private static @NotNull ChessMove tryPawnPromotion(@NotNull String algebraic, boolean whiteTurn, ChessBoard board) {
        int equalIndex = algebraic.indexOf('=');
        char promotionPiece = algebraic.charAt(equalIndex + 1);
        String movepart = algebraic.substring(0, equalIndex);

        int toX, toY, fromX;

        if (movepart.contains("x")) {
            
            fromX = movepart.charAt(0) - 'a';
            toX = movepart.charAt(2) - 'a';
            toY = 8 - (movepart.charAt(3) - '0');
        } else {
            
            toX = movepart.charAt(0) - 'a';
            toY = 8 - (movepart.charAt(1) - '0');
            fromX = toX;
        }

        
        List<ChessPiece> pawns = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getName().equalsIgnoreCase("Pawn"))
                .filter(piece -> piece.getX() == fromX)
                .toList();

        for (ChessPiece pawn : pawns) {
            if (canPawnMoveTo(pawn, toX, toY, board)) {
                return new ChessMove(pawn.getX(), pawn.getY(), toX, toY, board, true, promotionPiece, false, false);
            }
        }

        return new ChessMove(-1, -1, -1, -1, board);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove tryPieceMove(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        char pieceChar = algebraic.charAt(0);
        int toX = algebraic.charAt(1) - 'a';
        int toY = 8 - (algebraic.charAt(2) - '0');

        
        List<ChessPiece> pieces = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getChar() == pieceChar)
                .toList();

        for (ChessPiece piece : pieces) {
            if (piece.isValidMove(toX, toY)) {
                return new ChessMove(piece.getX(), piece.getY(), toX, toY, board);
            }
        }

        return new ChessMove(-1, -1, -1, -1, board);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove tryPieceCapture(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        char pieceChar = algebraic.charAt(0);
        int toX = algebraic.charAt(2) - 'a';
        int toY = 8 - (algebraic.charAt(3) - '0');

        
        List<ChessPiece> pieces = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getChar() == pieceChar)
                .toList();

        for (ChessPiece piece : pieces) {
            if (piece.isValidMove(toX, toY)) {
                return new ChessMove(piece.getX(), piece.getY(), toX, toY, board);
            }
        }

        return new ChessMove(-1, -1, -1, -1, board);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove tryDisambiguatedMove(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        char pieceChar = algebraic.charAt(0);
        char disambiguator = algebraic.charAt(1);
        int toX = algebraic.charAt(2) - 'a';
        int toY = 8 - (algebraic.charAt(3) - '0');

        
        List<ChessPiece> pieces = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getChar() == pieceChar)
                .filter(piece -> piece.isValidMove(toX, toY))
                .toList();

        if (pieces.size() == 1) {
            ChessPiece piece = pieces.get(0);
            return new ChessMove(piece.getX(), piece.getY(), toX, toY, board);
        }

        
        if (disambiguator >= 'a' && disambiguator <= 'h') {
            int fileX = disambiguator - 'a';
            for (ChessPiece piece : pieces) {
                if (piece.getX() == fileX) {
                    return new ChessMove(piece.getX(), piece.getY(), toX, toY, board);
                }
            }
        }

        if (disambiguator >= '1' && disambiguator <= '8') {
            int rankY = 8 - (disambiguator - '0');
            for (ChessPiece piece : pieces) {
                if (piece.getY() == rankY) {
                    return new ChessMove(piece.getX(), piece.getY(), toX, toY, board);
                }
            }
        }

        return new ChessMove(-1, -1, -1, -1, board);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove tryDisambiguatedCapture(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        char pieceChar = algebraic.charAt(0);
        char disambiguator = algebraic.charAt(1);
        int xIndex = algebraic.indexOf('x');
        int toX = algebraic.charAt(xIndex + 1) - 'a';
        int toY = 8 - (algebraic.charAt(xIndex + 2) - '0');

        
        List<ChessPiece> pieces = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getChar() == pieceChar)
                .filter(piece -> piece.isValidMove(toX, toY))
                .toList();

        if (pieces.size() == 1) {
            ChessPiece piece = pieces.get(0);
            return new ChessMove(piece.getX(), piece.getY(), toX, toY, board);
        }

        
        if (disambiguator >= 'a' && disambiguator <= 'h') {
            int fileX = disambiguator - 'a';
            for (ChessPiece piece : pieces) {
                if (piece.getX() == fileX) {
                    return new ChessMove(piece.getX(), piece.getY(), toX, toY, board);
                }
            }
        }

        if (disambiguator >= '1' && disambiguator <= '8') {
            int rankY = 8 - (disambiguator - '0');
            for (ChessPiece piece : pieces) {
                if (piece.getY() == rankY) {
                    return new ChessMove(piece.getX(), piece.getY(), toX, toY, board);
                }
            }
        }

        return new ChessMove(-1, -1, -1, -1, board);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove tryFullNotationCapture(@NotNull String algebraic, boolean whiteTurn, @NotNull ChessBoard board) {
        char pieceChar = algebraic.charAt(0);
        int fromX = algebraic.charAt(1) - 'a';
        int fromY = 8 - (algebraic.charAt(2) - '0');
        int xIndex = algebraic.indexOf('x');
        int toX = algebraic.charAt(xIndex + 1) - 'a';
        int toY = 8 - (algebraic.charAt(xIndex + 2) - '0');

        
        ChessPiece piece = board.getPiece(fromX, fromY);
        if (piece != null && piece.getChar() == pieceChar && piece.isWhite() == whiteTurn) {
            if (piece.isValidMove(toX, toY)) {
                return new ChessMove(fromX, fromY, toX, toY, board);
            }
        }

        return new ChessMove(-1, -1, -1, -1, board);
    }

    
    private static boolean canPawnMoveTo(@NotNull ChessPiece pawn, int toX, int toY, ChessBoard board) {
        if (pawn.getX() != toX) return false;
        
        int deltaY = toY - pawn.getY();
        if (pawn.isWhite()) {
            
            if (deltaY == -1) {
                return board.getPiece(toX, toY) == null;
            } else if (deltaY == -2 && pawn.getY() == 1) {
                return board.getPiece(toX, toY) == null && board.getPiece(toX, toY + 1) == null;
            }
        } else {
            
            if (deltaY == 1) {
                return board.getPiece(toX, toY) == null;
            } else if (deltaY == 2 && pawn.getY() == 6) {
                return board.getPiece(toX, toY) == null && board.getPiece(toX, toY - 1) == null;
            }
        }
        return false;
    }

    
    private static @Nullable ChessPiece findPawnForMoveAlternative(int toX, int toY, boolean whiteTurn, @NotNull ChessBoard board) {
        List<ChessPiece> pawns = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getName().equalsIgnoreCase("Pawn"))
                .toList();

        for (ChessPiece pawn : pawns) {
            if (canPawnMoveTo(pawn, toX, toY, board)) {
                return pawn;
            }
        }
        
        
        for (ChessPiece pawn : pawns) {
            int deltaY = toY - pawn.getY();
            if (whiteTurn) {
                if (deltaY == -1 && pawn.getX() == toX) {
                    if (board.getPiece(toX, toY) == null) {
                        return pawn;
                    }
                }
            } else {
                if (deltaY == 1 && pawn.getX() == toX) {
                    if (board.getPiece(toX, toY) == null) {
                        return pawn;
                    }
                }
            }
        }
        
        return null;
    }

    
    private static @Nullable ChessPiece findPawnForMoveAggressive(int toX, int toY, boolean whiteTurn, @NotNull ChessBoard board) {
        List<ChessPiece> pawns = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getName().equalsIgnoreCase("Pawn"))
                .toList();

        
        for (ChessPiece pawn : pawns) {
            if (pawn.getX() == toX) {
                int deltaY = toY - pawn.getY();
                
                
                if (whiteTurn) {
                    
                    if (deltaY == -1) {
                        
                        if (board.getPiece(toX, toY) == null) {
                            return pawn;
                        }
                    } else if (deltaY == -2 && pawn.getY() == 1) {
                        
                        if (board.getPiece(toX, toY) == null && board.getPiece(toX, toY + 1) == null) {
                            return pawn;
                        }
                    }
                } else {
                    
                    if (deltaY == 1) {
                        
                        if (board.getPiece(toX, toY) == null) {
                            return pawn;
                        }
                    } else if (deltaY == 2 && pawn.getY() == 6) {
                        
                        if (board.getPiece(toX, toY) == null && board.getPiece(toX, toY - 1) == null) {
                            return pawn;
                        }
                    }
                }
            }
        }
        
        
        for (ChessPiece pawn : pawns) {
            int deltaY = toY - pawn.getY();
            int deltaX = Math.abs(toX - pawn.getX());
            
            
            if (deltaX == 0) {
                if (whiteTurn) {
                    if (deltaY == -1 || (deltaY == -2 && pawn.getY() == 1)) {
                        
                        boolean pathClear = true;
                        if (deltaY == -2) {
                            
                            for (int y = pawn.getY() - 1; y >= toY; y--) {
                                if (board.getPiece(toX, y) != null) {
                                    pathClear = false;
                                    break;
                                }
                            }
                        } else {
                            
                            pathClear = board.getPiece(toX, toY) == null;
                        }
                        
                        if (pathClear) {
                            return pawn;
                        }
                    }
                } else {
                    if (deltaY == 1 || (deltaY == 2 && pawn.getY() == 6)) {
                        
                        boolean pathClear = true;
                        if (deltaY == 2) {
                            
                            for (int y = pawn.getY() + 1; y <= toY; y++) {
                                if (board.getPiece(toX, y) != null) {
                                    pathClear = false;
                                    break;
                                }
                            }
                        } else {
                            
                            pathClear = board.getPiece(toX, toY) == null;
                        }
                        
                        if (pathClear) {
                            return pawn;
                        }
                    }
                }
            }
        }
        
        return null;
    }

    private static boolean canPawnCaptureTo(@NotNull ChessPiece pawn, int toX, int toY, ChessBoard board) {
        int deltaY = toY - pawn.getY();
        if (pawn.isWhite()) {
            if (deltaY == -1 && Math.abs(toX - pawn.getX()) == 1) {
                ChessPiece target = board.getPiece(toX, toY);
                return target != null && target.isWhite() != pawn.isWhite();
            }
        } else {
            if (deltaY == 1 && Math.abs(toX - pawn.getX()) == 1) {
                ChessPiece target = board.getPiece(toX, toY);
                return target != null && target.isWhite() != pawn.isWhite();
            }
        }
        return false;
    }

    
    private static @Nullable ChessPiece findPawnForCaptureAggressive(int fromX, int toX, int toY, boolean whiteTurn, @NotNull ChessBoard board) {
        List<ChessPiece> pawns = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getName().equalsIgnoreCase("Pawn"))
                .toList();

        for (ChessPiece pawn : pawns) {
            
            int deltaY = toY - pawn.getY();
            int deltaX = Math.abs(toX - pawn.getX());
            
            if (whiteTurn) {
                
                if (deltaY == -1 && deltaX == 1) {
                    
                    ChessPiece target = board.getPiece(toX, toY);
                    if (target != null && target.isWhite() != whiteTurn) {
                        return pawn;
                    }
                    
                    if (target == null && Math.abs(fromX - pawn.getX()) == 1) {
                        return pawn;
                    }
                }
            } else {
                
                if (deltaY == 1 && deltaX == 1) {
                    
                    ChessPiece target = board.getPiece(toX, toY);
                    if (target != null && target.isWhite() != whiteTurn) {
                        return pawn;
                    }
                    
                    if (target == null && Math.abs(fromX - pawn.getX()) == 1) {
                        return pawn;
                    }
                }
            }
        }
        
        return null;
    }

    public ChessMove(int fromX, int fromY, int toX, int toY, @NotNull ChessBoard board) {
        this(fromX, fromY, toX, toY, board, false, '\0', false, false);
    }

    public ChessMove(int fromX, int fromY, int toX, int toY, @NotNull ChessBoard board,
                     boolean isPromotion, char promotionPiece, boolean isCastling, boolean isEnPassant) {
        this.movedPiece = fromX != -1 ? board.getPiece(fromX, fromY) : null;
        this.capturedPiece = toX != -1 ? board.getPiece(toX, toY) : null;

        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;

        this.isPromotion = isPromotion;
        this.promotionPiece = promotionPiece;
        this.isCastling = isCastling;
        this.isEnPassant = isEnPassant;

        this.board = board;
    }

    public boolean isValid() {
        return fromX >= 0 &&
                fromY >= 0 &&
                toX >= 0 &&
                toY >= 0;
    }

    public String asCoordinate() {
        return (char)(fromX + 'A') +
                (8 - fromY) + "-" +
                (char)(toX + 'A') +
                (8 - toY);
    }

    public String asFigurineAlgebraic() {
        if (fromX == -1 || fromY == -1 || toX == -1 || toY == -1) return "";

        if (isCastling) return toX == 6 ? "O-O" : "O-O-O";

        StringBuilder result = new StringBuilder();

        if (movedPiece != null && !(movedPiece instanceof PawnChessPiece))
            result.append(getPieceFigurine(movedPiece.getChar(), movedPiece.isWhite()));

        String disambiguation = getDisambiguation();
        result.append(disambiguation);

        if (capturedPiece != null || isEnPassant) {
            if (movedPiece instanceof PawnChessPiece && disambiguation.isEmpty())
                result.append((char)('a' + fromX));

            result.append("x");
        }

        result.append((char)('a' + toX));
        result.append(8 - toY);

        if (isPromotion && movedPiece != null) {
            result.append("=");
            result.append(getPieceFigurine(promotionPiece, movedPiece.isWhite()));
        }

        return result.toString();
    }

    @Contract(pure = true)
    private @NotNull String getPieceFigurine(char pieceChar, boolean isWhite) {
        return switch (pieceChar) {
            case 'K' -> isWhite ? "♔" : "♚";
            case 'Q' -> isWhite ? "♕" : "♛";
            case 'R' -> isWhite ? "♖" : "♜";
            case 'B' -> isWhite ? "♗" : "♝";
            case 'N' -> isWhite ? "♘" : "♞";
            default -> ""; 
        };
    }

    private @NotNull String getDisambiguation() {
        if (movedPiece == null || movedPiece instanceof PawnChessPiece) {
            return "";
        }

        List<ChessPiece> samePieces = board.getPieces(movedPiece.isWhite()).stream()
                .filter(piece -> piece.getChar() == movedPiece.getChar())
                .filter(piece -> piece.getX() != fromX || piece.getY() != fromY)
                .filter(piece -> Arrays.stream(piece.getValidFields())
                        .anyMatch(field -> field[0] == toX && field[1] == toY))
                .toList();

        if (samePieces.isEmpty()) {
            return "";
        }

        boolean fileUnique = samePieces.stream()
                .noneMatch(piece -> piece.getX() == fromX);

        if (fileUnique) {
            return String.valueOf((char)('a' + fromX));
        }

        boolean rankUnique = samePieces.stream()
                .noneMatch(piece -> piece.getY() == fromY);

        if (rankUnique) {
            return String.valueOf(8 - fromY);
        }

        return String.valueOf((char)('a' + fromX)) + (8 - fromY);
    }

    @Contract("_, _, _ -> new")
    private static @NotNull ChessMove castling(boolean kingside, boolean whiteTurn, @NotNull ChessBoard board) {
        int rank = whiteTurn ? 0 : 7;
        int fromX = 4;
        int toX = kingside ? 6 : 2;

        return new ChessMove(fromX, rank, toX, rank, board, false, '\0', true, false);
    }

    private static @Nullable ChessPiece findPieceCanMoveTo(char pieceChar, int toX, int toY, boolean whiteTurn,
                                                           @NotNull ChessBoard board, Character disambiguator) {
        List<ChessPiece> pieces = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getChar() == pieceChar)
                .toList();

        List<ChessPiece> validPieces = pieces.stream()
                .filter(piece -> Arrays.stream(piece.getValidFields())
                        .anyMatch(field -> field[0] == toX && field[1] == toY))
                .toList();

        if (validPieces.size() == 1) {
            return validPieces.getFirst();
        }

        if (disambiguator != null && validPieces.size() > 1) {
            if (disambiguator >= 'a' && disambiguator <= 'h') {
                int fileX = disambiguator - 'a';
                return validPieces.stream()
                        .filter(piece -> piece.getX() == fileX)
                        .findFirst()
                        .orElse(null);
            }

            if (disambiguator >= '1' && disambiguator <= '8') {
                int rankY = 8 - (disambiguator - '0');
                return validPieces.stream()
                        .filter(piece -> piece.getY() == rankY)
                        .findFirst()
                        .orElse(null);
            }
        }

        return validPieces.isEmpty() ? null : validPieces.getFirst();
    }

    private static @Nullable ChessPiece findPawnForMove(int toX, int toY, boolean whiteTurn, @NotNull ChessBoard board) {
        List<ChessPiece> pawns = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getName().equalsIgnoreCase("Pawn"))
                .toList();

        for (ChessPiece pawn : pawns) {
            if (pawn.getX() == toX) {
                
                int deltaY = toY - pawn.getY();
                if (whiteTurn) {
                    
                    if (deltaY == -1 || (deltaY == -2 && pawn.getY() == 1)) {
                        
                        if (deltaY == -1) {
                            if (board.getPiece(toX, toY) == null) {
                                return pawn;
                            }
                        } else if (deltaY == -2 && pawn.getY() == 1) {
                            if (board.getPiece(toX, toY) == null && board.getPiece(toX, toY + 1) == null) {
                                return pawn;
                            }
                        }
                    }
                } else {
                    
                    if (deltaY == 1 || (deltaY == 2 && pawn.getY() == 6)) {
                        
                        if (deltaY == 1) {
                            if (board.getPiece(toX, toY) == null) {
                                return pawn;
                            }
                        } else if (deltaY == 2 && pawn.getY() == 6) {
                            if (board.getPiece(toX, toY) == null && board.getPiece(toX, toY - 1) == null) {
                                return pawn;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable ChessPiece findPawnForCapture(int fromX, int toX, int toY, boolean whiteTurn, @NotNull ChessBoard board) {
        List<ChessPiece> pawns = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getName().equalsIgnoreCase("Pawn"))
                .toList();

        for (ChessPiece pawn : pawns) {
            if (pawn.getX() == fromX) {
                
                int deltaY = toY - pawn.getY();
                if (whiteTurn) {
                    
                    if (deltaY == -1) {
                        
                        ChessPiece targetPiece = board.getPiece(toX, toY);
                        if (targetPiece != null && targetPiece.isWhite() != whiteTurn) {
                            return pawn;
                        }
                    }
                } else {
                    
                    if (deltaY == 1) {
                        
                        ChessPiece targetPiece = board.getPiece(toX, toY);
                        if (targetPiece != null && targetPiece.isWhite() != whiteTurn) {
                            return pawn;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable ChessPiece findPawnForPromotion(int fromX, int toX, int toY, boolean whiteTurn, @NotNull ChessBoard board) {
        List<ChessPiece> pawns = board.getPieces(whiteTurn).stream()
                .filter(piece -> piece.getName().equalsIgnoreCase("Pawn"))
                .toList();

        for (ChessPiece pawn : pawns) {
            if (pawn.getX() == fromX) {
                
                int deltaY = toY - pawn.getY();
                if (whiteTurn) {
                    
                    if (deltaY == -1 || (deltaY == -2 && pawn.getY() == 1)) {
                        
                        if (deltaY == -1) {
                            if (board.getPiece(toX, toY) == null) {
                                return pawn;
                            }
                        } else if (deltaY == -2 && pawn.getY() == 1) {
                            if (board.getPiece(toX, toY) == null && board.getPiece(toX, toY + 1) == null) {
                                return pawn;
                            }
                        }
                    }
                } else {
                    
                    if (deltaY == 1 || (deltaY == 2 && pawn.getY() == 6)) {
                        
                        if (deltaY == 1) {
                            if (board.getPiece(toX, toY) == null) {
                                return pawn;
                            }
                        } else if (pawn.getY() == 6) {
                            if (board.getPiece(toX, toY) == null && board.getPiece(toX, toY - 1) == null) {
                                return pawn;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}