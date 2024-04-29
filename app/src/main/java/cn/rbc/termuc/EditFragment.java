package cn.rbc.termuc;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.json.*;
import android.net.*;
import java.util.*;
import cn.rbc.codeeditor.lang.*;
import cn.rbc.codeeditor.common.*;
import cn.rbc.codeeditor.util.*;
import cn.rbc.codeeditor.lang.c.*;

public class EditFragment extends Fragment implements OnTextChangeListener
{
	public final static int TYPE_C = 0;
	public final static int TYPE_CPP = 1;
	public final static int TYPE_H = 2;
	public final static int TYPE_HPP = 3;
	final static String FL = "f", TP = "t";
	private File fl;
	private TextEditor ed;
	int type;
	private String C = "gcc";

	public EditFragment() {
		cgs = new ArrayList<>();
	}

	public EditFragment(String path, int type) {
		cgs = new ArrayList<>();
		Bundle bd = new Bundle();
		bd.putString(FL, path);
		bd.putInt(TP, type);
		setArguments(bd);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		setRetainInstance(true);
		Bundle arg = getArguments();
		if (arg == null)
			return null;
		fl = new File(arg.getString(FL));
		type = arg.getInt(TP);
		final MainActivity ma = (MainActivity)getActivity();
		TextEditor editor = new TextEditor(ma);
		editor.setVerticalScrollBarEnabled(true);
		editor.setLayoutParams(new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		try {
			FileReader fr = new FileReader(fl);
			char[] cs = new char[1024];
			int i;
			StringBuilder sb = new StringBuilder();
			while ((i=fr.read(cs)) != -1)
				sb.append(cs, 0, i);
			fr.close();
			String s = sb.toString();
			editor.setText(s);
			editor.getText().setOnTextChangeListener(this);
			if ((type&1) == 0) {
				C = "gcc";
				editor.setLanguage(CLanguage.getInstance());
			} else {
				C = "g++";
				editor.setLanguage(CppLanguage.getInstance());
			}
			ma.setEditor(editor);
			ma.lsp.didOpen(fl, (type&1)==1?"cpp":"c", s);
		} catch(IOException fnf) {
			fnf.printStackTrace();
			Toast.makeText(ma, "打开失败！", Toast.LENGTH_SHORT).show();
		}
		return ed = editor;
	}

	public String getC() {
		return C;
	}

	//private int lastVer = -1;
	private ArrayList<Range> cgs;

	public void onChanged(CharSequence c, int start, int ver, boolean ins, boolean typ) {
		Document dp = ed.getText();
		boolean wordwrap = ed.isWordWrap();
		Range range = new Range();
		if (wordwrap) {
			range.stl = dp.findLineNumber(start);
			range.stc = dp.getLineOffset(range.stl);
		} else {
			range.stl = dp.findRowNumber(start);
			range.stc = dp.getRowOffset(range.stl);
		}
		range.stc = start - range.stc;
		if (ins) { // insert
			range.enl = range.stl;
			range.enc = range.stc;
		} else { // delete
			int e = start + c.length();
			c = "";
			if (wordwrap) {
				range.enl = dp.findLineNumber(e);
				range.enc = dp.getLineOffset(range.enl);
			} else {
				range.enl = dp.findRowNumber(e);
				range.enc = dp.getRowOffset(range.enl);
			}
			range.enc = e - range.enc;
		}
		range.msg = (String)c;
		cgs.add(range);
		//lastStr = (String)c;
		//if (lastVer != ver) {
		Lsp l = ((MainActivity)getActivity()).lsp;
		l.didChange(fl, ver, cgs);
		//lastStart = s;
		//lastVer = ver;
		// when inserting text and typing, call for completion
		if (ins && typ && c.length()==1) l.completionTry(fl, range.enl, range.enl+1, c.charAt(0));
			//ed.getAutoCompletePanel().dismiss();
		cgs.clear();
		//}
	}

	@Override
	public void onHiddenChanged(boolean hidden)
	{
		// TODO: Implement this method
		super.onHiddenChanged(hidden);
		if (!hidden) {
			((MainActivity)getActivity()).setEditor(ed);
			if ((type&1) == 0) // C
				ed.setLanguage(CLanguage.getInstance());
			else
				ed.setLanguage(CppLanguage.getInstance());
		}
	}

	public File getFile() {
		return fl;
	}
}
