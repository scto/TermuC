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

	private static SharedPreferences mSharedPref;
	private static Settings mInstance;
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
        SharedPreferences.Editor ed = mSharedPref.edit();
		ed.putBoolean(KEY_DARKMODE, dark_mode);
		ed.putBoolean(KEY_WORDWRAP, wordwrap);
		ed.putBoolean(KEY_WHITESPACE, whitespace);
		ed.putBoolean(KEY_SHOW_HIDDEN, show_hidden);
		ed.putInt(KEY_TEXTSIZE, textsize);
		ed.putString(KEY_CFLAGS, cflags);
		ed.putString(KEY_COMPLETION, completion);
		ed.putString(KEY_LSP_HOST, lsp_host);
		ed.putString(KEY_LSP_PORT, Integer.toString(lsp_port));
		ed.commit();
	}

    public static void releaseInstance() {
        mRefCount--;
        if (mRefCount == 0) {
            mInstance = null;
        }
    }

    private void initConfs() {
		SharedPreferences sp = mSharedPref;
        dark_mode = sp.getBoolean(KEY_DARKMODE, false);
		textsize = sp.getInt(KEY_TEXTSIZE, 14);
        wordwrap = sp.getBoolean(KEY_WORDWRAP, true);
		whitespace = sp.getBoolean(KEY_WHITESPACE, false);
		show_hidden = sp.getBoolean(KEY_SHOW_HIDDEN, true);
        cflags = sp.getString(KEY_CFLAGS, "-lm -Wall");
		completion = sp.getString(KEY_COMPLETION, "s");
		lsp_host = sp.getString(KEY_LSP_HOST, "127.0.0.1");
		lsp_port = Integer.parseInt(sp.getString(KEY_LSP_PORT, "48455"));
    }
}
