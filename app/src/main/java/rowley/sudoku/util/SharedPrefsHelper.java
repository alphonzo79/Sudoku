package rowley.sudoku.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsHelper {
    private static final String SHARED_PREFS_NAME = "sudokuPrefs";
    private static final String PROTECT_AGAINST_BAD_MOVES = "protectAgainstBadMoves";

    public static boolean getProtectAgainstBadMoves(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PROTECT_AGAINST_BAD_MOVES, false);
    }

    public static void setProtectAgainstBadMoves(Context context, boolean protect) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PROTECT_AGAINST_BAD_MOVES, protect);
        editor.commit();
    }
}
