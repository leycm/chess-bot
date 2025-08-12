package org.leycm.chessbot.trainer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ChessPgnParser {
    void processPgnFile(String filename, GameProcessor processor) throws IOException;

    record GameData(int whiteElo, int blackElo,
                    int whiteRatingDiff, int blackRatingDiff,
                    String result, String link, List<String> moves) {

    }

    record RawGameData(Map<String, String> headers, String movesText) {
        public static final RawGameData POISON_PILL = new RawGameData(null, null);
    }

    interface GameProcessor {
        void processGame(GameData gameData);
        default void onFilteredGame(String reason) {}
    }
}
