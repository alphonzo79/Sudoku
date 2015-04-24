package rowley.sudoku.activity;

import android.os.Bundle;

import com.crashlytics.android.Crashlytics;

/**
 * Created by joe on 4/23/15.
 */
public class MainActivityBuildVariant extends MainActivityParent {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Crashlytics.start(this);
    }
}
