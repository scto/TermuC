package cn.rbc.termuc;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import android.net.*;
import cn.rbc.codeeditor.lang.*;
import cn.rbc.codeeditor.common.*;
import cn.rbc.codeeditor.util.*;
import cn.rbc.codeeditor.lang.c.*;
import cn.rbc.codeeditor.view.*;
import android.content.*;
import android.content.res.*;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class EditFragment extends Fragment
implements OnTextChangeListener, DialogInterface.OnClickListener, Formatter, Runnable
{
	public final static int TYPE_C = 1;
	public final static int TYPE_CPP = 2;
	public final static int TYPE_HEADER = 0x80000000;
	public final static int TYPE_OTHER = 0;
	public final static int TYPE_MASK = 0x7fffffff;
	final static String FL = "f", TP = "t", CS = "c", TS = "s";
	private File fl;
	private TextEditor ed;
	int type = -1;
	private String C = "clang";
	private long lastModified;
	private java.util.List<Range> changes = new java.util.ArrayList<>();

	public EditFragment() {
	}

	public EditFragment(String path, int type) {
		fl = new File(path);
		this.type = type;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final MainActivity ma = (MainActivity)getActivity();
		TextEditor editor = new TextEditor(ma);
		ed = editor;
		if (Settings.dark_mode)
			editor.setColorScheme(ColorSchemeDark.getInstance());
		DisplayMetrics dm = getResources().getDisplayMetrics();
		editor.setTextSize((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, Settings.textsize, dm));
		editor.setWordWrap(Settings.wordwrap);
		editor.setShowNonPrinting(Settings.whitespace);
		editor.setLayoutParams(new FrameLayout.LayoutParams(
								   FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		if (savedInstanceState!=null) {
			fl = new File((String)savedInstanceState.getCharSequence(FL));
			type = savedInstanceState.getInt(TP, type);
			editor.setDocument((Document)savedInstanceState.getCharSequence(CS));
			editor.setTextSize(savedInstanceState.getInt(TS));
		} else try {
			String s = load();
			int tp = type&TYPE_MASK;
			if (tp == TYPE_C) {
				C = "clang";
				editor.setLanguage(CLanguage.getInstance());
			} else if (tp == TYPE_CPP) {
				C = "clang++";
				editor.setLanguage(CppLanguage.getInstance());
			} else {
				editor.setLanguage(LanguageNonProg.getInstance());
			}
			ma.setEditor(editor);
			if (tp != TYPE_OTHER)
				MainActivity.lsp.didOpen(fl, tp==TYPE_CPP?"cpp":"c", s);
		} catch(IOException fnf) {
			fnf.printStackTrace();
			HelperUtils.show(Toast.makeText(ma, R.string.open_failed, Toast.LENGTH_SHORT));
		}
		if ((type&TYPE_MASK)!=TYPE_OTHER && "s".equals(Settings.completion))
			editor.setFormatter(this);
		lastModified = fl.lastModified();
		return editor;
	}

	public String getC() {
		return C;
	}

	@Override
	public int createAutoIndent(CharSequence text) {
		return 0;
	}

	@Override
	public CharSequence format(CharSequence txt, int width) {
		int start = ed.getSelectionStart(), end = ed.getSelectionEnd();
		if (start==end)
			MainActivity.lsp.formatting(fl, width, ed.isUseSpace());
		else {
			Range range = new Range();
			Document text = ed.getText();
			if (ed.isWordWrap()) {
				range.stl = text.findLineNumber(start);
				range.stc = text.getLineOffset(range.stl);
				range.enl = text.findLineNumber(end);
				range.enc = text.getLineOffset(range.enl);
			} else {
				range.stl = text.findRowNumber(start);
				range.stc = text.getRowOffset(range.stl);
				range.enl = text.findRowNumber(end);
				range.enc = text.getRowOffset(range.enl);
			}
			range.stc = start - range.stc;
			range.enc = end - range.enc;
			MainActivity.lsp.rangeFormatting(fl, range, width, ed.isUseSpace());
		}
		return null;
	}

	private int mVer;
	private long mSendTime;

	public void onChanged(CharSequence c, int start, int ver, boolean ins, boolean typ) {
		TextEditor editor = ed;
		Document text = editor.getText();
		boolean wordwrap = editor.isWordWrap();
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
		mVer = ver;
		mSendTime = System.currentTimeMillis();
		editor.postDelayed(this, 1000L);
		//}
	}

	public void run() {
		Lsp lsp = MainActivity.lsp;
		if (lsp.lastReceivedTime()<mSendTime) {
			lsp.didChange(fl, mVer, ed.getText().toString());
		}
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
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden) {
			MainActivity ma = (MainActivity)getActivity();
			ma.setEditor(ed);
			ma.setFileRunnable((type&TYPE_HEADER)==0);
			int tp = type&TYPE_MASK;
			if (tp == TYPE_C) {// C
				ed.setLanguage(CLanguage.getInstance());
				C = "clang";
			} else if (tp == TYPE_CPP) {
				ed.setLanguage(CppLanguage.getInstance());
				C = "clang++";
			} else {
				ed.setLanguage(LanguageNonProg.getInstance());
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putCharSequence(CS, ed.getText());
		outState.putCharSequence(FL, fl.getAbsolutePath());
		outState.putInt(TP, type);
		outState.putInt(TS, (int)ed.getTextSize());
	}

	public void save() throws IOException {
        FileWriter fileWriter = new FileWriter(fl);
        fileWriter.write(ed.getText().toString());
		fileWriter.flush();
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
		if ((type&TYPE_MASK)!=TYPE_OTHER)
			ed.getText().setOnTextChangeListener(this);
		return s;
	}

	public File getFile() {
		return fl;
	}
}
