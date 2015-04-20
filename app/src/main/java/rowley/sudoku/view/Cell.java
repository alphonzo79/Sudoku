package rowley.sudoku.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import rowley.sudoku.R;

/**
 * Created by joe on 4/19/15.
 */
public class Cell extends FrameLayout {
    private boolean[] possibilities = new boolean[9];
    private boolean[] markedGuesses = new boolean[9];
    private int chosenNumber;
    private boolean isMarked = false;

    private TextView chosenNumDisplay;
    private TextView guessesDisplay;

    private int squareWidth = 0;

    public Cell(Context context) {
        super(context);
        initView(context);
    }

    public Cell(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public Cell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.view_cell, this);

        chosenNumDisplay = (TextView)findViewById(R.id.chosen_number_view);
        guessesDisplay = (TextView)findViewById(R.id.noted_guesses_view);
    }

    public void resetCell() {
        for(int i = 0; i < possibilities.length; i++) {
            possibilities[i] = true;
        }

        for(int i = 0; i < markedGuesses.length; i++) {
            markedGuesses[i] = false;
        }

        chosenNumber = 0;
        chosenNumDisplay.setText("");
        chosenNumDisplay.setVisibility(GONE);

        guessesDisplay.setText("");
        guessesDisplay.setVisibility(GONE);

        isMarked = false;
    }

    public void setChosenNumber(int oneBasedChosenNumber, boolean display) {
        if(oneBasedChosenNumber > 0 && oneBasedChosenNumber <= possibilities.length) {
            this.chosenNumber = oneBasedChosenNumber;
            chosenNumDisplay.setText(String.valueOf(chosenNumber));
            if (display) {
                chosenNumDisplay.setVisibility(VISIBLE);
                guessesDisplay.setVisibility(GONE);
            }
            possibilities[oneBasedChosenNumber - 1] = false;
        }
    }

    public int getOneBasedChosenNumber() {
        return chosenNumber;
    }

    public int removeOneBasedChosenNumber() {
        int result = chosenNumber;
        chosenNumber = 0;
        chosenNumDisplay.setText("");
        chosenNumDisplay.setVisibility(GONE);
        if(result > 0) {
            possibilities[result - 1] = true;
        }

        return result;
    }

    public void setMarkedGuesses(boolean[] markedGuesses) {
        this.markedGuesses = markedGuesses;

        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < markedGuesses.length; i++) {
            if(markedGuesses[i]) {
                if (builder.length() % 5 == 0) {
                    builder.append("\n");
                } else if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(i);
            }
        }
        guessesDisplay.setText(builder.toString());
        guessesDisplay.setVisibility(VISIBLE);

        chosenNumDisplay.setVisibility(GONE);

        builder = null;
    }

    public boolean[] getMarkedGuesses() {
        return markedGuesses;
    }

    public boolean[] getPossibilities() {
        return possibilities;
    }

    public void toggleMarked() {
        isMarked = !isMarked;
        getBackground().setLevel(isMarked ? 1 : 0);
    }

    public boolean getIsMarked() {
        return isMarked;
    }

    public void addPossibility(int zeroBasedPossibility) {
        possibilities[zeroBasedPossibility] = true;
    }

    public void removePossibility(int zeroBasedPossibility) {
        possibilities[zeroBasedPossibility] = false;
    }

    public void finalizeCell() {
        if(chosenNumber > 0) {
            chosenNumDisplay.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = getDefaultSize(getSuggestedMinimumWidth(),widthMeasureSpec);
        if(width>this.squareWidth)
        {
            this.squareWidth = width;
        }
        setMeasuredDimension(this.squareWidth, this.squareWidth);
    }
}
