package cn.rbc.termuc;
import android.preference.*;
import android.os.*;
import android.view.*;
import cn.rbc.codeeditor.util.*;
import android.widget.*;

public class SettingsActivity extends PreferenceActivity
implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener
{
	private final static String TAG = "SettingsActivity";

	private CheckBoxPreference mDarkModePref, mWordWrapPref, mWhitespacePref, mShowHidden;
	private EditTextPreference mCFlagsPref, mHost, mPort;
	private ListPreference mSizePref, mEngine;

	private boolean mDark, mWrap, mSpace;
	private String mComp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Application.dark_mode)
			setTheme(android.R.style.Theme_Holo);
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.settings);

		mDarkModePref = (CheckBoxPreference)findPreference(Application.KEY_DARKMODE);
		mSizePref = (ListPreference)findPreference(Application.KEY_TEXTSIZE);
		mWordWrapPref = (CheckBoxPreference)findPreference(Application.KEY_WORDWRAP);
		mWhitespacePref = (CheckBoxPreference)findPreference(Application.KEY_WHITESPACE);
		mShowHidden = (CheckBoxPreference)findPreference(Application.KEY_SHOW_HIDDEN);
		mCFlagsPref = (EditTextPreference)findPreference(Application.KEY_CFLAGS);
		mEngine = (ListPreference)findPreference(Application.KEY_COMPLETION);
		mEngine.setOnPreferenceChangeListener(this);
		mHost = (EditTextPreference)findPreference(Application.KEY_LSP_HOST);
		mPort = (EditTextPreference)findPreference(Application.KEY_LSP_PORT);
		findPreference(Application.KEY_CHECKAPP).setOnPreferenceClickListener(this);
		findPreference(Application.KEY_INITAPP).setOnPreferenceClickListener(this);

		mDark = Application.dark_mode;
		mWrap = Application.wordwrap;
		mSpace = Application.whitespace;
		mComp = Application.completion;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				break;
		}
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference p1) {
		if (Application.KEY_CHECKAPP.equals(p1.getKey()))
			Utils.testApp(this, true);
		else
			Utils.initBack(this, true);
		return true;
	}

	@Override
	public boolean onPreferenceChange(Preference p1, Object p2) {
		if (p1.compareTo(mEngine)==0) {
			boolean enable = "s".equals(p2);
			mHost.setEnabled(enable);
			mPort.setEnabled(enable);
			if (!enable)
				HelperUtils.show(Toast.makeText(this, "敬请期待\nComing soon", Toast.LENGTH_SHORT));
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		setResult(mDark != mDarkModePref.isChecked()
				  ? RESULT_FIRST_USER : 
				  (mWrap == mWordWrapPref.isChecked()
				  && mSpace == mWhitespacePref.isChecked()
				  && mComp.equals(mEngine.getValue()))
				  ? RESULT_CANCELED : RESULT_OK);
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Application.dark_mode = mDarkModePref.isChecked();
		Application.wordwrap = mWordWrapPref.isChecked();
		Application.whitespace = mWhitespacePref.isChecked();
		Application.textsize = Integer.parseInt(mSizePref.getValue());
		Application.show_hidden = mShowHidden.isChecked();
		Application.cflags = mCFlagsPref.getText();
		Application.completion = mEngine.getValue();
		Application.lsp_host = mHost.getText();
		Application.lsp_port = Integer.parseInt(mPort.getText());
		super.onPause();
	}
}
