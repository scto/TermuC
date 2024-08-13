package cn.rbc.termuc;
import android.widget.*;
import android.view.*;
import android.content.*;
import java.io.*;
import android.text.*;

public class HeaderAdapter extends ArrayAdapter<String> implements SpinnerAdapter
{
	public HeaderAdapter(Context context, int id) {
		super(context, id);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		if (v instanceof TextView) {
			TextView tv = (TextView)v;
			tv.setText(new File(tv.getText().toString()).getName());
		}
		return v;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return super.getView(position, convertView, parent);
	}
}
