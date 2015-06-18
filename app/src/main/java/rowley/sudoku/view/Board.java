package rowley.sudoku.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Random;

import rowley.sudoku.R;
import rowley.sudoku.model.CellState;
import rowley.sudoku.model.MoveRecord;
import rx.Observable;
import rx.Subscriber;
import rx.android.app.AppObservable;

/**
 * Created by joe on 4/19/15.
 */
public class Board extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
    private final int BOARD_SIZE = 81;

    private Cell[] cells;
    private CellState[] activeCellStates;
    private int[] winningBoard;
    private int moveIndex;
    private MoveRecord[] moveRecord;

    private CellState cellStateTempHolder;
    private boolean[] markedGuessesTransferArray;

    private Random rand;

    private boolean warnOnBadEntry = false;

    private final String BUNDLE_KEY_STATES = "BUNDLE_KEY_STATES";
    private final String BUNDLE_KEY_WINNING_BOARD = "BUNDLE_KEY_WINNING_BOARD";
    private final String BUNDLE_KEY_MOVE_INDEX = "BUNDLE_KEY_MOVE_INDEX";
    private final String BUNDLE_KEY_MOVE_RECORD = "BUNDLE_KEY_MOVE_RECORD";
    private final String BUNDLE_KEY_CELLS_ENABLED = "BUNDLE_KEY_CELLS_ENABLED";

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
            cells = new Cell[BOARD_SIZE];
            activeCellStates = new CellState[BOARD_SIZE];
            winningBoard = new int[BOARD_SIZE];
            moveIndex = 0;
            //start big to avoid resizing if we can.
            // The data is simple enough that this shouldn't cost us too much in memory footprint.
            moveRecord = new MoveRecord[BOARD_SIZE * 3];
            for(int i = 0; i < moveRecord.length; i++) {
                moveRecord[i] = new MoveRecord();
            }
            cellStateTempHolder = new CellState();

            for (int i = 0; i < cells.length; i++) {
                Cell cell = (Cell) findViewWithTag(String.valueOf(i));
                cell.setOnClickListener(this);
                cell.setOnLongClickListener(this);
                cell.setEnabled(false);
                cells[i] = cell;

                activeCellStates[i] = new CellState();
            }

            rand = new Random(System.currentTimeMillis());
        }
    }

    /**
     * Synchronously reset all state tracking and create a valid board, then back out to the given level of complexity.<br />
     * <b>This method runs synchronously. It is advised to call this method from a background thread. It does not do any work with UI elements. To finalize the board and display the results of this method, use {@link #finalizeBoard()}</b>
     * @param targetComplexity The target level of complexity to which the board should be cleared, expressed in number of possible permutations (valid and non-valid)
     */
    public void initializeBoard(double targetComplexity) {
        moveIndex = 0;
        for (int i = 0; i < moveRecord.length; i++) {
            moveRecord[i].setCellIndex(-1);
            moveRecord[i].getPreviousState().resetState();
        }

        for (CellState state : activeCellStates) {
            state.resetState();
        }

        if (!findValueForCellAndAllOthers(0, true)) {
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

    private boolean findValueForCellAndAllOthers(int targetCell, boolean retainValue) {
        if(targetCell > activeCellStates.length - 1) {
            return true;
        }
        if(activeCellStates[targetCell].getOneBasedChosenNumber() > 0) {
            //We're running through looking for a hint and this will allow us to skip over cells
            //that have already been set. Just move along to the next cell
            return findValueForCellAndAllOthers(targetCell + 1, retainValue);
        }

        boolean result = false;

        int possibilitiesSize = activeCellStates[targetCell].getPossibilities().length;
        int index = rand.nextInt(possibilitiesSize);
        for (int i = 0; i < possibilitiesSize && !result; i++, index++) {
            if (index >= possibilitiesSize) {
                index = 0;
            }

            if (activeCellStates[targetCell].getPossibilities()[index]) {
                activeCellStates[targetCell].setOneBasedChosenNumber(index + 1);
                Log.d("JAR", "Trying Value " + (index + 1) + " in cell " + targetCell);
                if (removePossibilityFromCounterparts(index, targetCell)) {
                    result = findValueForCellAndAllOthers(targetCell + 1, retainValue);
                }

                if (!result || !retainValue) {
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
        clearCell(cellIndex, true);
    }

    private void clearCell(int cellIndex, boolean addToMoveRecord) {
        if(addToMoveRecord) {
            activeCellStates[cellIndex].duplicateState(cellStateTempHolder);
            addToMoveRecord(cellIndex, MoveRecord.MoveType.CLEAR, cellStateTempHolder);
        }
        clearCellState(cellIndex);
        cells[cellIndex].setCellState(activeCellStates[cellIndex]);
    }

    public void setNumToCell(int oneBasedChosenNumber, int cellIndex) {
        activeCellStates[cellIndex].duplicateState(cellStateTempHolder);
        addToMoveRecord(cellIndex, MoveRecord.MoveType.SET, cellStateTempHolder);

        int alreadySet = activeCellStates[cellIndex].getOneBasedChosenNumber();
        if(alreadySet != 0) {
            clearCell(cellIndex, false);
        }

        activeCellStates[cellIndex].setOneBasedChosenNumber(oneBasedChosenNumber);
        cells[cellIndex].setCellState(activeCellStates[cellIndex]);
        boolean validMove = removePossibilityFromCounterparts(oneBasedChosenNumber - 1, cellIndex);
        if(warnOnBadEntry) {
            if(validMove) {
                validMove = !isPossibilitySetAsChosenInCounterpart(oneBasedChosenNumber - 1, cellIndex);
            }
            if(!validMove) {
                undo();

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
        activeCellStates[cellIndex].duplicateState(cellStateTempHolder);
        addToMoveRecord(cellIndex, MoveRecord.MoveType.SET_GUESSES, cellStateTempHolder);
        activeCellStates[cellIndex].setMarkedGuesses(markedPossibilities);
        cells[cellIndex].setCellState(activeCellStates[cellIndex]);
    }

    public void markCellAsPivot(boolean isMarked, int cellIndex, boolean addToMoveRecord) {
        if(addToMoveRecord) {
            activeCellStates[cellIndex].duplicateState(cellStateTempHolder);
            addToMoveRecord(cellIndex, MoveRecord.MoveType.MARK, cellStateTempHolder);
        }
        activeCellStates[cellIndex].setIsMarked(isMarked);
        cells[cellIndex].setCellState(activeCellStates[cellIndex]);
    }


    public boolean isCellMarkedAsPivot(int cellIndex) {
        return activeCellStates[cellIndex].isMarked();
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
            if(markedGuessesTransferArray == null) {
                markedGuessesTransferArray = new boolean[activeCellStates[index].getMarkedGuesses().length];
            }
            for(int i = 0; i < markedGuessesTransferArray.length; i++) {
                markedGuessesTransferArray[i] = activeCellStates[index].getMarkedGuesses()[i];
            }
            ((BoardListener)getContext()).onShowMarkCellFrag(markedGuessesTransferArray, activeCellStates[index].isMarked(), index);
        }
        return true;
    }

    public Pair<Integer, Integer> getHint() {
        Pair<Integer, Integer> result = null;

        int attemptCount = 0;
        boolean success = false;
        int[] cellAndValue = new int[2];
        while(!success && attemptCount < 100) {
            cellAndValue = findPossibleHintForRandomCell();
            if(cellAndValue[0] >= 0 && cellAndValue[1] > 0) {
                activeCellStates[cellAndValue[0]].setOneBasedChosenNumber(cellAndValue[1]);
                if (removePossibilityFromCounterparts(cellAndValue[1] - 1, cellAndValue[0])) {
                    success = findValueForCellAndAllOthers(0, false);
                }

                activeCellStates[cellAndValue[0]].removeOneBasedChosenNumber();
                addPossibilityToCounterparts(cellAndValue[1] - 1, cellAndValue[0]);
            }

            attemptCount++;
        }


        if(success) {
            result = new Pair<Integer, Integer>(cellAndValue[0], cellAndValue[1]);
        }

        return result;
    }

    private int[] findPossibleHintForRandomCell() {
        int[] result = new int[]{-1, 0};

        int cellIndex = rand.nextInt(cells.length);
        if(activeCellStates[cellIndex].getOneBasedChosenNumber() == 0) {
            int possibilitiesSize = activeCellStates[cellIndex].getPossibilities().length;
            int index = rand.nextInt(possibilitiesSize);
            boolean validPossibilityFound = false;
            for (int i = 0; i < possibilitiesSize && !validPossibilityFound; i++, index++) {
                if (index >= possibilitiesSize) {
                    index = 0;
                }

                if (activeCellStates[cellIndex].getPossibilities()[index]) {
                    validPossibilityFound = true;
                }
            }

            //We let it index one last time at the end of the last loop
            //So we have a 1-based value for that possibility now
            if(validPossibilityFound) {
                result[0] = cellIndex;
                result[1] = index;
            }
        }

        return result;
    }

    private void addToMoveRecord(int cellIndex, MoveRecord.MoveType moveType, CellState previousState) {
        if(moveIndex >= moveRecord.length) {
            MoveRecord[] largerArray = new MoveRecord[moveRecord.length + 81];
            for(int i = 0; i < largerArray.length; i++) {
                if(i < moveRecord.length) {
                    largerArray[i] = moveRecord[i];
                } else {
                    largerArray[i] = new MoveRecord();
                }
            }
            moveRecord = largerArray;
        }

        moveRecord[moveIndex].setCellIndex(cellIndex);
        moveRecord[moveIndex].setMoveType(moveType);
        previousState.duplicateState(moveRecord[moveIndex].getPreviousState());

        moveIndex++;
    }

    public boolean undo() {
        boolean handled = false;

        if(moveIndex > 0) {
            moveIndex--;

            int cellIndex = moveRecord[moveIndex].getCellIndex();

            MoveRecord.MoveType moveType = moveRecord[moveIndex].getMoveType();
            if(moveType == MoveRecord.MoveType.SET) {
                if(activeCellStates[moveIndex].getOneBasedChosenNumber() != 0) {
                    clearCellState(cellIndex);
                }
                if(moveRecord[moveIndex].getPreviousState().getOneBasedChosenNumber() != 0) {
                    removePossibilityFromCounterparts(moveRecord[moveIndex].getPreviousState().getOneBasedChosenNumber() - 1, cellIndex);
                }
            } else if(moveType == MoveRecord.MoveType.CLEAR) {
                if(moveRecord[moveIndex].getPreviousState().getOneBasedChosenNumber() != 0) {
                    removePossibilityFromCounterparts(moveRecord[moveIndex].getPreviousState().getOneBasedChosenNumber() - 1, cellIndex);
                }
            }

            moveRecord[moveIndex].getPreviousState().duplicateState(activeCellStates[cellIndex]);
            cells[cellIndex].setCellState(activeCellStates[cellIndex]);

            moveRecord[moveIndex].setCellIndex(-1);
            moveRecord[moveIndex].getPreviousState().resetState();

            handled = true;
        }

        return handled;
    }

    public void saveState(Bundle inState) {
        inState.putParcelableArray(BUNDLE_KEY_STATES, activeCellStates);
        inState.putIntArray(BUNDLE_KEY_WINNING_BOARD, winningBoard);
        inState.putInt(BUNDLE_KEY_MOVE_INDEX, moveIndex);
        inState.putParcelableArray(BUNDLE_KEY_MOVE_RECORD, moveRecord);

        boolean[] cellsEnabled = new boolean[cells.length];
        for(int i = 0; i < cells.length; i++) {
            cellsEnabled[i] = cells[i].isEnabled();
        }
        inState.putBooleanArray(BUNDLE_KEY_CELLS_ENABLED, cellsEnabled);
    }

    public void restoreState(Bundle saveInstanceState) {
        Parcelable[] parcelableArray = saveInstanceState.getParcelableArray(BUNDLE_KEY_STATES);
        boolean[] cellsEnabled = saveInstanceState.getBooleanArray(BUNDLE_KEY_CELLS_ENABLED);
        for(int i = 0; i < activeCellStates.length; i++) {
            activeCellStates[i] = (CellState)parcelableArray[i];
            cells[i].setCellState(activeCellStates[i]);
            cells[i].setEnabled(cellsEnabled[i]);
        }
        winningBoard = saveInstanceState.getIntArray(BUNDLE_KEY_WINNING_BOARD);
        moveIndex = saveInstanceState.getInt(BUNDLE_KEY_MOVE_INDEX);
        parcelableArray = saveInstanceState.getParcelableArray(BUNDLE_KEY_MOVE_RECORD);
        moveRecord = new MoveRecord[parcelableArray.length];
        for(int i = 0; i < moveRecord.length; i++) {
            moveRecord[i] = (MoveRecord)parcelableArray[i];
        }
    }

    public interface BoardListener {
        public void onShowSetCellFrag(boolean[] possibilities, int cellIndex);
        public void onShowMarkCellFrag(boolean[] markedPossibilities, boolean isMarked, int cellIndex);
        public void onComplete();
    }
}
