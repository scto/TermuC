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
import android.app.AlertDialog.Builder;
import java.util.ArrayList;
import java.util.List;

public class EditFragment extends Fragment
implements OnTextChangeListener, DialogInterface.OnClickListener, Formatter
{
	public final static int
	TYPE_C = 1,
	TYPE_CPP = 2,
	TYPE_HEADER = 4,
	TYPE_TXT = 0,
	TYPE_BLOD = 0x80000000,
	TYPE_MASK = 3;
	final static String FL = "f", TP = "t", CS = "c", TS = "s", MK = "m";
	private File fl;
	private TextEditor ed;
	int type = -1;
	private String C;
	private long lastModified;
	private List<Range> changes = new ArrayList<>();

	public EditFragment() {
	}

	public EditFragment(File path, int type) {
		fl = path;
		this.type = type;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final MainActivity ma = (MainActivity)getActivity();
		TextEditor editor = new TextEditor(ma);
		ed = editor;
		if (Application.dark_mode)
			editor.setColorScheme(ColorSchemeDark.getInstance());
		DisplayMetrics dm = getResources().getDisplayMetrics();
		editor.setTypeface(Application.typeface());
		editor.setTextSize((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, Application.textsize, dm));
		editor.setWordWrap(Application.wordwrap);
		editor.setShowNonPrinting(Application.whitespace);
		editor.setUseSpace(Application.usespace);
		editor.setTabSpaces(Application.tabsize);
		editor.setLayoutParams(new FrameLayout.LayoutParams(
								   FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		if (savedInstanceState!=null) {
			fl = new File((String)savedInstanceState.getCharSequence(FL));
			type = savedInstanceState.getInt(TP, type);
			Document doc = savedInstanceState.getParcelable(CS);
			doc.setMetrics(editor);
			doc.resetRowTable();
			editor.setDocument(doc);
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
				C = null;
				editor.setLanguage(LanguageNonProg.getInstance());
			}
			ma.setEditor(editor);
			if (tp != TYPE_TXT && "s".equals(Application.completion))
				MainActivity.lsp.didOpen(fl, tp==TYPE_CPP?"cpp":"c", s);
		} catch(IOException fnf) {
			fnf.printStackTrace();
			HelperUtils.show(Toast.makeText(ma, R.string.open_failed+fnf.getMessage(), Toast.LENGTH_SHORT));
		}
		if ((type&TYPE_MASK)!=TYPE_TXT) {
			if ("s".equals(Application.completion))
				editor.setFormatter(this);
			editor.setAutoComplete("l".equals(Application.completion));
		}
		lastModified = fl.lastModified();
		return editor;
	}

	public String getC() {
		return C;
	}

	@Override
	public int createAutoIndent(CharSequence text) {
		return 4;
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

	private int mVer = 0;

	public void onChanged(CharSequence c, int start, boolean ins, boolean typ) {
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
		Lsp lsp = MainActivity.lsp;
		lsp.didChange(fl, ++mVer, changes);
		// when inserting text and typing, call for completion
		if (ins && typ && c.length()==1)
			lsp.completionTry(fl, range.enl, range.enc+1, c.charAt(0));
		changes.clear();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (isVisible())
			refresh();
	}

	private void refresh() {
		long mLast = fl.lastModified();
		if (mLast>lastModified) {
			lastModified = mLast;
			Builder bd = new Builder(getContext());
			bd.setTitle(fl.getName());
			bd.setMessage(getString(R.string.file_modified, fl.getName()));
			bd.setPositiveButton(android.R.string.ok, this);
			bd.setNegativeButton(android.R.string.cancel, null);
			bd.create().show();
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
				C = null;
			}
			refresh();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(CS, ed.getText());
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
		if ((type&TYPE_MASK)!=TYPE_TXT && "s".equals(Application.completion))
			ed.getText().setOnTextChangeListener(this);
		return s;
	}

	public File getFile() {
		return fl;
	}

	public static int fileType(File pwd) {
		String _it = pwd.getName();
		int _tp;
		if (_it.endsWith(".c"))
			_tp = EditFragment.TYPE_C;
		else if (FileAdapter.isCpp(_it))
			_tp = EditFragment.TYPE_CPP;
		else if (_it.endsWith(".h"))
			_tp = EditFragment.TYPE_C | EditFragment.TYPE_HEADER;
		else if (_it.endsWith(".hpp"))
			_tp = EditFragment.TYPE_CPP | EditFragment.TYPE_HEADER;
		else if (!Utils.isBlob(pwd))
			_tp = EditFragment.TYPE_TXT | EditFragment.TYPE_HEADER;
		else
			_tp = EditFragment.TYPE_BLOD;
		return _tp;
	}
}
