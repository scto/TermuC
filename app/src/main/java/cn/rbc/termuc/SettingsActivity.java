package cn.rbc.termuc;
import android.preference.*;
import android.os.*;
import android.view.*;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
	private final static String TAG = "SettingsActivity";

	private CheckBoxPreference mDarkModePref, mWordWrapPref, mWhitespacePref;
	private EditTextPreference mCFlagsPref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
		prefSet.setOnPreferenceChangeListener(this);

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
		Settings.mDarkMode = mDarkModePref.isChecked();
		Settings.mWordWrap = mWordWrapPref.isChecked();
		Settings.mWhiteSpace = mWhitespacePref.isChecked();
		Settings.mCFlags = mCFlagsPref.getText();
		Settings.writeBack();
	}

	private void updateWidget() {
		mDarkModePref.setChecked(Settings.mDarkMode);
		mWordWrapPref.setChecked(Settings.mWordWrap);
		mWhitespacePref.setChecked(Settings.mWhiteSpace);
		mCFlagsPref.setText(Settings.mCFlags);
	}
}
