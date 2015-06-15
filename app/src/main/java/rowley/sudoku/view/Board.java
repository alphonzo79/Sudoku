package rowley.sudoku.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Random;

import rowley.sudoku.R;
import rowley.sudoku.model.CellState;
import rx.Observable;
import rx.Subscriber;
import rx.android.app.AppObservable;

/**
 * Created by joe on 4/19/15.
 */
public class Board extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
    private Cell[] cells = new Cell[81];
    private CellState[] activeCellStates = new CellState[81];
    private int[] winningBoard = new int[81];
    private int moveIndex = 0;
    private int[] moveRecord = new int[81];

    private Random rand;

    private boolean warnOnBadEntry = false;

    public Board(Context context) {
        super(context);
        initView(context);
    }

    public Board(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public Board(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.view_board, this);

        if(!isInEditMode()) {
            for (int i = 0; i < cells.length; i++) {
                Cell cell = (Cell) findViewWithTag(String.valueOf(i));
                cell.setOnClickListener(this);
                cell.setOnLongClickListener(this);
                cell.setEnabled(false);
                cells[i] = cell;

                activeCellStates[i] = new CellState();
            }
        }

        rand = new Random(System.currentTimeMillis());
    }

    /**
     * Synchronously reset all state tracking and create a valid board, then back out to the given level of complexity.<br />
     * <b>This method runs synchronously. It is advised to call this method from a background thread. It does not do any work with UI elements. To finalize the board and display the results of this method, use {@link #finalizeBoard()}</b>
     * @param targetComplexity The target level of complexity to which the board should be cleared, expressed in number of possible permutations (valid and non-valid)
     */
    public void initializeBoard(double targetComplexity) {
        moveIndex = 0;
        for (int i = 0; i < moveRecord.length; i++) {
            moveRecord[i] = -1;
        }

        for (CellState state : activeCellStates) {
            state.resetState();
        }

        if (!findValueForCellAndAllOthers(0)) {
            Toast.makeText(getContext(), R.string.failed_to_build_board, Toast.LENGTH_SHORT).show();
        }
        for (int i = 0; i < activeCellStates.length; i++) {
            winningBoard[i] = activeCellStates[i].getOneBasedChosenNumber();
        }

        boolean shouldContinue = true;
        while (shouldContinue) {
            int cellIndex = rand.nextInt(cells.length);
            clearCellState(cellIndex);
            shouldContinue = estimateComplexity() < targetComplexity;
        }
    }

    /**
     * Display the results of {@link #initializeBoard(double)}. This method is meant to run on the UI thread
     */
    public void finalizeBoard() {
        for(int i = 0; i < cells.length; i++) {
            cells[i].setCellState(activeCellStates[i]);
            cells[i].setEnabled(activeCellStates[i].getOneBasedChosenNumber() == 0);
        }
    }

    private boolean findValueForCellAndAllOthers(int targetCell) {
        if(targetCell > activeCellStates.length - 1) {
            return true;
        }

        boolean result = false;

        int possibilitiesSize = activeCellStates[targetCell].getPossibilities().length;
        int index = rand.nextInt(possibilitiesSize);
        for(int i = 0; i < possibilitiesSize && !result; i++, index++) {
            if(index >= possibilitiesSize) {
                index = 0;
            }

            if(activeCellStates[targetCell].getPossibilities()[index]) {
                activeCellStates[targetCell].setOneBasedChosenNumber(index + 1);
                Log.d("JAR", "Trying Value " + (index + 1) + " in cell " + targetCell);
                if(removePossibilityFromCounterparts(index, targetCell)) {
                    result = findValueForCellAndAllOthers(targetCell + 1);
                }

                if(!result) {
                    activeCellStates[targetCell].removeOneBasedChosenNumber();
                    addPossibilityToCounterparts(index, targetCell);
                }
            }
        }

        return result;
    }

    private boolean removePossibilityFromCounterparts(int zeroBasedPossibility, int cellNum) {
        boolean success = true;

        int rowNum = cellNum / 9;
        int workingIndex = cellNum - 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum + 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex++;
        }

        int columnNum = cellNum % 9;
        workingIndex = cellNum - 9;
        while(workingIndex >= 0) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex += -9;
        }
        workingIndex = cellNum + 9;
        while(workingIndex >= 0 && workingIndex < activeCellStates.length) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex += 9;
        }

        //for the block follow the formula we have come up with, save time by skipping the current row and current column.
        //Brute-force try two rows above and two rows below
        int blockNum = figureBlockNumForCell(cellNum);
        workingIndex = cellNum - 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum - 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex++;
        }
        workingIndex = cellNum - 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum - 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex++;
        }
        workingIndex = cellNum + 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum + 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex++;
        }
        workingIndex = cellNum + 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum + 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            activeCellStates[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(activeCellStates[workingIndex]) || activeCellStates[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex++;
        }

        return success;
    }

    private void addPossibilityToCounterparts(int zeroBasedPossibility, int cellNum) {
        int rowNum = cellNum / 9;
        int workingIndex = cellNum - 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum + 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }

        int columnNum = cellNum % 9;
        workingIndex = cellNum - 9;
        while(workingIndex >= 0) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex += -9;
        }
        workingIndex = cellNum + 9;
        while(workingIndex >= 0 && workingIndex < activeCellStates.length) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex += 9;
        }

        //for the block follow the formula we have come up with, save time by skipping the current row and current column.
        //Brute-force try two rows above and two rows below
        int blockNum = figureBlockNumForCell(cellNum);
        workingIndex = cellNum - 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum - 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }
        workingIndex = cellNum - 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum - 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }
        workingIndex = cellNum + 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum + 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }
        workingIndex = cellNum + 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum + 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                activeCellStates[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }
    }

    private boolean isPossibilitySetAsChosenInCounterpart(int zeroBasedPossibility, int cellNum) {
        boolean result = false;

        int rowNum = cellNum / 9;
        int workingIndex = cellNum - 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum + 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }

        int columnNum = cellNum % 9;
        workingIndex = cellNum - 9;
        while(workingIndex >= 0 && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex += -9;
        }
        workingIndex = cellNum + 9;
        while(workingIndex >= 0 && workingIndex < activeCellStates.length && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex += 9;
        }

        //for the block follow the formula we have come up with, save time by skipping the current row and current column.
        //Brute-force try two rows above and two rows below
        int blockNum = figureBlockNumForCell(cellNum);
        workingIndex = cellNum - 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum - 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }
        workingIndex = cellNum - 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum - 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }
        workingIndex = cellNum + 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum + 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }
        workingIndex = cellNum + 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum + 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = activeCellStates[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }

        return result;
    }

    private boolean doesCellHaveMoreThanZeroPossibilities(CellState cellState) {
        for(boolean possibility : cellState.getPossibilities()) {
            if(possibility) {
                return true;
            }
        }

        return false;
    }

    private int figureBlockNumForCell(int cellIndex) {
        //Dividing the index by 27 and THEN multiplying by 3 is critical. This cannot be simplified
        //to simply dividing by 9 because we need to force the first 27 cells to 0 when multiplied
        //by three; the next 27 cells to 3; the last 27 to 6. Add that num to the column / 3 and you'll
        //get an accurate figure for blocks 0 - 8 in rows of three.
        return (cellIndex % 9) / 3 + ((cellIndex / 27) * 3);
    }

    private double estimateComplexity() {
        double result = 1;
        int possibilitiesCount = 0;

        for(CellState state : activeCellStates) {
            if(state.getOneBasedChosenNumber() == 0) {
                possibilitiesCount = 0;
                for(boolean possible : state.getPossibilities()) {
                    if(possible) {
                        possibilitiesCount++;
                    }
                }

                if(possibilitiesCount > 0) {
                    result *= possibilitiesCount;
                }
            }
        }

        return result;
    }

    private boolean isCompleted() {
        boolean result = true;

        for(int i = 0; i < activeCellStates.length; i++) {
            int chosenNum = activeCellStates[i].getOneBasedChosenNumber();
            if(chosenNum == 0) {
                result = false;
                break;
            } else if(isPossibilitySetAsChosenInCounterpart(chosenNum - 1, i)) {
                result = false;
                break;
            }
        }

        return result;
    }

    private void clearCellState(int cellIndex) {
        int retrieved = activeCellStates[cellIndex].removeOneBasedChosenNumber();
        if(retrieved > 0) {
            addPossibilityToCounterparts(retrieved - 1, cellIndex);
            Log.d("JAR", "Found complexity of " + estimateComplexity());
        }
    }

    public void clearCell(int cellIndex) {
        clearCellState(cellIndex);
        cells[cellIndex].setCellState(activeCellStates[cellIndex]);
    }

    public void setNumToCell(int oneBasedChosenNumber, int cellIndex) {
        int alreadySet = activeCellStates[cellIndex].getOneBasedChosenNumber();
        if(alreadySet != 0) {
            clearCell(cellIndex);
        }

        activeCellStates[cellIndex].setOneBasedChosenNumber(oneBasedChosenNumber);
        cells[cellIndex].setCellState(activeCellStates[cellIndex]);
        boolean validMove = removePossibilityFromCounterparts(oneBasedChosenNumber - 1, cellIndex);
        if(warnOnBadEntry) {
            if(validMove) {
                validMove = !isPossibilitySetAsChosenInCounterpart(oneBasedChosenNumber - 1, cellIndex);
            }
            if(!validMove) {
                activeCellStates[cellIndex].removeOneBasedChosenNumber();
                cells[cellIndex].setCellState(activeCellStates[cellIndex]);
                addPossibilityToCounterparts(oneBasedChosenNumber - 1, cellIndex);

                Toast.makeText(getContext(), R.string.bad_move_warning, Toast.LENGTH_LONG).show();
            }
        }

        if(isCompleted()) {
            for(Cell cell: cells) {
                cell.setEnabled(false);
            }
            ((BoardListener)getContext()).onComplete();
        }
    }

    public void setMarksToCell(boolean[] markedPossibilities, int cellIndex) {
        activeCellStates[cellIndex].setMarkedGuesses(markedPossibilities);
        cells[cellIndex].setCellState(activeCellStates[cellIndex]);
    }

    public void markCellAsPivot(boolean isMarked, int cellIndex) {
        activeCellStates[cellIndex].setIsMarked(isMarked);
        cells[cellIndex].setCellState(activeCellStates[cellIndex]);
    }

    public void setWarnOnBadEntry(boolean warnOnBadEntry) {
        this.warnOnBadEntry = warnOnBadEntry;
    }

    @Override
    public void onClick(View v) {
        if(getContext() instanceof BoardListener) {
            int index = Integer.parseInt((String)v.getTag());
            ((BoardListener)getContext()).onShowSetCellFrag(activeCellStates[index].getPossibilities(), index);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if(getContext() instanceof BoardListener) {
            int index = Integer.parseInt((String)v.getTag());
            ((BoardListener)getContext()).onShowMarkCellFrag(activeCellStates[index].getMarkedGuesses(), activeCellStates[index].isMarked(), index);
        }
        return true;
    }

    public interface BoardListener {
        public void onShowSetCellFrag(boolean[] possibilities, int cellIndex);
        public void onShowMarkCellFrag(boolean[] markedPossibilities, boolean isMarked, int cellIndex);
        public void onComplete();
    }
}
