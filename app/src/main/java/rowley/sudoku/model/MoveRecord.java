package rowley.sudoku.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by joe on 6/15/15.
 */
public class MoveRecord implements Parcelable {
    //Current state is tracked (updated) and preserved elsewhere. To track changes all we need
    //to do is save off the previous state of a cell so we can restore that state upon undo.
    //since the cell is dumb and reacts entirely to the state fed to it this is all we need.
    private int cellIndex;
    private MoveType moveType;
    private CellState previousState;

    public MoveRecord() {
        cellIndex = -1;
        previousState = new CellState();
    }

    protected MoveRecord(Parcel in) {
        cellIndex = in.readInt();
        previousState = in.readParcelable(CellState.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(cellIndex);
        dest.writeParcelable(previousState, flags);
    }


    public static final Creator CREATOR = new Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new MoveRecord(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new Object[0];
        }
    };

    public int getCellIndex() {
        return cellIndex;
    }

    public void setCellIndex(int cellIndex) {
        this.cellIndex = cellIndex;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public void setMoveType(MoveType moveType) {
        this.moveType = moveType;
    }

    public CellState getPreviousState() {
        return previousState;
    }

    public void setPreviousState(CellState previousState) {
        this.previousState = previousState;
    }

    public enum MoveType {
        SET, CLEAR, SET_GUESSES, MARK
    }
}
