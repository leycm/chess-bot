//package org.leycm.chessbot.chess;
//
//import java.util.Arrays;
//
//public abstract class Player {
//
//    public enum Status {
//        S0_PLAYER_IS_THINKING,
//        S1_PIECE_MOVEMENT_DETECTED,
//        S2_PLAYER_MOVES_OWN_PIECE,
//        S3_PLAYER_ENDED_MOVE,
//        S5_CAPTURE_OPPONENT_FIRST,
//        S7_MOVE_FINISHED_SUCCESSFULLY
//    }
//
//    private boolean[][] lastSensorFeedback;
//
//    private boolean detectChange(ChessBoard board, boolean[][] sensorFeedback) {
//
//        if (lastSensorFeedback == null){
//            lastSensorFeedback = sensorFeedback;
//        }
//
////        int sensorFeedbackInt = Arrays.stream(sensorFeedback);
////        int sensorFeedback_int = Arrays.stream(board.getBooleanBoard()).mapToInt();
//
////        sensorFeedback - lastSensorFeedback
////        change = board.getBooleanBoard ^ sensorFeedback ;// find change relative to stored board
//
////        board.getPiece(int x, int y);
//
////        x = find_x(change);
////        y = find_y(change);
//
//        // do your own bounds checking
//
//
////        boolean onePieceIsLifted =
////        boolean twoPiecesAreLifted =
////        int nPiecesLifted =
////    }
//////
////    public void tick(boolean[][] liftedPiece, ChessBoard board, boolean inWhiteTeam) {
////
////        Status nextStatus = Status.values()[which];
////
////        board.getPiece(0, 0).isValidMove(0, 0);
////
////                detectChange(board,sensorFeedback)
////
////
////        switch (nextStatus) {
////            case S0_PLAYER_IS_THINKING:
////                if nPiecesLifted == 1:
////                nextStatus = Status.S1_PIECE_MOVEMENT_DETECTED
////            else
////                // green light: off
////                // red light: on
////                nextStatus = nextStatus // remain in this state
////
////            case S1_PIECE_MOVEMENT_DETECTED:
////                // green light: on
////                // red light: off
////                if (nPiecesLifted == 1) {
////                    if (liftedPiece.isOwn) {
////                        posSelf = liftedPiece.position;
////                        nextStatus = Status.S2_PLAYER_MOVES_OWN_PIECE;
////                    } else {
////                        posOther = liftedPiece.position;
////                        nextStatus = Status.S7_MOVE_FINISHED_SUCCESSFULLY;
////                    }
////                }
////                    if liftedPiece.isOwn:
////                        posSelf = liftedPiece.position;
////                        nextStatus = Status.S2_PLAYER_MOVES_OWN_PIECE;
////                    else:
////                        posOther = liftedPiece.position;
////                        nextStatus = Status.S7_MOVE_FINISHED_SUCCESSFULLY;
////                else:
////                    // green light: off
////                    // red light: on
////                    break;
////            case S2_PLAYER_MOVES_OWN_PIECE:
////                // green light: on
////                // red light: off
////
////                break;
////            case S3_PLAYER_ENDED_MOVE:
////                // green light: on
////                // red light: off
////                break;
////            case S7_MOVE_FINISHED_SUCCESSFULLY:
////                // green light: on
////                // red light: off
////
////                break;
////
////            case S5_CAPTURE_OPPONENT_FIRST:
////                // green light: on
////                // red light: off...
////                break;
////
////
////            case S3_PLAYER_ENDED_MOVE:
////                ...
////                break;
////            case S3_PLAYER_ENDED_MOVE:
//                ...
//                break;
//
//        }
//    }

//}

