package cn.rbc.termuc;
import android.widget.*;
import android.view.*;
import android.content.*;
import java.io.*;
import android.text.*;
import java.util.*;

public class HeaderAdapter extends ArrayAdapter<String>
implements SpinnerAdapter, Iterable<String>
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

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			private int curr = 0;
			public boolean hasNext() {
				return curr < getCount();
			}
			public String next() {
				return getItem(curr++);
			}
			public void remove() {}
		};
	}

	@Override
	public Spliterator<String> spliterator() {
		return null;
	}
}
