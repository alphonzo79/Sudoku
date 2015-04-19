package rowley.sudoku.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
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
    }

    public Cell(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Cell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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

    public void setChosenNumber(int chosenNumber, boolean display) {
        this.chosenNumber = chosenNumber;
        chosenNumDisplay.setText(String.valueOf(chosenNumber));
        if(display) {
            chosenNumDisplay.setVisibility(VISIBLE);
        }

        guessesDisplay.setVisibility(GONE);
    }

    public int getChosenNumber() {
        return chosenNumber;
    }

    public int removeChosenNumber() {
        int result = chosenNumber;
        chosenNumber = 0;
        chosenNumDisplay.setText("");
        chosenNumDisplay.setVisibility(GONE);
        possibilities[result] = true;

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

    public void addPossibility(int possibility) {
        possibilities[possibility] = true;
    }

    public void removePossibility(int possibility) {
        possibilities[possibility] = false;
    }

    public void finalizeCell() {
        if(chosenNumber > 0) {
            chosenNumDisplay.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
    {
        final int width = getDefaultSize(getSuggestedMinimumWidth(),widthMeasureSpec);
        if(width>this.squareWidth)
        {
            this.squareWidth = width;
        }
        setMeasuredDimension(this.squareWidth, this.squareWidth);
    }
}
