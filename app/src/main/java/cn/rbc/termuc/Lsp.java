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
		s.append(",\"method\":\"");s.append(m);
		s.append("\",\"params\":");s.append(JSONObject.wrap(p));
		s.append("}");
		return s.toString();
	}

	public void initialize() {
		tp = INITIALIZE;
		StringBuilder sb = new StringBuilder("{\"processId\":");
		sb.append(android.os.Process.myPid());
		sb.append(",\"initializationOptions\":{\"fallbackFlags\":[\"-Wall\"]}}");
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
		StringBuilder m = new StringBuilder("{\"textDocument\":{\"uri\":");
		m.append(JSONObject.quote(Uri.fromFile(f).toString()));
		m.append(",\"languageId\":\"");m.append(lang);
		m.append("\",\"version\":0,\"text\":");
		m.append(JSONObject.quote(ct));
		m.append("}}");
		//tp = OPEN;
		mExecutor.execute(new Send("textDocument/didOpen", m.toString(), false));
	}

	public void didSave(File f) {
		StringBuilder s = new StringBuilder("{\"textDocument\":{\"uri\":");
		s.append(JSONObject.quote(Uri.fromFile(f).toString()));
		s.append("}}");
		//tp = SAVE;
		mExecutor.execute(new Send("textDocument/didSave", s.toString(), false));
	}

	public synchronized void didChange(File f, int version, String text) {
		StringBuilder sb = new StringBuilder("{\"textDocument\":{\"uri\":");
		sb.append(JSONObject.quote(Uri.fromFile(f).toString()));
		sb.append(",\"version\":");
		sb.append(version);
		sb.append("},\"contentChanges\":[{\"text\":");
		sb.append(JSONObject.quote(text));
		sb.append("}]}").toString();
		mExecutor.execute(new Send("textDocument/didChange", sb.toString(), false));
	}

	public synchronized void didChange(File f, int version, List<Range> chs) {
		StringBuilder sb = new StringBuilder("{\"textDocument\":{\"uri\":");
		sb.append(JSONObject.quote(Uri.fromFile(f).toString()));
		sb.append(",\"version\":");
		sb.append(version);
		sb.append("},\"contentChanges\":[");
		for (int i=0,j=chs.size(); i<j; i++) {
			Range c = chs.get(i);
			sb.append("{\"range\":{\"start\":{\"line\":");
			sb.append(c.stl);
			sb.append(",\"character\":");
			sb.append(c.stc);
			sb.append("},\"end\":{\"line\":");
			sb.append(c.enl);
			sb.append(",\"character\":");
			sb.append(c.enc);
			sb.append("}},\"text\":");
			sb.append(JSONObject.quote(c.msg));
			sb.append("},");
		}
		sb.setCharAt(sb.length()-1, ']');
		sb.append('}');
		//tp = CHANGE;
		mExecutor.execute(new Send("textDocument/didChange", sb.toString(), false));
	}

	public synchronized boolean completionTry(File f, int l, int c, char tgc) {
		byte b = isCompTrig(tgc);
		if (b==0)
			return false;
		StringBuilder sb = new StringBuilder("{\"textDocument\":{\"uri\":");
		sb.append(JSONObject.quote(Uri.fromFile(f).toString()));
		sb.append("},\"position\":{\"line\":");
		sb.append(l);
		sb.append(",\"character\":");
		sb.append(c);
		sb.append("},\"context\":{\"triggerKind\":");
		sb.append(b);
		if (b==2) {
			sb.append(",\"triggerCharacter\":\"");
			sb.append(tgc);
			sb.append('"');
		}
		sb.append("}}");
		tp = COMPLETION;
		//Log.d(TAG, sb.toString());
		mExecutor.execute(new Send("textDocument/completion", sb.toString(), true));
		return true;
	}

	public synchronized void formatting(File fl, int tabSize, boolean useSpace) {
		StringBuilder sb = new StringBuilder("{\"textDocument\":{\"uri\":");
		sb.append(JSONObject.quote(Uri.fromFile(fl).toString()));
		sb.append("},\"options\":{\"tabSize\":");
		sb.append(tabSize);
		sb.append(",\"insertSpaces\":");
		sb.append(useSpace);
		sb.append("}}");
		//Log.d(TAG, sb.toString());
		mExecutor.execute(new Send("textDocument/formatting", sb.toString(), true));
	}

	public synchronized void rangeFormatting(File fl, Range range, int tabSize, boolean useSpace) {
		StringBuilder sb = new StringBuilder("{\"textDocument\":{\"uri\":");
		sb.append(JSONObject.quote(Uri.fromFile(fl).toString()));
		sb.append("},\"range\":{\"start\":{\"line\":");
		sb.append(range.stl);
		sb.append(",\"character\":");
		sb.append(range.stc);
		sb.append("},\"end\":{\"line\":");
		sb.append(range.enl);
		sb.append(",\"character\":");
		sb.append(range.enc);
		sb.append("}},\"options\":{\"tabSize\":");
		sb.append(tabSize);
		sb.append(",\"insertSpaces\":");
		sb.append(useSpace);
		sb.append("}}");
		//Log.d(TAG, sb.toString());
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
				ow.write((s.length+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
				ow.write(s);
				ow.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
