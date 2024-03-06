package cn.rbc.codeeditor.view.autocomplete;

//import android.graphics.Bitmap;
import java.util.*;

public class ListItem {
    //public Bitmap bitmap;
    public String label;
	public int kind;
	public Deque<Edit> edits = new ArrayDeque<>();
}
