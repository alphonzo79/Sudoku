package rowley.sudoku.fragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import rowley.sudoku.R;

/**
 * Created by joe on 4/21/15.
 */
public class MarkCellDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String MARKED_POSSIBLITIES = "markedPossibilities";
    private static final String IS_MARKED = "isMarked";
    private static final String CELL_INDEX = "cellIndex";

    private boolean[] markedPossibilities;
    private boolean isMarked;

    private View[] cells;
    private View one, two, three, four, five, six, seven, eight, nine;
    private Button markButton, saveButton, clearButton;

    public static MarkCellDialogFragment newInstance(boolean[] markedPossibilities, boolean isMarked, int cellIndex) {
        MarkCellDialogFragment frag = new MarkCellDialogFragment();

        Bundle args = new Bundle();
        args.putBooleanArray(MARKED_POSSIBLITIES, markedPossibilities);
        args.putBoolean(IS_MARKED, isMarked);
        args.putInt(CELL_INDEX, cellIndex);
        frag.setArguments(args);

        frag.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        return frag;
    }

    public MarkCellDialogFragment() {

    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if(!(activity instanceof MarkCellFragListener)) {
            throw new IllegalStateException("The hosting activity must implement the MarkCellFragListener");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
        View root = inflator.inflate(R.layout.dialog_fragment_mark_cell, container, false);

        cells = new View[9];

        one = root.findViewById(R.id.select_1);
        one.setOnClickListener(this);
        cells[0] = one;

        two = root.findViewById(R.id.select_2);
        two.setOnClickListener(this);
        cells[1] = two;

        three = root.findViewById(R.id.select_3);
        three.setOnClickListener(this);
        cells[2] = three;

        four = root.findViewById(R.id.select_4);
        four.setOnClickListener(this);
        cells[3] = four;

        five = root.findViewById(R.id.select_5);
        five.setOnClickListener(this);
        cells[4] = five;

        six = root.findViewById(R.id.select_6);
        six.setOnClickListener(this);
        cells[5] = six;

        seven = root.findViewById(R.id.select_7);
        seven.setOnClickListener(this);
        cells[6] = seven;

        eight = root.findViewById(R.id.select_8);
        eight.setOnClickListener(this);
        cells[7] = eight;

        nine = root.findViewById(R.id.select_9);
        nine.setOnClickListener(this);
        cells[8] = nine;

        markButton = (Button)root.findViewById(R.id.mark_button);
        markButton.setOnClickListener(this);
        isMarked = getArguments().getBoolean(IS_MARKED, false);
        setMarkButtonText();
        
        saveButton = (Button)root.findViewById(R.id.save_notes_button);
        saveButton.setOnClickListener(this);
        
        clearButton = (Button)root.findViewById(R.id.clear_notes_button);
        clearButton.setOnClickListener(this);

        markedPossibilities = getArguments().getBooleanArray(MARKED_POSSIBLITIES);
        for(int i = 0; i < markedPossibilities.length; i++) {
            cells[i].setActivated(markedPossibilities[i]);
        }
        
        return root;
    }

    private void setMarkButtonText() {
        if(isMarked) {
            markButton.setText(R.string.clear_mark);
        } else {
            markButton.setText(R.string.mark_as_pivot);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.mark_button:
                isMarked = !isMarked;
                setMarkButtonText();
                ((MarkCellFragListener)getActivity()).onMarkCell(isMarked, getArguments().getInt(CELL_INDEX));
                dismiss();
                break;
            case R.id.save_notes_button:
                ((MarkCellFragListener)getActivity()).onSaveNotes(markedPossibilities, getArguments().getInt(CELL_INDEX));
                dismiss();
                break;
            case R.id.clear_notes_button:
                for(int i = 0; i < markedPossibilities.length; i++) {
                    markedPossibilities[i] = false;
                    cells[i].setActivated(false);
                }
                ((MarkCellFragListener)getActivity()).onSaveNotes(markedPossibilities, getArguments().getInt(CELL_INDEX));
                dismiss();
                break;
            case R.id.select_1:
                markedPossibilities[0] = !markedPossibilities[0];
                one.setActivated(markedPossibilities[0]);
                break;
            case R.id.select_2:
                markedPossibilities[1] = !markedPossibilities[1];
                two.setActivated(markedPossibilities[1]);
                break;
            case R.id.select_3:
                markedPossibilities[2] = !markedPossibilities[2];
                three.setActivated(markedPossibilities[2]);
                break;
            case R.id.select_4:
                markedPossibilities[3] = !markedPossibilities[3];
                four.setActivated(markedPossibilities[3]);
                break;
            case R.id.select_5:
                markedPossibilities[4] = !markedPossibilities[4];
                five.setActivated(markedPossibilities[4]);
                break;
            case R.id.select_6:
                markedPossibilities[5] = !markedPossibilities[5];
                six.setActivated(markedPossibilities[5]);
                break;
            case R.id.select_7:
                markedPossibilities[6] = !markedPossibilities[6];
                seven.setActivated(markedPossibilities[6]);
                break;
            case R.id.select_8:
                markedPossibilities[7] = !markedPossibilities[7];
                eight.setActivated(markedPossibilities[7]);
                break;
            case R.id.select_9:
                markedPossibilities[8] = !markedPossibilities[8];
                nine.setActivated(markedPossibilities[8]);
                break;
        }
    }

    public interface MarkCellFragListener {
        public void onSaveNotes(boolean[] markedPossibilities, int cellIndex);
        public void onMarkCell(boolean isMarked, int cellIndex);
    }
}
