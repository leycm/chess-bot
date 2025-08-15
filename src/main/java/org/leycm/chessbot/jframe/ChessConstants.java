package org.leycm.chessbot.jframe;

import java.awt.Color;
import java.awt.Font;

/**
 * Constants used throughout the Chess GUI application.
 */
public final class ChessConstants {

    public static final int SQUARE_SIZE = 70;
    public static final int BOARD_SIZE = SQUARE_SIZE * 8;

    public static final Color LIGHT_SQUARE = new Color(240, 217, 181);
    public static final Color DARK_SQUARE = new Color(181, 136, 99);

    public static final Color HIGHLIGHT_COLOR = new Color(221, 221, 0, 128);
    public static final Color LAST_MOVE_COLOR = new Color(135, 172, 0, 128);
    public static final Color CHECK_COLOR = new Color(255, 0, 0, 128);
    public static final Color VALID_MOVE_COLOR = new Color(24, 179, 24, 80);

    public static final Color SELECTED_MOVE_COLOR = new Color(100, 100, 150);
    public static final Color HOVER_MOVE_COLOR = new Color(80, 80, 120);
    public static final Color PANEL_BACKGROUND = new Color(40, 40, 40);
    public static final Color HISTORY_BACKGROUND = new Color(50, 50, 50);
    public static final Color BUTTON_BACKGROUND = new Color(60, 60, 60);

    public static final Font COORDINATE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font PIECE_FONT = new Font("Serif", Font.PLAIN, 48);
    public static final Font HISTORY_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    public static final Font HISTORY_NUMBER_FONT = new Font(Font.MONOSPACED, Font.BOLD, 12);
    public static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    public static final int MOVE_DOT_SIZE = 22;
    public static final int MOVE_DOT_RADIUS = MOVE_DOT_SIZE / 2;

    private ChessConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
