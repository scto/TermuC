package cn.rbc.termuc;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.json.*;
import android.net.*;
import java.util.*;
/*import tiiehenry.code.view.*;
import tiiehenry.code.language.c.*;
import tiiehenry.code.language.cpp.*;
import tiiehenry.code.language.java.*;*/
import cn.rbc.codeeditor.lang.*;
import cn.rbc.codeeditor.common.*;
import cn.rbc.codeeditor.util.*;
import android.util.*;

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
	//private int id;
	//private int _ed;
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
			//sb.trimToSize();
			String s = sb.toString();
			editor.setText(s);
			editor.getText().setOnTextChangeListener(this);
			if ((type&1) == 0) {
				C = "gcc";
				editor.setLanguage(LanguageC.getInstance());
			} else {
				C = "g++";
				editor.setLanguage(LanguageCpp.getInstance());
			}
			//editor.setLanguage(.getInstance());
			ma.setEditor(editor);
			ma.lsp.didOpen(fl, (type&1)==1?"cpp":"c", s);
		} catch(Exception fnf) {
			fnf.printStackTrace();
			Toast.makeText(ma, "打开失败！", Toast.LENGTH_SHORT).show();
		}
		return ed = editor;
	}

	public String getC() {
		return C;
	}

	private int lastVer = -1;
	private ArrayList<Quart> cgs;

	public void onChanged(CharSequence c, int s, int ver, boolean ins, boolean typ) {
		Document dp = ed.getText();
		boolean b = ed.isWordWrap();
		Quart q = new Quart();
		if (b) {
			q.sr = dp.findLineNumber(s);
			q.sc = dp.getLineOffset(q.sr);
		} else {
			q.sr = dp.findRowNumber(s);
			q.sc = dp.getRowOffset(q.sr);
		}
		q.sc = s - q.sc;
		if (ins) { // i
			q.er = q.sr;
			q.ec = q.sc;
		} else { // d
			int e = s + c.length();
			c = "";
			if (b) {
				q.er = dp.findLineNumber(e);
				q.ec = dp.getLineOffset(q.er);
			} else {
				q.er = dp.findRowNumber(e);
				q.ec = dp.getRowOffset(q.er);
			}
			q.ec = e - q.ec;
		}
		q.tx = (String)c;
		cgs.add(q);
		//lastStr = (String)c;
		//if (lastVer != ver) {
		Lsp l = ((MainActivity)getActivity()).lsp;
		l.didChange(fl, ver, cgs);
		//lastStart = s;
		//lastVer = ver;
		if (ins && typ && (s=c.length())==1 && l.completionTry(fl, q.er, q.ec+1, c.charAt(0)))
			ed.getAutoCompletePanel().dismiss();
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
			if ((type&1) == 0) { // C
				ed.setLanguage(LanguageC.getInstance());
			} else {
				ed.setLanguage(LanguageCpp.getInstance());
			}
			//_tmp = 0;
		}
	}

	public File getFile() {
		return fl;
	}
}
