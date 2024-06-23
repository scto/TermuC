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

public class Lsp {
	//final static String METHOD = "method";
	//final static String PARAMS = "params";
	final static int INITIALIZE = 0, INITIALIZED = 1,
	OPEN = 2, CLOSE = 3,
	COMPLETION = 4, FIX = 5, CHANGE = 6, SAVE = 7, NOTI = 8;
	private final static String TAG = "LSP";
	//private static int idx;
	private int tp;
	int id = 0;
	private Socket sk;
	private char[] compTrigs = {};
	//private Thread t;
	//private Context mA;

	public void start(final Context c, final Handler read) {
		Utils.run(c, "/system/bin/nc", new String[]{"-l", "-s", "127.0.0.1", "-p", "48455", "clangd", "--header-insertion-decorators=0"}, Environment.getExternalStorageDirectory().getAbsolutePath(), true);
		sk = new Socket();
		new Thread(){
			public void run() {
				try{
					int i=0;
					for (; !sk.isConnected() && i<20; i++) {
						Thread.sleep(100L);
						try {
							sk.connect(new InetSocketAddress("127.0.0.1", 48455));
						}catch(SocketException s){}
					}
					if (i==20)
						throw new Exception("Connection failed");
					InputStream is = sk.getInputStream();
					final int L = 2048;
					byte[] b = new byte[L];
					while (is.read(b, 0, 16) != -1) {
						if (new String(b, 0, 14).equals("Content-Length")) {
							int c = 0, d;
							while (Character.isDigit(d = is.read())) {
								c = c * 10 + d - 48;
							}
							is.skip(3L);
							StringBuilder sb = new StringBuilder();
							while (c > L) {
								int rl = is.read(b);
								c -= rl;
								String s = new String(b, 0, rl, StandardCharsets.UTF_8);
								sb.append(s);
							}
							for (d=0; d<c; d++)
								b[d] = (byte)is.read();
							String s = sb.append(new String(b,0,c, StandardCharsets.UTF_8)).toString();
							Message msg = new Message();
							msg.what = tp;
							msg.obj = s;
							read.sendMessage(msg);
							//tp = NOTI;
						}
					}
					is.close();
				} catch (Exception ioe) {
					Log.e(TAG, ioe.getMessage());
					// HelperUtils.show(Toast.makeText(c, ioe.getMessage(), 1));
				}
			}
		}.start();
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
		new SendThread("initialize", sb.toString(), true).start();
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
		new SendThread("initialized", new HashMap(), false).start();
	}

	public void didClose(File f) {
		HashMap<String,String> m = new HashMap<>();
		m.put("uri", Uri.fromFile(f).toString());
		HashMap<String,HashMap> k = new HashMap<>();
		k.put("textDocument", m);
		tp = CLOSE;
		new SendThread("textDocument/didClose", k, false).start();
	}

	public void didOpen(File f, String lang, String ct) {
		StringBuilder m = new StringBuilder("{\"textDocument\":{\"uri\":")
		.append(JSONObject.quote(Uri.fromFile(f).toString()))
		.append(",\"languageId\":\"").append(lang)
		.append("\",\"version\":0,\"text\":")
		.append(JSONObject.quote(ct))
		.append("}}");
		//tp = OPEN;
		new SendThread("textDocument/didOpen", m.toString(), false).start();
	}

	public void didSave(File f) {
		String s = new StringBuilder("{\"textDocument\":{\"uri\":")
		.append(JSONObject.quote(Uri.fromFile(f).toString()))
		.append("}}").toString();
		//tp = SAVE;
		new SendThread("textDocument/didSave", s, false).start();
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
		Thread td = new SendThread("textDocument/didChange", sb.toString(), false);
		td.start();
		try{
			td.join();
		}catch(InterruptedException ie){}
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
		new SendThread("textDocument/completion", sb.toString(), true).start();
		return true;
	}

	public boolean isConnected() {
		return sk.isConnected();
	}

	class SendThread extends Thread {
		private byte[] s;

		public SendThread(String cmd, Object hm, boolean req) {
			super();
			s = wrap(cmd, hm, req).getBytes(StandardCharsets.UTF_8);
		}

		public void run() {
			try {
				if (sk==null || sk.isClosed())
					sk = new Socket("127.0.0.1", 48455);
				else if (!sk.isConnected())
					sk.connect(new InetSocketAddress("127.0.0.1", 48455));
				OutputStream ow = sk.getOutputStream();
				ow.write(new StringBuilder("Content-Length: ").append(s.length).append("\r\n\r\n").toString().getBytes());
				ow.write(s);
				ow.flush();
			} catch(IOException e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}
}
