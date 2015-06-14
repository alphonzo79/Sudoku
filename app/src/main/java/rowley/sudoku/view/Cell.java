package rowley.sudoku.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import rowley.sudoku.R;
import rowley.sudoku.model.CellState;

/**
 * Created by joe on 4/19/15.
 */
public class Cell extends FrameLayout {

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

    public void setCellState(CellState state) {
        if(state.getOneBasedChosenNumber() != 0) {
            chosenNumDisplay.setText(String.valueOf(state.getOneBasedChosenNumber()));
            chosenNumDisplay.setVisibility(VISIBLE);
            guessesDisplay.setText("");
                guessesDisplay.setVisibility(GONE);
        } else {
            chosenNumDisplay.setText("");
            chosenNumDisplay.setVisibility(GONE);

            //Do we have guesses to show?
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i < state.getMarkedGuesses().length; i++) {
                if(state.getMarkedGuesses()[i]) {
                    if (builder.length() == 5 || builder.length() == 11) {
                        builder.append("\n");
                    } else if (builder.length() > 0) {
                        builder.append(" ");
                    }
                    builder.append(i + 1);
                }
            }

            if(builder.length() > 0) {
                guessesDisplay.setText(builder.toString());
                guessesDisplay.setVisibility(VISIBLE);
                chosenNumDisplay.setVisibility(GONE);
            } else {
                guessesDisplay.setText("");
                guessesDisplay.setVisibility(GONE);
            }

            builder = null;
        }

        getBackground().setLevel(state.isMarked() ? 1 : 0);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int width = getDefaultSize(getSuggestedMinimumWidth(),widthMeasureSpec);
        if(width>this.squareWidth)
        {
            this.squareWidth = width;
        }
        setMeasuredDimension(this.squareWidth, this.squareWidth);

        int newWidthMeasureSpec = MeasureSpec.makeMeasureSpec( this.squareWidth, MeasureSpec.EXACTLY );
        super.onMeasure(newWidthMeasureSpec, newWidthMeasureSpec);
    }
}
