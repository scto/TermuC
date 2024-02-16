package com.mrikso.texteditor;

import android.app.*;

import android.os.Bundle;
import android.widget.*;
import com.mrikso.codeeditor.util.*;
import android.view.*;
import java.io.*;
import android.os.*;
import android.view.ViewTreeObserver.*;
import android.view.inputmethod.*;
import java.lang.reflect.*;
import android.content.*;
import com.mrikso.codeeditor.lang.*;
import android.content.res.*;
import android.util.*;

public class MainActivity extends Activity {
	private static StrComp cmp = new StrComp();
    private static File root = Environment.getExternalStorageDirectory();
    private final static String PREF = "/data/data/com.termux/files";
    private final static String PREFC = "com.termux.RUN_COMMAND_";
    private ArrayAdapter<String> adp, hda;
	private FragmentManager mFmgr;
	private EditFragment lastFrag = null;
	private boolean byhand = true;
    private View keys, showlist;
    private File pwd;
    private TextView pwdpth, msgEmpty;
    private LinearLayout subc;
    private TextEditor textEditor;
	private Menu _appmenu;
	private ActionBar ab;
	private ActionBar.OnNavigationListener abnlisr = new ActionBar.OnNavigationListener() {
		public boolean onNavigationItemSelected(int p1, long p2) {
			if (byhand)
				showFrag(mFmgr.findFragmentByTag(hda.getItem(p1)));
			return false;
		}
	};
	private SearchAction mSearchAction;

	private void envInit() {
		pwd = new File(getPreferences(MODE_PRIVATE).getString("pwd", root.getPath()));
	}

