package cn.rbc.termuc;
import android.preference.*;
import android.os.*;
import android.view.*;
import cn.rbc.codeeditor.util.*;
import android.widget.*;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
	private final static String TAG = "SettingsActivity";

	private CheckBoxPreference mDarkModePref, mWordWrapPref, mWhitespacePref;
	private EditTextPreference mCFlagsPref, mHost, mPort;
	private ListPreference mEngine;

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
		mWordWrapPref = (CheckBoxPreference)prefSet
			.findPreference(getText(R.string.key_wordwrap));
		mWhitespacePref = (CheckBoxPreference)prefSet
			.findPreference(getText(R.string.key_whitespace));
		mCFlagsPref = (EditTextPreference)prefSet
			.findPreference(getText(R.string.key_cflags));
		mEngine = (ListPreference)prefSet
			.findPreference(getText(R.string.key_completion));
		mEngine.setOnPreferenceChangeListener(this);
		mHost = (EditTextPreference)prefSet
			.findPreference(getText(R.string.key_lsp_host));
		mPort = (EditTextPreference)prefSet
			.findPreference(getText(R.string.key_lsp_port));
		//prefSet.setOnPreferenceChangeListener(this);

		Settings.getInstance(PreferenceManager
			.getDefaultSharedPreferences(getApplicationContext()));

		updateWidget();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
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
		}
		return true;
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
		Settings.cflags = mCFlagsPref.getText();
		Settings.completion = mEngine.getValue();
		Settings.lsp_host = mHost.getText();
		Settings.lsp_port = Integer.parseInt(mPort.getText());
		Settings.writeBack();
	}

	private void updateWidget() {
		mDarkModePref.setChecked(Settings.dark_mode);
		mWordWrapPref.setChecked(Settings.wordwrap);
		mWhitespacePref.setChecked(Settings.whitespace);
		mCFlagsPref.setText(Settings.cflags);
		mEngine.setValue(Settings.completion);
		mHost.setText(Settings.lsp_host);
		mPort.setText(Integer.toString(Settings.lsp_port));
	}
}
