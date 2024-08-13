package cn.rbc.termuc;
import android.preference.*;
import android.os.*;
import android.view.*;
import cn.rbc.codeeditor.util.*;
import android.widget.*;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
	private final static String TAG = "SettingsActivity";

	private CheckBoxPreference mDarkModePref, mWordWrapPref, mWhitespacePref, mShowHidden;
	private EditTextPreference mCFlagsPref, mHost, mPort;
	private ListPreference mSizePref, mEngine;

	private boolean mWrap, mSpace;
	private String mComp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Settings.dark_mode)
			setTheme(android.R.style.Theme_Holo);
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.settings);

		PreferenceScreen prefSet = getPreferenceScreen();

		mDarkModePref = (CheckBoxPreference)prefSet
			.findPreference(getText(R.string.key_dark_mode));
		mSizePref = (ListPreference)prefSet
			.findPreference(getText(R.string.key_textsize));
		mWordWrapPref = (CheckBoxPreference)prefSet
			.findPreference(getText(R.string.key_wordwrap));
		mWhitespacePref = (CheckBoxPreference)prefSet
			.findPreference(getText(R.string.key_whitespace));
		mShowHidden = (CheckBoxPreference)prefSet
			.findPreference(getText(R.string.key_show_hidden));
		mCFlagsPref = (EditTextPreference)prefSet
			.findPreference(getText(R.string.key_cflags));
		mEngine = (ListPreference)prefSet
			.findPreference(getText(R.string.key_completion));
		mEngine.setOnPreferenceChangeListener(this);
		mHost = (EditTextPreference)prefSet
			.findPreference(getText(R.string.key_lsp_host));
		mPort = (EditTextPreference)prefSet
			.findPreference(getText(R.string.key_lsp_port));
		Settings.getInstance(PreferenceManager
			.getDefaultSharedPreferences(getApplicationContext()));

		mWrap = Settings.wordwrap;
		mSpace = Settings.whitespace;
		mComp = Settings.completion;
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
		setResult(mWrap == mWordWrapPref.isChecked()
				  && mSpace == mWhitespacePref.isChecked()
				  && mComp.equals(mEngine.getValue())
				  ? RESULT_CANCELED : RESULT_OK);
		super.onBackPressed();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateWidget();
	}

	@Override
	protected void onDestroy() {
		Settings.releaseInstance();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Settings.dark_mode = mDarkModePref.isChecked();
		Settings.wordwrap = mWordWrapPref.isChecked();
		Settings.whitespace = mWhitespacePref.isChecked();
		Settings.textsize = Integer.parseInt(mSizePref.getValue());
		Settings.show_hidden = mShowHidden.isChecked();
		Settings.cflags = mCFlagsPref.getText();
		Settings.completion = mEngine.getValue();
		Settings.lsp_host = mHost.getText();
		Settings.lsp_port = Integer.parseInt(mPort.getText());
		Settings.writeBack();
	}

	private void updateWidget() {
		mDarkModePref.setChecked(Settings.dark_mode);
		mSizePref.setValue(Integer.toString(Settings.textsize));
		mWordWrapPref.setChecked(Settings.wordwrap);
		mWhitespacePref.setChecked(Settings.whitespace);
		mShowHidden.setChecked(Settings.show_hidden);
		mCFlagsPref.setText(Settings.cflags);
		mEngine.setValue(Settings.completion);
		mHost.setText(Settings.lsp_host);
		mPort.setText(Integer.toString(Settings.lsp_port));
		boolean b = "s".equals(Settings.completion);
		mHost.setEnabled(b);
		mPort.setEnabled(b);
	}
}
