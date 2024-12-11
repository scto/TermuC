package cn.rbc.codeeditor.common;
import cn.rbc.codeeditor.util.*;

public interface OnTextChangeListener {
	//void onBeginBatch();
	void onChanged(CharSequence c, int start, boolean ins, boolean typ);
	//void onEndBatch();
}
