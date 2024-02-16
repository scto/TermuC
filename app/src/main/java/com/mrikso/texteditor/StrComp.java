package com.mrikso.texteditor;
import java.util.*;

public class StrComp implements Comparator<String>
{
	@Override
	public int compare(String p1, String p2)
	{
		return p1.compareToIgnoreCase(p2);
	}
}
