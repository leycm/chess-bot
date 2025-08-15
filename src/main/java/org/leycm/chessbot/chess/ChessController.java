package org.leycm.chessbot.chess;

import lombok.Data;
import lombok.Setter;

import java.io.Serializable;

@Data
public abstract class ChessController implements Serializable {
    private final String name;
    private final String type;

    @Setter private boolean myTurn;
    @Setter private boolean myColorWhite;

    public ChessController(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public abstract void onTick(ChessBoard board);

    public void tick(ChessBoard board) {
        onTick(board);
    }


    public String getDisplayName() {
        return isMyColorWhite() ? "[White] " : "[Black] " + name + " <" + type + ">";
    }

}
