package cn.rbc.codeeditor.view.autocomplete;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import cn.rbc.termuc.R;
import cn.rbc.codeeditor.util.Flag;
import cn.rbc.codeeditor.view.FreeScrollingTextField;

import java.util.ArrayList;
import java.util.List;

import static cn.rbc.codeeditor.util.DLog.log;
import android.annotation.*;
import android.text.style.*;
import android.graphics.*;

/**
 * Adapter定义
 */
public class AutoPanelAdapter extends BaseAdapter  {

    final static int PADDING = 20;
    private Flag _abort;
    private DisplayMetrics dm;
    private ArrayList<ListItem> listItems;
    public Bitmap bitmap;
    private Context _context;
    private AutoCompletePanel mAutoComplete;
    private FreeScrollingTextField mTextFiled;
	private LayoutInflater mInflater;

    public AutoPanelAdapter(Context context, AutoCompletePanel panel, FreeScrollingTextField textField) {
        _context = context;
        mAutoComplete = panel;
        mTextFiled = textField;
        _abort = new Flag();
        listItems = new ArrayList<>();
        dm = context.getResources().getDisplayMetrics();
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_method);
		mInflater = LayoutInflater.from(context);
    }

    public void abort() {
        _abort.set();
    }

    @Override
    public int getCount() {
        return listItems.size();
    }

    @Override
    public ListItem getItem(int i) {
        return listItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View tempView = null;
        if (view == null) {
            View rootView = mInflater.inflate(R.layout.auto_panel_item, null);
            tempView = rootView;
        } else {
            tempView = view;
        }
        TextView textView = tempView.findViewById(R.id.auto_panel_text);
        ImageView imageView = tempView.findViewById(R.id.auto_panel_icon);
		ListItem it = getItem(i);
        String text = it.label;
		int tp = it.kind;
        SpannableString spannableString = null;
        ForegroundColorSpan foregroundColorSpan = null;
        boolean isDarkMode = mTextFiled.getColorScheme().isDark();
        log(text);
		int t;
		if ((t=text.indexOf(tp==15?' ':'(')) >= 0) {
            // 宏、函数
            ForegroundColorSpan argsForegroundColorSpan = null;
            spannableString = new SpannableString(text);
            if(isDarkMode) {
                foregroundColorSpan = new ForegroundColorSpan(Color.WHITE);
                argsForegroundColorSpan = new ForegroundColorSpan(Color.parseColor("#9C9C9C"));
            } else {
                foregroundColorSpan = new ForegroundColorSpan(Color.BLACK);
                argsForegroundColorSpan = new ForegroundColorSpan(Color.GRAY);
            }
            spannableString.setSpan(foregroundColorSpan, 0, t, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(argsForegroundColorSpan, t, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            // 其他
            spannableString = new SpannableString(text);
            //    if(setting.isDarkMode()) {
            //          foregroundColorSpan = new ForegroundColorSpan(Color.WHITE);
            //    }
            //    else{
            foregroundColorSpan = new ForegroundColorSpan(mAutoComplete._textColor);
            //    }
			if (tp == 14) // 关键字
				spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(foregroundColorSpan, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(spannableString);
		imageView.setImageBitmap(tp==2 || tp==3 ? bitmap : null);
        return tempView;
    }

    public void restart() {
        _abort.clear();
    }

	public void setData(ArrayList<ListItem> l) {
		listItems.clear();
		listItems.addAll(l);
	}
    /**
     * 计算列表高
     *
     * @return
     */
    public View getItemView() {
        return mInflater.inflate(R.layout.auto_panel_item, null);
    }

    /**
     * 实现自动完成的过滤算法
     */
    public Filter getFilter() {
        Filter filter = new Filter() {
            /**
             * 本方法在后台线程执行，定义过滤算法
             */
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                // 此处实现过滤
                // 过滤后利用FilterResults将过滤结果返回
                ArrayList<String> buf = new ArrayList<String>();
                String input = String.valueOf(constraint).toLowerCase();

                String[] keywords = AutoCompletePanel._globalLanguage.getUserWord();
                for (String k : keywords) {
                    if (k.toLowerCase().startsWith(input))
                        buf.add(k);
                }
                keywords = AutoCompletePanel._globalLanguage.getKeywords();
                for (String k : keywords) {
                    if (k.indexOf(input) == 3)
                        buf.add(k);
                }
                keywords = AutoCompletePanel._globalLanguage.getNames();
                for (String k : keywords) {
                    if (k.toLowerCase().startsWith(input))
                        buf.add(k);
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = buf;   // results是上面的过滤结果
                filterResults.count = buf.size();  // 结果数量
                return filterResults;
            }

            /**
             * 本方法在UI线程执行，用于更新自动完成列表
             */
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0 && !_abort.isSet()) {
                    // 有过滤结果，显示自动完成列表
                    listItems.clear();   // 清空旧列表
                    ArrayList<String> stringArrayList = (ArrayList<String>) results.values;
                    for (int i = 0; i < stringArrayList.size(); i++) {
                        String itemText = stringArrayList.get(i);
						ListItem it = new ListItem();
						if (itemText.startsWith("[K]")) {
							it.label = itemText.substring(3);
							it.kind = 14;
						} else {
							it.label = itemText;
							it.kind = itemText.contains("(") ? 2 : 0;
						}
						listItems.add(it);
                    }
                    notifyDataSetChanged();
                    mAutoComplete.show();
                } else {
                    // 无过滤结果，关闭列表
                    notifyDataSetInvalidated();
                }
            }

        };
        return filter;
    }
}
