package cn.rbc.termuc;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import android.net.*;
import java.util.*;
import cn.rbc.codeeditor.lang.*;
import cn.rbc.codeeditor.common.*;
import cn.rbc.codeeditor.util.*;
import cn.rbc.codeeditor.lang.c.*;
import cn.rbc.codeeditor.view.*;
import android.content.*;

public class EditFragment extends Fragment
implements OnTextChangeListener, DialogInterface.OnClickListener
{
	public final static int TYPE_C = 0;
	public final static int TYPE_CPP = 1;
	public final static int TYPE_H = 2;
	public final static int TYPE_HPP = 3;
	final static String FL = "f", TP = "t";
	private File fl;
	private TextEditor ed;
	int type;
	private String C = "clang";
	private long lastModified;
	private List<Range> changes = new ArrayList<>();

	public EditFragment() {
	}

	public EditFragment(String path, int type) {
		Bundle bd = new Bundle();
		bd.putString(FL, path);
		bd.putInt(TP, type);
		setArguments(bd);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		setRetainInstance(true);
		Bundle arg = getArguments();
		if (arg == null)
			return null;
		fl = new File(arg.getString(FL));
		lastModified = fl.lastModified();
		type = arg.getInt(TP);
		final MainActivity ma = (MainActivity)getActivity();
		TextEditor editor = new TextEditor(ma);
		ed = editor;
		editor.setVerticalScrollBarEnabled(true);
		if (Settings.dark_mode)
			editor.setColorScheme(ColorSchemeDark.getInstance());
		editor.setWordWrap(Settings.wordwrap);
		editor.setShowNonPrinting(Settings.whitespace);
		editor.setLayoutParams(new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		try {
			String s = load();
			if ((type&1) == 0) {
				C = "clang";
				editor.setLanguage(CLanguage.getInstance());
			} else {
				C = "clang++";
				editor.setLanguage(CppLanguage.getInstance());
			}
			ma.setEditor(editor);
			MainActivity.lsp.didOpen(fl, (type&1)==1?"cpp":"c", s);
		} catch(IOException fnf) {
			fnf.printStackTrace();
			Toast.makeText(ma, "打开失败！", Toast.LENGTH_SHORT).show();
		}
		return editor;
	}

	public String getC() {
		return C;
	}

	//private int lastVer = -1;

	public void onChanged(CharSequence c, int start, final int ver, boolean ins, boolean typ) {
		Document text = ed.getText();
		boolean wordwrap = ed.isWordWrap();
		Range range = new Range();
		if (wordwrap) {
			range.stl = text.findLineNumber(start);
			range.stc = text.getLineOffset(range.stl);
		} else {
			range.stl = text.findRowNumber(start);
			range.stc = text.getRowOffset(range.stl);
		}
		range.stc = start - range.stc;
		if (ins) { // insert
			range.enl = range.stl;
			range.enc = range.stc;
		} else { // delete
			int e = start + c.length();
			c = "";
			if (wordwrap) {
				range.enl = text.findLineNumber(e);
				range.enc = text.getLineOffset(range.enl);
			} else {
				range.enl = text.findRowNumber(e);
				range.enc = text.getRowOffset(range.enl);
			}
			range.enc = e - range.enc;
		}
		range.msg = (String)c;
		changes.add(range);
		//lastStr = (String)c;
		//if (lastVer != ver) {
		Lsp lsp = MainActivity.lsp;
		lsp.didChange(fl, ver, changes);
		//lastStart = s;
		//lastVer = ver;
		// when inserting text and typing, call for completion
		if (ins && typ && c.length()==1)
			lsp.completionTry(fl, range.enl, range.enc+1, c.charAt(0));
		changes.clear();
		ed.postDelayed(new Runnable(){
			long sendTime = System.currentTimeMillis();
			public void run() {
				Lsp lsp = MainActivity.lsp;
				if (lsp.lastReceivedTime()<sendTime) {
					lsp.didChange(fl, ver, ed.getText().toString());
				}
			}
		}, 1000L);
		//}
	}

	@Override
	public void onResume() {
		super.onResume();
		long mLast = fl.lastModified();
		if (mLast>lastModified) {
			lastModified = mLast;
			new AlertDialog.Builder(getContext())
			.setTitle(fl.getName())
			.setMessage(getString(R.string.file_modified, fl.getName()))
			.setPositiveButton(android.R.string.ok, this)
			.setNegativeButton(android.R.string.cancel, null)
			.create().show();
		}
	}

	@Override
	public void onClick(DialogInterface diag, int id) {
		try {
			String s = load();
			((MainActivity)getActivity()).lsp.didChange(fl, 0, s);
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@Override
	public void onHiddenChanged(boolean hidden)
	{
		super.onHiddenChanged(hidden);
		if (!hidden) {
			((MainActivity)getActivity()).setEditor(ed);
			if ((type&1) == 0) {// C
				ed.setLanguage(CLanguage.getInstance());
				C = "clang";
			} else {
				ed.setLanguage(CppLanguage.getInstance());
				C = "clang++";
			}
		}
	}

	public void save() throws IOException {
        FileWriter fileWriter = new FileWriter(fl);
        fileWriter.write(ed.getText().toString());
        fileWriter.close();
		lastModified = fl.lastModified();
    }

	public String load() throws IOException {
		FileReader fr = new FileReader(fl);
		char[] buf = new char[1024];
		int i;
		StringBuilder sb = new StringBuilder();
		while ((i=fr.read(buf))!=-1)
			sb.append(buf, 0, i);
		fr.close();
		String s = sb.toString();
		ed.setText(s);
		ed.getText().setOnTextChangeListener(this);
		return s;
	}

	public File getFile() {
		return fl;
	}
}
