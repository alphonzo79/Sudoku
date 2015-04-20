package rowley.sudoku.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import java.util.Random;

import rowley.sudoku.R;

/**
 * Created by joe on 4/19/15.
 */
public class Board extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
    private Cell[] cells = new Cell[81];
    private int[] winningBoard = new int[81];
    private int moveIndex = 0;
    private int[] moveRecord = new int[81];

    private Random rand;

    public Board(Context context) {
        super(context);
    }

    public Board(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Board(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initView(Context context) {
        inflate(context, R.layout.view_board, this);

        for(int i = 0; i < cells.length; i++) {
            Cell cell = (Cell)findViewWithTag(i);
            cell.setOnClickListener(this);
            cell.setOnLongClickListener(this);
            cells[i] = cell;
        }

        rand = new Random(System.currentTimeMillis());
    }

    public void initializeBoard(double targetComplexity) {
        moveIndex = 0;
        for(int i = 0; i < moveRecord.length; i++) {
            moveRecord[i] = -1;
        }

        for(Cell cell : cells) {
            cell.resetCell();
        }

        findValueForCellAndAllOthers(0);
        for(int i = 0; i < cells.length; i++) {
            winningBoard[i] = cells[i].getOneBasedChosenNumber();
        }

        boolean shouldContinue = true;
        while(shouldContinue) {
            int cellIndex = rand.nextInt(cells.length);
            int retrieved = cells[cellIndex].removeOneBasedChosenNumber();
            if(retrieved > 0) {
                addPossibilityToCounterparts(retrieved - 1, cellIndex);
                shouldContinue = estimateComplexity() < targetComplexity;
            }
        }

        for(Cell cell : cells) {
            cell.finalizeCell();
        }
    }

    private boolean findValueForCellAndAllOthers(int targetCell) {
        if(targetCell > cells.length - 1) {
            return true;
        }

        boolean result = false;

        int possibilitiesSize = cells[targetCell].getPossibilities().length;
        int index = rand.nextInt(possibilitiesSize);
        for(int i = 0; i < possibilitiesSize && !result; i++, index++) {
            if(index > possibilitiesSize) {
                index = 0;
            }

            if(cells[targetCell].getPossibilities()[index]) {
                cells[targetCell].setChosenNumber(index + 1, false);
                if(removePossibilityFromCounterparts(index, targetCell)) {
                    result = findValueForCellAndAllOthers(targetCell + 1);
                }

                if(!result) {
                    cells[targetCell].removeOneBasedChosenNumber();
                    addPossibilityToCounterparts(index, targetCell);
                }
            }
        }

        return result;
    }

    private boolean removePossibilityFromCounterparts(int zeroBasedPossibility, int cellNum) {
        //todo
        return true;
    }

    private void addPossibilityToCounterparts(int zeroBasedPossibility, int cellNum) {
        //todo
    }

    private double estimateComplexity() {
        //todo
        return 0;
    }

    @Override
    public void onClick(View v) {
        //todo
    }

    @Override
    public boolean onLongClick(View v) {
        //todo
        return false;
    }
}
