package com.mrikso.texteditor;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import com.mrikso.codeeditor.lang.*;

public class EditFragment extends Fragment
{
	public final static int TYPE_C = 0;
	public final static int TYPE_CPP = 1;
	public final static int TYPE_H = 2;
	public final static int TYPE_HPP = 3;
	private String path;
	int type;
	private String C = "gcc";

	public EditFragment(String path, int type) {
		this.path = path;
		this.type = type;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		MainActivity ma = (MainActivity)getActivity();
		TextEditor editor = new TextEditor(ma);
		editor.setLayoutParams(new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		try {
			FileInputStream fr = new FileInputStream(path);
			byte[] cs = new byte[fr.available()];
			fr.read(cs);
			fr.close();
			editor.setText(new String(cs));
			if ((type&1) == 0) {
				C = "gcc";
				editor.setLanguage(LanguageC.getInstance());
			} else {
				C = "g++";
				editor.setLanguage(LanguageCpp.getInstance());
			}
			ma.setEditor(editor);
		} catch(Exception fnf) {
			fnf.printStackTrace();
			Toast.makeText(ma, "Open failed!", Toast.LENGTH_SHORT).show();
		}
		return editor;
	}

	public String getC() {
		return C;
	}

	@Override
	public void onHiddenChanged(boolean hidden)
	{
		// TODO: Implement this method
		super.onHiddenChanged(hidden);
		if (!hidden) {
			((MainActivity)getActivity()).setEditor((TextEditor)getView());
		}
	}

	public String getPath() {
		return path;
	}
}
