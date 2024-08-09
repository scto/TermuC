package cn.rbc.termuc;
import android.net.Uri;
import android.content.*;
import java.net.*;
import java.io.*;
import org.json.JSONObject;
import java.util.*;
import android.util.Log;
import android.app.*;
import java.nio.charset.StandardCharsets;
import android.os.*;
import cn.rbc.codeeditor.util.*;
import android.widget.*;
import android.util.JsonReader;
import java.util.concurrent.*;

public class Lsp implements Runnable {
	final static int INITIALIZE = 0, INITIALIZED = 1,
	OPEN = 2, CLOSE = 3,
	COMPLETION = 4, FIX = 5, CHANGE = 6, SAVE = 7, NOTI = 8,
	ERROR = -1;
	private final static String TAG = "LSP";
	private final static byte[] CONTENTLEN = "Content-Length: ".getBytes(StandardCharsets.UTF_8);
	private int tp;
	private Socket sk;
	private ExecutorService mExecutor;
	private char[] compTrigs = {};
	private long mLastReceivedTime;
	private Handler mRead;

	public void start(final Context mC, Handler read) {
		Utils.run(mC, "/system/bin/nc", new String[]{"-l", "-s", Settings.lsp_host, "-p", Integer.toString(Settings.lsp_port), "clangd", "--header-insertion-decorators=0", "--completion-style=bundled"}, Environment.getExternalStorageDirectory().getAbsolutePath(), true);
		mExecutor = Executors.newSingleThreadExecutor();
		sk = new Socket();
		mRead = read;
		new Thread(this).start();
	}

	public void run() {
		try{
			int i = 0;
			do {
				try {
					sk = new Socket(Settings.lsp_host, Settings.lsp_port);
				} catch (SocketException s) {
					Thread.sleep(250L);
				}
				i++;
			} while (i<=20 && !sk.isConnected());
			if (i>20)
				throw new IOException("Connection failed");
			InputStream is = sk.getInputStream();
			byte[] b = new byte[16];
			OUTER:	while (true) {
				for (i=0;i<16;i++) {
					int t = is.read();
					if (t==-1)
						break OUTER;
					b[i] = (byte)t;
				}
				if (Arrays.equals(b, CONTENTLEN)) {
					int len = 0;
					while (Character.isDigit(i = is.read()))
						len = len * 10 + i - 48;
					mLastReceivedTime = System.currentTimeMillis();
					is.skip(3L);
					byte[] strb = new byte[len];
					for (i=0; i<len; i++)
						strb[i] = (byte)is.read();
					InputStream r = new ByteArrayInputStream(strb);
					JsonReader limitInput = new JsonReader(new InputStreamReader(r, StandardCharsets.UTF_8));
					Message msg = new Message();
					msg.what = tp;
					msg.obj = limitInput;
					mRead.sendMessage(msg);
					//tp = NOTI;
				}
			}
			is.close();
		} catch (Exception ioe) {
			Log.e(TAG, ioe.getMessage());
		}
		Message msg = new Message();
		msg.what = ERROR;
		mRead.sendMessage(msg);
	}

	public long lastReceivedTime() {
		return mLastReceivedTime;
	}

	private static String wrap(String m, Object p, boolean req) {
		StringBuilder s = new StringBuilder("{\"jsonrpc\":\"2.0\"");
		if (req)
			s.append(",\"id\":0");
		return s.append(",\"method\":\"").append(m)
		.append("\",\"params\":").append(JSONObject.wrap(p))
		.append("}").toString();
	}

	public void initialize() {
		tp = INITIALIZE;
		StringBuilder sb = new StringBuilder("{\"processId\":")
		.append(android.os.Process.myPid())
		.append(",\"initializationOptions\":{\"fallbackFlags\":[\"-Wall\"]}}");
		mExecutor.execute(new Send("initialize", sb.toString(), true));
	}

	public void setCompTrigs(char[] c) {
		compTrigs = c;
	}

	public byte isCompTrig(char c) {
		if (Character.isJavaIdentifierPart(c))
			return 1;
		for (int i=0,l=compTrigs.length; i<l; i++)
			if (compTrigs[i] == c)
				return 2;
		return 0;
	}

	public void initialized() {
		tp = INITIALIZED;
		mExecutor.execute(new Send("initialized", new HashMap(), false));
	}

	public void didClose(File f) {
		HashMap<String,String> m = new HashMap<>();
		m.put("uri", Uri.fromFile(f).toString());
		HashMap<String,HashMap> k = new HashMap<>();
		k.put("textDocument", m);
		tp = CLOSE;
		mExecutor.execute(new Send("textDocument/didClose", k, false));
	}

	public void didOpen(File f, String lang, String ct) {
		StringBuilder m = new StringBuilder("{\"textDocument\":{\"uri\":")
		.append(JSONObject.quote(Uri.fromFile(f).toString()))
		.append(",\"languageId\":\"").append(lang)
		.append("\",\"version\":0,\"text\":")
		.append(JSONObject.quote(ct))
		.append("}}");
		//tp = OPEN;
		mExecutor.execute(new Send("textDocument/didOpen", m.toString(), false));
	}

