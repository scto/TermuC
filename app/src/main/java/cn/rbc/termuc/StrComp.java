package cn.rbc.termuc;
import java.util.*;

public class StrComp implements Comparator<String>
{
	public int compare(String p1, String p2)
	{
		return p1.compareToIgnoreCase(p2);
	}
}
