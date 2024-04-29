package cn.rbc.codeeditor.view.autocomplete;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Filter;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import cn.rbc.termuc.R;
import cn.rbc.codeeditor.lang.Language;
import cn.rbc.codeeditor.lang.LanguageNonProg;
import cn.rbc.codeeditor.view.FreeScrollingTextField;
import android.view.*;
import android.widget.*;
import java.util.*;
import android.annotation.*;
import android.graphics.*;
import android.util.*;
import cn.rbc.codeeditor.util.*;


public class AutoCompletePanel implements OnItemClickListener {

    public static Language _globalLanguage = LanguageNonProg.getInstance();
   // public CharSequence _constraint;
    public int _off;
    private FreeScrollingTextField _textField;
    private Context _context;
    private ListPopupWindow _autoCompletePanel;
    private AutoPanelAdapter _adapter;
    private Filter _filter;
    private int _verticalOffset;
    private int _height;
    private int _horizontal;
    private int _backgroundColor;
    private GradientDrawable gd;
    public int _textColor, _mxHeight;
    private boolean isShow = false;

    public AutoCompletePanel(FreeScrollingTextField textField) {
        _textField = textField;
        _context = textField.getContext();
        initAutoCompletePanel();

    }

    synchronized public static Language getLanguage() {
        return _globalLanguage;
    }

    synchronized public static void setLanguage(Language lang) {
        _globalLanguage = lang;
    }

    public void setTextColor(int color) {
        _textColor = color;
        gd.setStroke(1, color);
        _autoCompletePanel.setBackgroundDrawable(gd);
    }

    public void setBackgroundColor(int color) {
        _backgroundColor = color;
        gd.setColor(color);
        _autoCompletePanel.setBackgroundDrawable(gd);
    }

    public void setBackground(Drawable color) {
        _autoCompletePanel.setBackgroundDrawable(color);
    }

    @SuppressWarnings("ResourceType")
    private void initAutoCompletePanel() {
        _autoCompletePanel = new ListPopupWindow(_context);
        _autoCompletePanel.setAnchorView(_textField);
        _adapter = new AutoPanelAdapter(_context, this, _textField);
        _autoCompletePanel.setAdapter(_adapter);
		_autoCompletePanel.setAnimationStyle(0);
        _filter = _adapter.getFilter();
        //_autoCompletePanel.setContentWidth(ListPopupWindow.WRAP_CONTENT);
        _autoCompletePanel.setHeight(300);
	  // _mxHeight = _adapter.getItemHeight() * 3;
        TypedArray array = _context.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.colorBackground,
                android.R.attr.textColorPrimary,
        });
        int backgroundColor = array.getColor(0, 0xFF00FF);
        int textColor = array.getColor(1, 0xFF00FF);
        array.recycle();
        gd = new GradientDrawable();
        gd.setColor(backgroundColor);
        gd.setCornerRadius(4);
        gd.setStroke(1, textColor);
        setTextColor(textColor);
        _autoCompletePanel.setBackgroundDrawable(gd);
        _autoCompletePanel.setOnItemClickListener(this);
    }

	public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
		select(p3);
	}

    public void select(int pos) {
		Deque<Edit> edits = _adapter.getItem(pos).edits;
		Document doc = _textField.getText();
		doc.beginBatchEdit();
		doc.setTyping(false);
		long tp = System.nanoTime();
		Edit it = edits.pollFirst();
		int mc = it.start + it.text.length();
		doc.deleteAt(it.start, it.len, tp);
		doc.insertBefore(it.text.toCharArray(), it.start, tp);
		while (!edits.isEmpty()) {
			it = edits.pop();
			doc.deleteAt(it.start, it.len, tp);
			doc.insertBefore(it.text.toCharArray(), it.start, tp);
			if (it.start + it.len <= mc)
				mc += it.text.length() - it.len;
			else if (it.start < mc)
				mc = it.start + it.text.length();
		}
		doc.endBatchEdit();
		_textField.moveCaret(mc);
		_textField.mCtrlr.determineSpans();
        //_textField.replaceText(_textField.getCaretPosition() - _off, _off, text);
        _adapter.abort();
        dismiss();
    }

    public void setWidth(int width) {
        _autoCompletePanel.setWidth(width);
    }

    public void setHeight(int height) {
        if (_height != height) {
            _height = height;
            _autoCompletePanel.setHeight(height);
        }
    }

    public void setHorizontalOffset(int horizontal) {
        horizontal = Math.min(horizontal, _textField.getWidth() / 2);
        if (_horizontal != horizontal) {
            _horizontal = horizontal;
            _autoCompletePanel.setHorizontalOffset(horizontal);
        }
    }

    void setVerticalOffset(int verticalOffset) {
        //verticalOffset=Math.min(verticalOffset,_textField.getWidth()/2);
        int max = 0 - _autoCompletePanel.getHeight();
        if (verticalOffset > max) {
            _textField.scrollBy(0, verticalOffset - max);
            verticalOffset = max;
        }
        if (_verticalOffset != verticalOffset) {
            _verticalOffset = verticalOffset;
            _autoCompletePanel.setVerticalOffset(verticalOffset);
        }
    }

    public void update(CharSequence constraint) {
        _adapter.restart();
        _filter.filter(constraint);
    }

	public void update(ArrayList<ListItem> l) {
		if (l==null || l.isEmpty()) {
			_adapter.notifyDataSetInvalidated();
		} else {
			_adapter.setData(l);
			int y = _textField.getCaretY() + _textField.rowHeight() / 2 - _textField.getScrollY();
			setHeight(_adapter.getItemHeight() * Math.min(3, l.size()));
			setHorizontalOffset(AutoPanelAdapter.PADDING);
			setWidth(_textField.getWidth() - AutoPanelAdapter.PADDING * 2);
			setVerticalOffset(y - _textField.getHeight());//_textField.getCaretY()-_textField.getScrollY()-_textField.getHeight());
			_adapter.notifyDataSetChanged();
			show();
		}
	}

	public Bitmap getBitmap() {
		return _adapter.bitmap;
	}

    public void show() {
        if (!isShow()) {
			_autoCompletePanel.show();
        }
		//_autoCompletePanel.getListView().setFadingEdgeLength(0);
		isShow = true;
    }

    public void dismiss() {
        if (isShow()) {
			_autoCompletePanel.dismiss();
        }
		isShow = false;
		//HelperUtils.show(Toast.makeText(_textField.getContext(), String.valueOf(isShow), 1));
    }

    public boolean isShow() {
        return isShow && _autoCompletePanel.isShowing();
    }

}