	public void didSave(File f) {
		String s = new StringBuilder("{\"textDocument\":{\"uri\":")
		.append(JSONObject.quote(Uri.fromFile(f).toString()))
		.append("}}").toString();
		//tp = SAVE;
		mExecutor.execute(new Send("textDocument/didSave", s, false));
	}

	public synchronized void didChange(File f, int version, String text) {
		String sb = new StringBuilder("{\"textDocument\":{\"uri\":")
		.append(JSONObject.quote(Uri.fromFile(f).toString()))
		.append(",\"version\":")
		.append(version)
		.append("},\"contentChanges\":[{\"text\":")
		.append(JSONObject.quote(text))
		.append("}]}").toString();
		mExecutor.execute(new Send("textDocument/didChange", sb, false));
	}

	public synchronized void didChange(File f, int version, List<Range> chs) {
		StringBuilder sb = new StringBuilder("{\"textDocument\":{\"uri\":")
		.append(JSONObject.quote(Uri.fromFile(f).toString()))
		.append(",\"version\":")
		.append(version)
		.append("},\"contentChanges\":[");
		for (int i=0,j=chs.size(); i<j; i++) {
			Range c = chs.get(i);
			sb.append("{\"range\":{\"start\":{\"line\":")
			.append(c.stl)
			.append(",\"character\":")
			.append(c.stc)
			.append("},\"end\":{\"line\":")
			.append(c.enl)
			.append(",\"character\":")
			.append(c.enc)
			.append("}},\"text\":")
			.append(JSONObject.quote(c.msg))
			.append("},");
		}
		sb.setCharAt(sb.length()-1, ']');
		sb.append('}');
		Log.d(TAG, sb.toString());
		//tp = CHANGE;
		mExecutor.execute(new Send("textDocument/didChange", sb.toString(), false));
	}

	public synchronized boolean completionTry(File f, int l, int c, char tgc) {
		byte b = isCompTrig(tgc);
		if (b==0)
			return false;
		StringBuilder sb = new StringBuilder("{\"textDocument\":{\"uri\":")
		.append(JSONObject.quote(Uri.fromFile(f).toString()))
		.append("},\"position\":{\"line\":")
		.append(l)
		.append(",\"character\":")
		.append(c)
		.append("},\"context\":{\"triggerKind\":")
		.append(b);
		if (b==2)
			sb.append(",\"triggerCharacter\":\"")
			.append(tgc)
			.append('"');
		sb.append("}}");
		tp = COMPLETION;
		Log.d(TAG, sb.toString());
		mExecutor.execute(new Send("textDocument/completion", sb.toString(), true));
		return true;
	}

	public synchronized void formatting(File fl, int tabSize, boolean useSpace) {
		StringBuilder sb = new StringBuilder("{\"textDocument\":{\"uri\":")
		.append(JSONObject.quote(Uri.fromFile(fl).toString()))
		.append("},\"options\":{\"tabSize\":")
		.append(tabSize)
		.append(",\"insertSpaces\":")
		.append(useSpace)
		.append("}}");
		Log.d(TAG, sb.toString());
		mExecutor.execute(new Send("textDocument/formatting", sb.toString(), true));
	}

	public synchronized void rangeFormatting(File fl, Range range, int tabSize, boolean useSpace) {
		StringBuilder sb = new StringBuilder("{\"textDocument\":{\"uri\":")
			.append(JSONObject.quote(Uri.fromFile(fl).toString()))
			.append("},\"range\":{\"start\":{\"line\":")
			.append(range.stl)
			.append(",\"character\":")
			.append(range.stc)
			.append("},\"end\":{\"line\":")
			.append(range.enl)
			.append(",\"character\":")
			.append(range.enc)
			.append("}},\"options\":{\"tabSize\":")
			.append(tabSize)
			.append(",\"insertSpaces\":")
			.append(useSpace)
			.append("}}");
		Log.d(TAG, sb.toString());
		mExecutor.execute(new Send("textDocument/rangeFormatting", sb.toString(), true));
	}

	public boolean isConnected() {
		return sk.isConnected();
	}

	class Send implements Runnable {
		private byte[] s;

		public Send(String cmd, Object hm, boolean req) {
			s = wrap(cmd, hm, req).getBytes(StandardCharsets.UTF_8);
		}

		public void run() {
			try {
				if (sk==null || sk.isClosed())
					sk = new Socket(Settings.lsp_host, Settings.lsp_port);
				else if (!sk.isConnected())
					sk.connect(new InetSocketAddress(Settings.lsp_host, Settings.lsp_port));
				OutputStream ow = sk.getOutputStream();
				ow.write(CONTENTLEN);
				ow.write(new StringBuilder().append(s.length).append("\r\n\r\n").toString().getBytes(StandardCharsets.UTF_8));
				ow.write(s);
				ow.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
