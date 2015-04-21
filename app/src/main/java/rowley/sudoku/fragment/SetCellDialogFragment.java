package rowley.sudoku.fragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import rowley.sudoku.R;

/**
 * Created by joe on 4/20/15.
 */
public class SetCellDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String POSSIBILITIES_KEY = "possibilities";
    private static final String CELL_INDEX_KEY = "cellIndex";

    private View[] cells;
    private View one, two, three, four, five, six, seven, eight, nine;
    private View clear;

    public static SetCellDialogFragment newInstance(boolean[] possibilities, int cellIndex) {
        SetCellDialogFragment frag = new SetCellDialogFragment();

        Bundle args = new Bundle();
        args.putBooleanArray(POSSIBILITIES_KEY, possibilities);
        args.putInt(CELL_INDEX_KEY, cellIndex);
        frag.setArguments(args);

        frag.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        return frag;
    }

    public SetCellDialogFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(!(activity instanceof SetCellFragListener)) {
            throw new IllegalStateException("The host activity must implement the SetCellFragListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_fragment_set_cell, container, false);

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

        clear = root.findViewById(R.id.clear_button);
        clear.setOnClickListener(this);

        boolean[] possibilities = getArguments().getBooleanArray(POSSIBILITIES_KEY);
        for(int i = 0; i < possibilities.length; i++) {
            cells[i].setEnabled(possibilities[i]);
        }

        return root;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.clear_button:
                ((SetCellFragListener)getActivity()).onClear(getArguments().getInt(CELL_INDEX_KEY));
                break;
            case R.id.select_1:
                ((SetCellFragListener)getActivity()).onCellSet(1, getArguments().getInt(CELL_INDEX_KEY));
                break;
            case R.id.select_2:
                ((SetCellFragListener)getActivity()).onCellSet(2, getArguments().getInt(CELL_INDEX_KEY));
                break;
            case R.id.select_3:
                ((SetCellFragListener)getActivity()).onCellSet(3, getArguments().getInt(CELL_INDEX_KEY));
                break;
            case R.id.select_4:
                ((SetCellFragListener)getActivity()).onCellSet(4, getArguments().getInt(CELL_INDEX_KEY));
                break;
            case R.id.select_5:
                ((SetCellFragListener)getActivity()).onCellSet(5, getArguments().getInt(CELL_INDEX_KEY));
                break;
            case R.id.select_6:
                ((SetCellFragListener)getActivity()).onCellSet(6, getArguments().getInt(CELL_INDEX_KEY));
                break;
            case R.id.select_7:
                ((SetCellFragListener)getActivity()).onCellSet(7, getArguments().getInt(CELL_INDEX_KEY));
                break;
            case R.id.select_8:
                ((SetCellFragListener)getActivity()).onCellSet(8, getArguments().getInt(CELL_INDEX_KEY));
                break;
            case R.id.select_9:
                ((SetCellFragListener)getActivity()).onCellSet(9, getArguments().getInt(CELL_INDEX_KEY));
                break;
        }

        dismiss();
    }

    public interface SetCellFragListener {
        public void onClear(int cellIndex);
        public void onCellSet(int oneBasedNumChosen, int cellIndex);
    }
}
