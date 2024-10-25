package cn.rbc.termuc;
import android.content.*;
import android.content.SharedPreferences.*;
import android.preference.*;
import android.content.pm.*;
import android.app.AlertDialog.Builder;
import android.net.*;
import java.util.*;
import android.util.*;
import android.widget.*;
import android.app.*;
import android.view.*;
import android.graphics.drawable.*;
import cn.rbc.codeeditor.util.*;
import android.graphics.*;

public class Application extends android.app.Application {
	final static String
	KEY_DARKMODE = "darkmode",
	KEY_FONT = "font",
	KEY_MYFONT = "myfont",
	KEY_WORDWRAP = "wordwrap",
	KEY_WHITESPACE = "whitespace",
	KEY_TEXTSIZE = "fontsize",
	KEY_SHOW_HIDDEN = "showhidden",
	KEY_CHECKAPP = "checkapp",
	KEY_INITAPP = "initapp",
	KEY_CFLAGS = "cflags",
	KEY_COMPLETION = "completion",
	KEY_LSP_HOST = "lsphost",
	KEY_LSP_PORT = "lspport";

	public static boolean dark_mode, wordwrap, whitespace, show_hidden;
	public static String font, cflags, completion, lsp_host;
	public static int lsp_port, textsize;

	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		initConfs();
	}

    private void initConfs() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        dark_mode = sp.getBoolean(KEY_DARKMODE, false);
		String f = sp.getString(KEY_FONT, "m");
		if ("c".equals(f))
			f = sp.getString(KEY_MYFONT, "");
		font = f;
		textsize = Integer.parseInt(sp.getString(KEY_TEXTSIZE, "14"));
        wordwrap = sp.getBoolean(KEY_WORDWRAP, true);
		whitespace = sp.getBoolean(KEY_WHITESPACE, false);
		show_hidden = sp.getBoolean(KEY_SHOW_HIDDEN, true);
        cflags = sp.getString(KEY_CFLAGS, "-lm -Wall");
		completion = sp.getString(KEY_COMPLETION, "s");
		lsp_host = sp.getString(KEY_LSP_HOST, "127.0.0.1");
		lsp_port = Integer.parseInt(sp.getString(KEY_LSP_PORT, "48455"));
    }

	static Typeface typeface() {
		try {
			return "n".equals(font) ? Typeface.SANS_SERIF
				: "s".equals(font) ? Typeface.SERIF
				: "m".equals(font) ? Typeface.MONOSPACE
				: Typeface.createFromFile(font);
		} catch (RuntimeException re) {
			return Typeface.MONOSPACE;
		}
	}
}