	private void showFrag(Fragment frag) {
		if (frag == lastFrag)
			return;
		FragmentTransaction mTans = mFmgr.beginTransaction();
		if (lastFrag != null)
			mTans.hide(lastFrag);
		mTans.show(frag).commit();
		lastFrag = (EditFragment)frag;
		_appmenu.findItem(R.id.run).setEnabled(lastFrag.type<2);
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		envInit();
		getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			public void onGlobalLayout() {
				try {
					InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
					Method declaredMethod = inputMethodManager.getClass().getDeclaredMethod("getInputMethodWindowVisibleHeight", new Class[0]);
					declaredMethod.setAccessible(true);
					if (((Integer)declaredMethod.invoke(inputMethodManager, new Object[0])).intValue() > 0) {
						showlist.setVisibility(View.GONE);
						subc.setVisibility(View.GONE);
						keys.setVisibility(View.VISIBLE);
						return;
					}
					showlist.setVisibility(View.VISIBLE);
					keys.setVisibility(View.GONE);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}});
		mFmgr = getFragmentManager();
		hda = new ArrayAdapter<String>(new ContextThemeWrapper(getBaseContext(), android.R.style.Theme_Holo), R.layout.header_dropdown_item);
		Resources.Theme rt = getResources().newTheme();
		rt.applyStyle(android.R.style.Theme_Holo, true);
		hda.setDropDownViewTheme(rt);
		ab = getActionBar();
        ab.setListNavigationCallbacks(hda, abnlisr);
		setContentView(R.layout.activity_main);
        showlist = findViewById(R.id.show_list);
		keys = findViewById(R.id.keys);
		subc = findViewById(R.id.subcontainer);
		ListView l = findViewById(R.id.file_list);
		View hd = View.inflate(this, R.layout.list_header, null);
		pwdpth = hd.findViewById(R.id.pwd);
		msgEmpty = findViewById(R.id.msg_empty);
		l.addHeaderView(hd);
		adp = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		l.setAdapter(adp);
		l.setOnItemClickListener(new ListView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> av, View v, int i, long n) {
				String _it = adp.getItem(i-1);
				if ("..".equals(_it))
					pwd = pwd.getParentFile();
				else {
					pwd = new File(pwd, _it);
					if (pwd.isFile()) {
						_it = pwd.getAbsolutePath();
						int _i = hda.getCount();
						if (mFmgr.findFragmentByTag(_it)!=null) {
							for (_i--; -1<_i && !_it.equals(hda.getItem(_i)); _i--);
						} else {
							int _idx;
							if (_it.endsWith(".c"))
								_idx = EditFragment.TYPE_C;
							else if (_it.endsWith(".cpp"))
								_idx = EditFragment.TYPE_CPP;
							else if (_it.endsWith(".h"))
								_idx = EditFragment.TYPE_H;
							else if (_it.endsWith(".hpp"))
								_idx = EditFragment.TYPE_HPP;
							else
								_idx = _i = -1;
							if (_idx != -1) {
								EditFragment ef = new EditFragment(_it, _idx);
								FragmentTransaction mts = mFmgr.beginTransaction()
								.add(R.id.editFrag, ef, _it);
								if (lastFrag!=null)
									mts.hide(lastFrag);
								mts.show(ef)
								.commit();
								lastFrag = ef;
								hda.add(_it);
								byhand = false;
								if (_i==0) {
									getMenuInflater().inflate(R.menu.main, _appmenu);
									ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
									ab.setDisplayShowTitleEnabled(false);
									msgEmpty.setVisibility(View.GONE);
								}
								_appmenu.findItem(R.id.run).setEnabled(_idx<2);
							}
						}
						if (_i != -1) {
							ab.setSelectedNavigationItem(_i);
							byhand = true;
						}
						pwd = pwd.getParentFile();
						return;
					}
				}
				refresh();
			}
		});
		l.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v, final int i, long l) {
				PopupMenu pm = new PopupMenu(MainActivity.this, v);
				Menu _m = pm.getMenu();
				_m.add("删除").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(){
					public boolean onMenuItemClick(MenuItem mi) {
						if (new File(pwd, adp.getItem(i-1)).delete())
							adp.remove(adp.getItem(i-1));
						return true;
					}
				});
				pm.show();
				return true;
			}
		});
		refresh();
		mSearchAction = new SearchAction(this);
    }

	private void run() {
        Intent intent = new Intent();
        intent.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent.setAction("com.termux.RUN_COMMAND");
        intent.putExtra(new StringBuffer(PREFC).append("PATH").toString(), new StringBuffer(PREF).append("/usr/bin/bash").toString());
        intent.putExtra(new StringBuffer(PREFC).append("RUNNER").toString(), "app-shell");
        intent.putExtra(new StringBuffer(PREFC).append("ARGUMENTS").toString(), new String[]{"-c", new StringBuffer(lastFrag.getC()).append(" \"").append(escape(lastFrag.getPath())).append("\" -lm -Wall -o $TMPDIR/m && $TMPDIR/m && echo -n \"\nPress any key to continue...\" && read").toString()});
        intent.putExtra(new StringBuffer(PREFC).append("WORKDIR").toString(), pwd.getPath());
        intent.putExtra(new StringBuffer(PREFC).append("BACKGROUND").toString(), false);
        intent.putExtra(new StringBuffer(PREFC).append("SESSION_ACTION").toString(), "0");
        startService(intent);
    }

    private void refresh() {
		pwdpth.setText(pwd.getPath());
        adp.clear();
        String[] list = this.pwd.list();
        if (list != null) {
            if (!root.equals(this.pwd))
                adp.add("..");
            adp.addAll(list);
            adp.sort(cmp);
            adp.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.run:
                try {
                    save();
                    run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
				break;
			case R.id.undo:
                this.textEditor.undo();
                break;
            case R.id.redo:
                this.textEditor.redo();
                break;
            case R.id.save:
				try {
					save();
				} catch(IOException e) {
					e.printStackTrace();
				}
				break;
			case R.id.search:
				startActionMode(mSearchAction);
				break;
			case R.id.close:
				int sd = ab.getSelectedNavigationIndex();
				String _t = hda.getItem(ab.getSelectedNavigationIndex());
				hda.remove(_t);
				FragmentTransaction mTans = mFmgr.beginTransaction();
				mTans.remove(mFmgr.findFragmentByTag(_t));
				int cnt = hda.getCount();
				if (hda.getCount() == 0) {
					ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
					ab.setDisplayShowTitleEnabled(true);
					_appmenu.clear();
					msgEmpty.setVisibility(View.VISIBLE);
				} else {
					if (cnt == sd)
						sd--;
					lastFrag = (EditFragment)mFmgr.findFragmentByTag(hda.getItem(sd));
					mTans.show(lastFrag);
				}
				mTans.commit();
				break;
        }
        return true;
    }

    public void inputKey(View view) {
        String charSequence = ((TextView) view).getText().toString();
        if ("⇥".equals(charSequence))
            charSequence = "\t";
        textEditor.insert(textEditor.getCaretPosition(), charSequence);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (i != keyEvent.KEYCODE_BACK || subc.getVisibility() != View.VISIBLE) {
            return super.onKeyUp(i, keyEvent);
        }
        subc.setVisibility(View.GONE);
        return false;
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        textEditor.setDocument((Document) bundle.getCharSequence("code"));
        pwd = new File(bundle.getString("pwd"));
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putCharSequence("code", textEditor.getText());
        bundle.putString("pwd", pwd.getPath());
        super.onSaveInstanceState(bundle);
    }

    public void createFile(View view) {
        View inflate = View.inflate(this, R.layout.edit, null);
        final EditText e = inflate.findViewById(R.id.file_name);
		new AlertDialog.Builder(this)
		.setTitle("新建")
		.setView(inflate)
		.setPositiveButton("文件", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface di, int p) {
				try {
					new File(pwd, e.getText().toString()).createNewFile();
					refresh();
				} catch (Exception e) {
					e.printStackTrace();
					toast(e.getMessage());
				}
			}
		})
		.setNeutralButton("文件夹", new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int p) {
					try {
						new File(pwd, e.getText().toString()).mkdir();
						refresh();
					} catch (Exception e) {
						e.printStackTrace();
						toast(e.getMessage());
					}
				}
			})
		.setNegativeButton(android.R.string.cancel, null)
		.create().show();
    }

    public void save() throws IOException {
        FileWriter fileWriter = new FileWriter(lastFrag.getPath());
        fileWriter.write(textEditor.getText().toString());
        fileWriter.close();
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		_appmenu = menu;
		return super.onPrepareOptionsMenu(menu);
	}

    @Override
    protected void onStop() {
        getPreferences(MODE_PRIVATE).edit().putString("pwd", pwd.getPath()).commit();
        super.onStop();
    }

    private String escape(String str) {
        return str.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$").replace("\"", "\\\"");
    }

    public void toast(CharSequence charSequence) {
        Toast.makeText(this, charSequence, 0).show();
    }

    public void showList(View view) {
        subc.setVisibility(View.VISIBLE);
    }

	public void setEditor(TextEditor edit) {
		textEditor = edit;
	}

	public TextEditor getEditor() {
		return textEditor;
	}
}
