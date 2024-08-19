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

public class Application extends android.app.Application
{
	final static String
		KEY_DARKMODE = "darkmode",
		KEY_WORDWRAP = "wordwrap",
		KEY_WHITESPACE = "whitespace",
		KEY_TEXTSIZE = "textsize",
		KEY_SHOW_HIDDEN = "showhidden",
		KEY_CHECKAPP = "checkapp",
		KEY_CFLAGS = "cflags",
		KEY_COMPLETION = "completion",
		KEY_LSP_HOST = "lsphost",
		KEY_LSP_PORT = "lspport";

	public static boolean dark_mode, wordwrap, whitespace, show_hidden;
	public static String cflags, completion, lsp_host;
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
		textsize = Integer.parseInt(sp.getString(KEY_TEXTSIZE, "14"));
        wordwrap = sp.getBoolean(KEY_WORDWRAP, true);
		whitespace = sp.getBoolean(KEY_WHITESPACE, false);
		show_hidden = sp.getBoolean(KEY_SHOW_HIDDEN, true);
        cflags = sp.getString(KEY_CFLAGS, "-lm -Wall");
		completion = sp.getString(KEY_COMPLETION, "s");
		lsp_host = sp.getString(KEY_LSP_HOST, "127.0.0.1");
		lsp_port = Integer.parseInt(sp.getString(KEY_LSP_PORT, "48455"));
    }

	public static void testApp(Activity ctx, boolean manually) {
		PackageManager pm = ctx.getPackageManager();
		try {
			pm.getPackageInfo("com.termux", PackageManager.GET_GIDS);
			if (manually)
				HelperUtils.show(Toast.makeText(ctx, R.string.installed, Toast.LENGTH_SHORT));
		} catch (PackageManager.NameNotFoundException e) {
			Builder bd = new Builder(ctx);
			bd.setTitle(R.string.install_app);
			bd.setMessage(R.string.confirm_install);
			bd.setNegativeButton(android.R.string.cancel, null);
			Install oc = new Install(ctx);
			bd.setPositiveButton(android.R.string.ok, oc);
			if (!manually)
				bd.setNeutralButton(R.string.no_remind, oc);
			bd.create().show();
		}
	}

	private static class Install
	implements DialogInterface.OnClickListener, SimpleAdapter.ViewBinder {
		Activity mApp;
		Install(Activity app) {
			mApp = app;
		}
		public boolean setViewValue(View v, Object o, String k) {
			if (v instanceof ImageView && o instanceof Drawable) {
				((ImageView)v).setImageDrawable((Drawable)o);
				return true;
			}
			return false;
		}
		public void onClick(DialogInterface d, int p) {
			Activity app = mApp;
			if (p==DialogInterface.BUTTON_NEUTRAL) {
				app.getPreferences(MODE_PRIVATE).edit().putBoolean(MainActivity.TESTAPP, false).commit();
				return;
			}
			Uri uri = Uri.parse("market://details?id=com.termux");
			Intent it = new Intent(Intent.ACTION_VIEW, uri);
			PackageManager pm = app.getPackageManager();
			List<ResolveInfo> lst = pm.queryIntentActivities(it, 0);
			List<Map<String,Object>> list = new ArrayList<>();
			Map<String,Object> m;
			final Intent[] its = new Intent[lst.size()+2];
			int i = 0;
			for (ResolveInfo ri:lst) {
				m = new ArrayMap<>();
				m.put("i", ri.loadIcon(pm));
				m.put("n", ri.loadLabel(pm));
				list.add(m);
				it = new Intent(Intent.ACTION_VIEW, uri);
				it.setPackage(ri.activityInfo.packageName);
				its[i++] = it;
			}
			m = new ArrayMap<>();
			m.put("i", null);
			m.put("n", "Github Release website");
			list.add(m);
			its[i++] = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/termux/termux-app/releases"));
			m = new ArrayMap<>();
			m.put("i", null);
			m.put("n", "F-Droid website");
			list.add(m);
			its[i] = new Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux"));
			Builder bd = new Builder(app);
			bd.setTitle(R.string.install_via);
			bd.setNegativeButton(android.R.string.cancel, null);
			SimpleAdapter sadp = new SimpleAdapter(app, list, R.layout.file_item, new String[]{"i", "n"}, new int[]{R.id.file_icon, R.id.file_name});
			sadp.setViewBinder(this);
			bd.setAdapter(sadp, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface d, int p) {
						mApp.startActivity(its[p]);
					}
				});
			bd.create().show();
		}
	}
}
