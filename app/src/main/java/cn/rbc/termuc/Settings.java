package cn.rbc.termuc;
import android.content.*;
import android.content.SharedPreferences.*;

public class Settings
{
	private final static String
		KEY_DARKMODE = "darkmode",
		KEY_WORDWRAP = "wordwrap",
		KEY_WHITESPACE = "whitespace",
		KEY_TEXTSIZE = "textsize",
		KEY_SHOW_HIDDEN = "showhidden",
		KEY_CFLAGS = "cflags",
		KEY_COMPLETION = "completion",
		KEY_LSP_HOST = "lsphost",
		KEY_LSP_PORT = "lspport";

	private static SharedPreferences mSharedPref = null;
	private static Settings mInstance = null;
	private static int mRefCount = 0;

	public static boolean dark_mode, wordwrap, whitespace, show_hidden;
	public static String cflags, completion, lsp_host;
	public static int lsp_port, textsize;

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
        mSharedPref.edit()
		.putBoolean(KEY_DARKMODE, dark_mode)
		.putBoolean(KEY_WORDWRAP, wordwrap)
		.putBoolean(KEY_WHITESPACE, whitespace)
		.putBoolean(KEY_SHOW_HIDDEN, show_hidden)
		.putInt(KEY_TEXTSIZE, textsize)
		.putString(KEY_CFLAGS, cflags)
		.putString(KEY_COMPLETION, completion)
		.putString(KEY_LSP_HOST, lsp_host)
		.putString(KEY_LSP_PORT, Integer.toString(lsp_port))
		.commit();
	}

    public static void releaseInstance() {
        mRefCount--;
        if (mRefCount == 0) {
            mInstance = null;
        }
    }

    private void initConfs() {
        dark_mode = mSharedPref.getBoolean(KEY_DARKMODE, false);
		textsize = mSharedPref.getInt(KEY_TEXTSIZE, 14);
        wordwrap = mSharedPref.getBoolean(KEY_WORDWRAP, true);
		whitespace = mSharedPref.getBoolean(KEY_WHITESPACE, false);
		show_hidden = mSharedPref.getBoolean(KEY_SHOW_HIDDEN, true);
        cflags = mSharedPref.getString(KEY_CFLAGS, "-lm -Wall");
		completion = mSharedPref.getString(KEY_COMPLETION, "s");
		lsp_host = mSharedPref.getString(KEY_LSP_HOST, "127.0.0.1");
		lsp_port = Integer.parseInt(mSharedPref.getString(KEY_LSP_PORT, "48455"));
    }
}
