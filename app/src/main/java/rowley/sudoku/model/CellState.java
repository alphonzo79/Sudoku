package rowley.sudoku.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by joe on 6/13/15.
 */
public class CellState implements Parcelable {
    private boolean[] possibilities = new boolean[9];
    private boolean[] markedGuesses = new boolean[9];
    private int oneBasedChosenNumber;
    private boolean isMarked = false;

    public CellState() {

    }

    protected CellState(Parcel in) {
        in.readBooleanArray(possibilities);
        in.readBooleanArray(markedGuesses);
        oneBasedChosenNumber = in.readInt();
        isMarked = in.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBooleanArray(possibilities);
        dest.writeBooleanArray(markedGuesses);
        dest.writeInt(oneBasedChosenNumber);
        dest.writeInt(isMarked ? 1 : 0);
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new CellState(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new Object[0];
        }
    };

    public boolean[] getPossibilities() {
        return possibilities;
    }

    public void setPossibilities(boolean[] possibilities) {
        this.possibilities = possibilities;
    }

    public boolean[] getMarkedGuesses() {
        return markedGuesses;
    }

    public void setMarkedGuesses(boolean[] markedGuesses) {
        this.markedGuesses = markedGuesses;
    }

    public void addPossibility(int zeroBasedPossibility) {
        possibilities[zeroBasedPossibility] = true;
    }

    public void removePossibility(int zeroBasedPossibility) {
        possibilities[zeroBasedPossibility] = false;
    }

    public int getOneBasedChosenNumber() {
        return oneBasedChosenNumber;
    }

    public void setOneBasedChosenNumber(int oneBasedChosenNumber) {
        if(oneBasedChosenNumber > 0 && oneBasedChosenNumber <= possibilities.length) {
            this.oneBasedChosenNumber = oneBasedChosenNumber;
            possibilities[oneBasedChosenNumber - 1] = false;
        }
    }

    public int removeOneBasedChosenNumber() {
        int result = oneBasedChosenNumber;
        oneBasedChosenNumber = 0;
        if(result > 0) {
            possibilities[result - 1] = true;
        }

        return result;
    }

    public boolean isMarked() {
        return isMarked;
    }

    public void setIsMarked(boolean isMarked) {
        this.isMarked = isMarked;
    }

    public void resetState() {
        for(int i = 0; i < possibilities.length; i++) {
            possibilities[i] = true;
        }

        for(int i = 0; i < markedGuesses.length; i++) {
            markedGuesses[i] = false;
        }

        oneBasedChosenNumber = 0;
        isMarked = false;
    }
}
