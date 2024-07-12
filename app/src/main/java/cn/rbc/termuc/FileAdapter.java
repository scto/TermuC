package cn.rbc.termuc;
import android.widget.*;
import android.view.*;
import android.content.*;
import java.util.*;
import java.io.*;

public class FileAdapter extends BaseAdapter implements Comparator<File>
{
	private Context mCont;
	private static FileItem parent;
	private FileItem[] mData;
	private boolean mNRoot;
	private File mPath;
	private LayoutInflater mInflater;

	public FileAdapter(Context context, File path) {
		super();
		mCont = context;
		mInflater = LayoutInflater.from(context);
		setPath(path);
	}

	@Override
	public long getItemId(int p1) {
		return p1;
	}

	@Override
	public FileItem getItem(int p1) {
		if (mNRoot) {
			if (p1==0) return parent;
			else p1--;
		}
		return mData[p1];
	}

	@Override
	public int getCount() {
		return mNRoot ? mData.length+1 : mData.length;
	}

	@Override
	public View getView(int pos, View convert, ViewGroup parent) {
		if (convert == null)
			convert = mInflater.inflate(R.layout.file_item, parent, false);
			//convert.setTag();
		FileItem fitm = getItem(pos);
		int icon = fitm.icon;
		//if (icon != -1)
		((ImageView)convert.findViewById(R.id.file_icon)).setImageResource(icon);
		((TextView)convert.findViewById(R.id.file_name)).setText(fitm.name);
		return convert;
	}

	public void setPath(File path) {
		mNRoot = !Utils.ROOT.equals(path);
		if (parent==null && mNRoot)
			parent = new FileItem(R.drawable.ic_folder_24, "..");
		mPath = path;
		File[] lst = path.listFiles();
		if (lst==null)
			lst = new File[0];
		Arrays.sort(lst, this);
		mData = new FileItem[lst.length];
		for (int i=0,l=lst.length;i<l;i++) {
			File f = lst[i];
			mData[i] = new FileItem(computeIcon(f), f.getName());
		}
	}

	public int compare(File a, File b) {
		boolean ad=a.isDirectory(), bd=b.isDirectory();
		return ad==bd?
			a.getName().compareToIgnoreCase(b.getName())
			:ad?-1:1;
	}

	private static int computeIcon(File f) {
		if (f.isDirectory())
			return R.drawable.ic_folder_24;
		else {
			String n = f.getName();
			if (n.endsWith(".c")||n.endsWith(".cpp")
				||n.endsWith(".h")||n.endsWith(".hpp"))
				return R.drawable.ic_code_24;
		}
		return R.drawable.ic_file_24;
	}
}
