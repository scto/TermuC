package cn.rbc.termuc;
import android.content.*;
import android.content.SharedPreferences.*;

public class Settings
{
	private final static String
		KEY_DARKMODE = "darkmode",
		KEY_WORDWRAP = "wordwrap",
		KEY_WHITESPACE = "whitespace",
		KEY_CFLAGS = "cflags";

	private static SharedPreferences mSharedPref = null;
	private static Settings mInstance = null;
	private static int mRefCount = 0;

	public static boolean mDarkMode, mWordWrap, mWhiteSpace;
	public static String mCFlags;

	private Settings(SharedPreferences pref) {
		mSharedPref = pref;
		initConfs();
	}

	public static Settings getInstance(SharedPreferences pref) {
        if (mInstance == null) {
            mInstance = new Settings(pref);
        }
        assert (pref == mSharedPref);
        mRefCount++;
        return mInstance;
    }

	public static void writeBack() {
        Editor editor = mSharedPref.edit();
        editor.putBoolean(KEY_DARKMODE, mDarkMode);
        editor.putBoolean(KEY_WORDWRAP, mWordWrap);
		editor.putBoolean(KEY_WHITESPACE, mWhiteSpace);
        editor.putString(KEY_CFLAGS, mCFlags);
        editor.commit();
    }

    public static void releaseInstance() {
        mRefCount--;
        if (mRefCount == 0) {
            mInstance = null;
        }
    }

    private void initConfs() {
        mDarkMode = mSharedPref.getBoolean(KEY_DARKMODE, false);
        mWordWrap = mSharedPref.getBoolean(KEY_WORDWRAP, true);
		mWhiteSpace = mSharedPref.getBoolean(KEY_WHITESPACE, false);
        mCFlags = mSharedPref.getString(KEY_CFLAGS, "-lm -Wall");
    }
}
