package cn.rbc.codeeditor.lang;
import cn.rbc.codeeditor.util.*;

public interface Formatter
{
	//public int createAutoIndent(CharSequence text);
	public void format(Document text, int width);
}
