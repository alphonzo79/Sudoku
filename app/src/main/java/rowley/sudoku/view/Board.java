package rowley.sudoku.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

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
            }
        }

        rand = new Random(System.currentTimeMillis());
    }

    public void initializeBoard(final double targetComplexity) {
        long startTime = System.currentTimeMillis();

        moveIndex = 0;
        for(int i = 0; i < moveRecord.length; i++) {
            moveRecord[i] = -1;
        }

        for(Cell cell : cells) {
            cell.resetCell();
        }

        if(!findValueForCellAndAllOthers(0)) {
            Toast.makeText(getContext(), R.string.failed_to_build_board, Toast.LENGTH_SHORT).show();
        }
        for(int i = 0; i < cells.length; i++) {
            winningBoard[i] = cells[i].getOneBasedChosenNumber();
        }

        boolean shouldContinue = true;
        while(shouldContinue) {
            int cellIndex = rand.nextInt(cells.length);
            clearCell(cellIndex);
            cells[cellIndex].setEnabled(true);
            shouldContinue = estimateComplexity() < targetComplexity;
        }

        for(Cell cell : cells) {
            cell.finalizeCell();
        }

        Log.d("JAR", "Time to create: " + (System.currentTimeMillis() - startTime));
    }

    private boolean findValueForCellAndAllOthers(int targetCell) {
        if(targetCell > cells.length - 1) {
            return true;
        }

        boolean result = false;

        int possibilitiesSize = cells[targetCell].getPossibilities().length;
        int index = rand.nextInt(possibilitiesSize);
        for(int i = 0; i < possibilitiesSize && !result; i++, index++) {
            if(index >= possibilitiesSize) {
                index = 0;
            }

            if(cells[targetCell].getPossibilities()[index]) {
                cells[targetCell].setChosenNumber(index + 1, false);
                Log.d("JAR", "Trying Value " + (index + 1) + " in cell " + targetCell);
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
        boolean success = true;

        int rowNum = cellNum / 9;
        int workingIndex = cellNum - 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum + 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex++;
        }

        int columnNum = cellNum % 9;
        workingIndex = cellNum - 9;
        while(workingIndex >= 0) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex += -9;
        }
        workingIndex = cellNum + 9;
        while(workingIndex >= 0 && workingIndex < cells.length) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex += 9;
        }

        //for the block follow the formula we have come up with, save time by skipping the current row and current column.
        //Brute-force try two rows above and two rows below
        int blockNum = figureBlockNumForCell(cellNum);
        workingIndex = cellNum - 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum - 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex++;
        }
        workingIndex = cellNum - 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum - 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex++;
        }
        workingIndex = cellNum + 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum + 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex++;
        }
        workingIndex = cellNum + 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
                success = false;
            }
            workingIndex--;
        }
        workingIndex = cellNum + 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            cells[workingIndex].removePossibility(zeroBasedPossibility);
            if(!(doesCellHaveMoreThanZeroPossibilities(cells[workingIndex]) || cells[workingIndex].getOneBasedChosenNumber() > 0)) {
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
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum + 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }

        int columnNum = cellNum % 9;
        workingIndex = cellNum - 9;
        while(workingIndex >= 0) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex += -9;
        }
        workingIndex = cellNum + 9;
        while(workingIndex >= 0 && workingIndex < cells.length) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex += 9;
        }

        //for the block follow the formula we have come up with, save time by skipping the current row and current column.
        //Brute-force try two rows above and two rows below
        int blockNum = figureBlockNumForCell(cellNum);
        workingIndex = cellNum - 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum - 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }
        workingIndex = cellNum - 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum - 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }
        workingIndex = cellNum + 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum + 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }
        workingIndex = cellNum + 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex--;
        }
        workingIndex = cellNum + 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum) {
            if(!isPossibilitySetAsChosenInCounterpart(zeroBasedPossibility, workingIndex)) {
                cells[workingIndex].addPossibility(zeroBasedPossibility);
            }
            workingIndex++;
        }
    }

    private boolean isPossibilitySetAsChosenInCounterpart(int zeroBasedPossibility, int cellNum) {
        boolean result = false;

        int rowNum = cellNum / 9;
        int workingIndex = cellNum - 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum + 1;
        while(workingIndex >= 0 && workingIndex / 9 == rowNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }

        int columnNum = cellNum % 9;
        workingIndex = cellNum - 9;
        while(workingIndex >= 0 && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex += -9;
        }
        workingIndex = cellNum + 9;
        while(workingIndex >= 0 && workingIndex < cells.length && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex += 9;
        }

        //for the block follow the formula we have come up with, save time by skipping the current row and current column.
        //Brute-force try two rows above and two rows below
        int blockNum = figureBlockNumForCell(cellNum);
        workingIndex = cellNum - 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum - 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }
        workingIndex = cellNum - 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum - 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }
        workingIndex = cellNum + 8;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum + 10;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }
        workingIndex = cellNum + 17;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex--;
        }
        workingIndex = cellNum + 19;
        while(workingIndex >=0 && figureBlockNumForCell(workingIndex) == blockNum && !result) {
            result = cells[workingIndex].getOneBasedChosenNumber() == zeroBasedPossibility + 1;
            workingIndex++;
        }

        return result;
    }

    private boolean doesCellHaveMoreThanZeroPossibilities(Cell cell) {
        for(boolean possibility : cell.getPossibilities()) {
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

        for(Cell cell : cells) {
            if(cell.getOneBasedChosenNumber() == 0) {
                possibilitiesCount = 0;
                for(boolean possible : cell.getPossibilities()) {
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

        for(Cell cell : cells) {
            int chosenNum = cell.getOneBasedChosenNumber();
            if(chosenNum == 0) {
                result = false;
                break;
            } else if(isPossibilitySetAsChosenInCounterpart(chosenNum - 1, Integer.valueOf((String)cell.getTag()))) {
                result = false;
                break;
            }
        }

        return result;
    }

    public void clearCell(int cellIndex) {
        int retrieved = cells[cellIndex].removeOneBasedChosenNumber();
        if(retrieved > 0) {
            addPossibilityToCounterparts(retrieved - 1, cellIndex);
            Log.d("JAR", "Found complexity of " + estimateComplexity());
        }
    }

    public void setNumToCell(int oneBasedChosenNumber, int cellIndex) {
        int alreadySet = cells[cellIndex].getOneBasedChosenNumber();
        if(alreadySet != 0) {
            clearCell(cellIndex);
        }

        cells[cellIndex].setChosenNumber(oneBasedChosenNumber, true);
        boolean validMove = removePossibilityFromCounterparts(oneBasedChosenNumber - 1, cellIndex);
        if(warnOnBadEntry) {
            if(validMove) {
                validMove = !isPossibilitySetAsChosenInCounterpart(oneBasedChosenNumber - 1, cellIndex);
            }
            if(!validMove) {
                cells[cellIndex].removeOneBasedChosenNumber();
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
        cells[cellIndex].setMarkedGuesses(markedPossibilities);
    }

    public void markCellAsPivot(boolean isMarked, int cellIndex) {
        cells[cellIndex].toggleMarked(isMarked);
    }

    public void setWarnOnBadEntry(boolean warnOnBadEntry) {
        this.warnOnBadEntry = warnOnBadEntry;
    }

    @Override
    public void onClick(View v) {
        if(getContext() instanceof BoardListener) {
            int index = Integer.parseInt((String)v.getTag());
            ((BoardListener)getContext()).onShowSetCellFrag(cells[index].getPossibilities(), index);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if(getContext() instanceof BoardListener) {
            int index = Integer.parseInt((String)v.getTag());
            ((BoardListener)getContext()).onShowMarkCellFrag(cells[index].getMarkedGuesses(), cells[index].getIsMarked(), index);
        }
        return true;
    }

    public interface BoardListener {
        public void onShowSetCellFrag(boolean[] possibilities, int cellIndex);
        public void onShowMarkCellFrag(boolean[] markedPossibilities, boolean isMarked, int cellIndex);
        public void onComplete();
    }
}
