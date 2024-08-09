package cn.rbc.termuc;
import android.content.*;
import android.os.*;
import java.io.*;
import java.nio.charset.*;
import android.util.*;
import java.util.*;

public class Utils {
	public final static File ROOT = Environment.getExternalStorageDirectory();
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

	public static String escape(String str) {
        return str.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$").replace("\"", "\\\"");
    }

	public static boolean isBlob(File f) {
		int i = Math.min((int)f.length(), 2048);
		try {
			FileInputStream file = new FileInputStream(f);
			byte[] bArr = new byte[i];
			new DataInputStream(file).readFully(bArr);
			for (int i2 = 0; i2 < i; i2++) {
				if (bArr[i2] == 0) {
					file.close();
					return true;
				}
			}
			file.close();
		} catch (IOException e) {}
		return false;
	}

	public static boolean removeFiles(File dir) {
		File[] fl = dir.listFiles();
		if (fl!=null)
			for (File f:fl)
				removeFiles(f);
		return dir.delete();
	}
}
