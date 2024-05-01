package cn.rbc.termuc;
import android.preference.*;
import android.os.*;

public class SettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}
}
