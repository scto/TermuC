package cn.rbc.termuc;

import android.app.*;

import android.widget.*;
import android.view.*;
import java.io.*;
import android.os.*;
import android.view.ViewTreeObserver.*;
import android.view.inputmethod.*;
import java.lang.reflect.*;
import android.content.*;
import android.content.res.*;
import android.util.Log;
import java.util.*;
import org.json.*;
import java.net.*;
import android.net.*;
import cn.rbc.codeeditor.util.*;
import android.preference.*;
import android.content.pm.*;

public class MainActivity extends Activity implements
	ActionBar.OnNavigationListener, OnGlobalLayoutListener,
	AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
	DialogInterface.OnClickListener, MenuItem.OnMenuItemClickListener, Runnable {

	public final static int SETTING = 0;
	private final static String PWD = "p", SHOWLIST = "l", FILES = "o";
    private ArrayAdapter<String> hda;
	private FileAdapter adp;
	private EditFragment lastFrag = null;
	private boolean byhand = true, inited = false;
    private View keys, showlist;
    private File pwd;
    private TextView pwdpth, msgEmpty, transTxV;
    private LinearLayout subc;
    private TextEditor codeEditor;
	private Menu _appmenu;
	private ActionBar ab;
	private SearchAction mSearchAction;
	private String transStr;
	private Dialog transDlg;
	private static MainHandler hand;
	static Lsp lsp;

	private void envInit() {
		Settings.getInstance(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
		pwd = new File(getPreferences(MODE_PRIVATE).getString(PWD, Utils.ROOT.getPath()));
		if (lsp==null) {
			hand = new MainHandler(this);
			lsp = new Lsp();
			lsp.start(this, hand);
		} else
			hand.updateActivity(this);
	}

	private void showFrag(Fragment frag) {
		if (frag == lastFrag)
			return;
		FragmentTransaction mTans = getFragmentManager().beginTransaction();
		if (lastFrag != null)
			mTans.hide(lastFrag);
		mTans.show(frag).commit();
		lastFrag = (EditFragment)frag;
	}

	public boolean onNavigationItemSelected(int p1, long p2) {
		if (byhand)
			showFrag(getFragmentManager().findFragmentByTag(hda.getItem(p1)));
		return false;
	}

	String getTag(int idx) {
		return hda.getItem(idx);
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		envInit();
		if (Settings.dark_mode)
			setTheme(android.R.style.Theme_Holo);
        super.onCreate(savedInstanceState);
		getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);
		hda = new HeaderAdapter(new ContextThemeWrapper(getBaseContext(), android.R.style.Theme_Holo), R.layout.header_dropdown_item);
		Resources.Theme rt = getResources().newTheme();
		rt.applyStyle(android.R.style.Theme_Holo, true);
		hda.setDropDownViewTheme(rt);
		ab = getActionBar();
		// ab.setHomeButtonEnabled(true);
        ab.setListNavigationCallbacks(hda, this);
		setContentView(R.layout.activity_main);
        showlist = findViewById(R.id.show_list);
		keys = findViewById(R.id.keys);
		subc = findViewById(R.id.subcontainer);
		ListView l = findViewById(R.id.file_list);
		View hd = View.inflate(this, R.layout.list_header, null);
		pwdpth = hd.findViewById(R.id.pwd);
		msgEmpty = findViewById(R.id.msg_empty);
		l.addHeaderView(hd);
		adp = new FileAdapter(this, pwd);
		l.setAdapter(adp);
		l.setOnItemClickListener(this);
		l.setOnItemLongClickListener(this);
		mSearchAction = new SearchAction(this);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
			requestPermissions(new String[]{
				android.Manifest.permission.READ_EXTERNAL_STORAGE,
				android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
				android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
				}, PackageManager.PERMISSION_GRANTED);
    }

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode==PackageManager.PERMISSION_GRANTED
			&& grantResults[0]==requestCode
			&& grantResults[1]==requestCode
			&& grantResults[2]==requestCode)
			refresh();
	}

    private void refresh() {
		pwdpth.setText(pwd.getPath());
		adp.setPath(pwd);
        adp.notifyDataSetChanged();
    }

	@Override
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
	}

	public void onItemClick(AdapterView<?> av, View v, int i, long n) {
		String _it = adp.getItem(i-1).name;
		if ("..".equals(_it))
			pwd = pwd.getParentFile();
		else {
			pwd = new File(pwd, _it);
			if (pwd.isFile()) {
				_it = pwd.getAbsolutePath();
				int _i = hda.getCount(), _tp = -1;
				if (getFragmentManager().findFragmentByTag(_it)!=null) {
					for (_i--; -1<_i && !_it.equals(hda.getItem(_i)); _i--);
				} else {
					if (_it.endsWith(".c"))
						_tp = EditFragment.TYPE_C;
					else if (_it.endsWith(".cpp")||_it.endsWith(".cxx"))
						_tp = EditFragment.TYPE_CPP;
					else if (_it.endsWith(".h"))
						_tp = EditFragment.TYPE_C|EditFragment.TYPE_HEADER;
					else if (_it.endsWith(".hpp"))
						_tp = EditFragment.TYPE_CPP|EditFragment.TYPE_HEADER;
					else if (!Utils.isBlob(pwd))
						_tp = EditFragment.TYPE_OTHER|EditFragment.TYPE_HEADER;
					else
						_i = -1;
					if (_tp != -1) {
						if (!inited) {
							lsp.initialize();
							inited = true;
						}
						EditFragment ef = new EditFragment(pwd.getPath(), _tp);
						FragmentTransaction mts = getFragmentManager().beginTransaction()
							.add(R.id.editFrag, ef, _it);
						if (lastFrag!=null)
							mts.hide(lastFrag);
						mts.show(ef)
							.commit();
						lastFrag = ef;
						hda.add(_it);
						byhand = false;
						if (_i==0) {
							_appmenu.clear();
							getMenuInflater().inflate(R.menu.main, _appmenu);
							ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
							ab.setDisplayShowTitleEnabled(false);
							msgEmpty.setVisibility(View.GONE);
						}
						setFileRunnable(((_tp&EditFragment.TYPE_HEADER)==0));
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

	public boolean onItemLongClick(AdapterView<?> av, View v, final int i, long l) {
		if (i==0||"..".equals(adp.getItem(i-1).name))
			return false;
		PopupMenu pm = new PopupMenu(MainActivity.this, v);
		Menu _m = pm.getMenu();
		transStr = adp.getItem(i-1).name;
		_m.add(R.string.delete).setOnMenuItemClickListener(this);
		pm.show();
		return true;
	}

	@Override
	public boolean onMenuItemClick(MenuItem p1) {
		new AlertDialog.Builder(this)
			.setTitle(R.string.delete)
			.setMessage(getString(R.string.confirm_delete, transStr))
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, this)
			.create().show();
		return true;
	}

	void setFileRunnable(boolean exec) {
		_appmenu.findItem(R.id.run).setVisible(exec);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.run:
                try {
                    lastFrag.save();
					if ((lastFrag.type&EditFragment.TYPE_MASK)!=EditFragment.TYPE_OTHER)
						lsp.didSave(lastFrag.getFile());
                    Utils.run(this, new StringBuilder(Utils.PREF).append("/usr/bin/bash").toString(), new String[]{"-c",
					new StringBuffer(lastFrag.getC())
					.append(" \"").append(Utils.escape(lastFrag.getFile().getAbsolutePath())).append("\" ")
					.append(Settings.cflags).append(" -o $TMPDIR/m && $TMPDIR/m && echo -n \"\nPress any key to exit...\" && read").toString()},
					pwd.getPath(), false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
				break;
			case R.id.undo:
                codeEditor.undo();
                break;
            case R.id.redo:
                codeEditor.redo();
                break;
            case R.id.save:
				try {
					lastFrag.save();
					if ((lastFrag.type&EditFragment.TYPE_MASK)!=EditFragment.TYPE_OTHER)
						lsp.didSave(lastFrag.getFile());
					toast(getText(R.string.saved));
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
				FragmentManager fm = getFragmentManager();
				FragmentTransaction mTans = fm.beginTransaction();
				mTans.remove(fm.findFragmentByTag(_t));
				int cnt = hda.getCount();
				if (hda.getCount() == 0) {
					ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
					ab.setDisplayShowTitleEnabled(true);
					_appmenu.clear();
					getMenuInflater().inflate(R.menu.nopen, _appmenu);
					msgEmpty.setVisibility(View.VISIBLE);
				} else {
					if (cnt == sd)
						sd--;
					lastFrag = (EditFragment)fm.findFragmentByTag(hda.getItem(sd));
					mTans.show(lastFrag);
				}
				mTans.commit();
				lsp.didClose(pwd);
				break;
			case R.id.settings:
				Intent it = new Intent(this, SettingsActivity.class);
				startActivityForResult(it, SETTING);
				break;
        }
        return true;
    }

    public void inputKey(View view) {
        String charSequence = ((TextView) view).getText().toString();
        if ("⇥".equals(charSequence)) {
			if (codeEditor.isUseSpace()) {
				int tabLen = codeEditor.getTabSpaces();
				int pos = codeEditor.getCaretPosition();
				Document doc = codeEditor.getText();
				int l = doc.findLineNumber(pos);
				int of = doc.getLineOffset(l);
				char[] cs = new char[tabLen-(pos-of)%tabLen];
				Arrays.fill(cs, ' ');
				charSequence = new String(cs);
			} else
            	charSequence = "\t";
		}
		codeEditor.getText().setTyping(true);
        codeEditor.paste(charSequence);
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
        pwd = new File(bundle.getString(PWD));
		int i = 0, j = 0;
		List<String> files = bundle.getStringArrayList(FILES);
		if (files != null) {
			FragmentManager fm = getFragmentManager();
			for (String s:bundle.getStringArrayList(FILES)) {
				hda.add(s);
				Fragment f = fm.findFragmentByTag(s);
				if (!f.isHidden()) {
					j = i;
					codeEditor = (TextEditor)f.getView();
				}
				i++;
			}
		if (!hda.isEmpty()) {
			_appmenu.clear();
			getMenuInflater().inflate(R.menu.main, _appmenu);
			ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			ab.setDisplayShowTitleEnabled(false);
			msgEmpty.setVisibility(View.GONE);
			byhand = false;
			ab.setSelectedNavigationItem(j);
			byhand = true;
		}
		}
		if (subc!=null)
			subc.setVisibility(bundle.getInt(SHOWLIST));
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString(PWD, pwd.getPath());
		bundle.putInt(SHOWLIST, subc.getVisibility());
		ArrayList<String> al = new ArrayList<>(hda.getCount());
		for (int i=0,l=hda.getCount();i<l;i++)
			al.add(hda.getItem(i));
		bundle.putStringArrayList(FILES, al);
        super.onSaveInstanceState(bundle);
    }

	@Override
	public void onClick(DialogInterface di, int id) {
		ProgressDialog pd = new ProgressDialog(MainActivity.this);
		pd.setMessage(getString(R.string.deleting, transStr));
		pd.setIndeterminate(true);
		pd.show();
		transDlg = pd;
		new Thread(this).start();
	}

	public void run() {
		final boolean ok = Utils.removeFiles(new File(pwd, transStr));
		runOnUiThread(new Runnable(){
				public void run(){
					if (ok) {
						toast(getText(R.string.deleted));
						refresh();
					}
					transDlg.dismiss();
				}
			});
	}

	@Override
	protected void onResume() {
		super.onResume();
		refresh();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==SETTING && resultCode==RESULT_OK) {
			FragmentManager fm = getFragmentManager();
			for (int i=getActionBar().getNavigationItemCount()-1;i>=0;i--) {
				Fragment f = fm.findFragmentByTag(hda.getItem(i));
				TextEditor ed = (TextEditor)f.getView();
				ed.setFormatter("s".equals(Settings.completion)?(EditFragment)f:null);
				ed.setWordWrap(Settings.wordwrap);
				ed.setShowNonPrinting(Settings.whitespace);
			}
		}
	}

	private DialogInterface.OnClickListener onc = new DialogInterface.OnClickListener(){
		public void onClick(DialogInterface p1, int p2) {
			try {
				File f = new File(pwd, transTxV.getText().toString());
				if (p2 == DialogInterface.BUTTON_POSITIVE)
					f.createNewFile();
				else
					f.mkdir();
				refresh();
			} catch (IOException e) {
				e.printStackTrace();
				toast(e.getMessage());
			}
		}
	};

    public void createFile(View view) {
        View inflate = View.inflate(this, R.layout.edit, null);
        transTxV = inflate.findViewById(R.id.edit_name);
		new AlertDialog.Builder(this)
		.setTitle(R.string.new_)
		.setView(inflate)
		.setPositiveButton(R.string.file, onc)
		.setNeutralButton(R.string.folder, onc)
		.setNegativeButton(android.R.string.cancel, null)
		.create().show();
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		_appmenu = menu;
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.nopen, menu);
		return true;
	}

    @Override
    protected void onStop() {
        getPreferences(MODE_PRIVATE).edit().putString(PWD, pwd.getPath()).commit();
        super.onStop();
    }

    private void toast(CharSequence charSequence) {
        HelperUtils.show(Toast.makeText(this, charSequence, 0));
    }

    public void showList(View view) {
		View v = subc;
        v.setVisibility(v.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
    }

	public void setEditor(TextEditor edit) {
		codeEditor = edit;
	}

	public TextEditor getEditor() {
		return codeEditor;
	}
}
