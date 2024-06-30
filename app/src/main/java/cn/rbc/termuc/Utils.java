package cn.rbc.termuc;
import android.content.*;

public class Utils
{
	public final static String PREF = "/data/data/com.termux/files";
    private final static String PREFC = "com.termux.RUN_COMMAND_";

	public static void run(Context cont, String cmd, String[] args, String pwd, boolean background) {
		Intent intent = new Intent()
        .setClassName("com.termux", "com.termux.app.RunCommandService")
        .setAction("com.termux.RUN_COMMAND")
        .putExtra(new StringBuilder(PREFC).append("PATH").toString(), cmd)
        .putExtra(new StringBuilder(PREFC).append("RUNNER").toString(), "app-shell")
        .putExtra(new StringBuilder(PREFC).append("ARGUMENTS").toString(), args)
        .putExtra(new StringBuilder(PREFC).append("WORKDIR").toString(), pwd)
        .putExtra(new StringBuilder(PREFC).append("BACKGROUND").toString(), background)
        .putExtra(new StringBuilder(PREFC).append("SESSION_ACTION").toString(), "0");
        cont.startService(intent);
	}
}
