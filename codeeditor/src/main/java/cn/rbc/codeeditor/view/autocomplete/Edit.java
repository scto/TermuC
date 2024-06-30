package cn.rbc.codeeditor.view.autocomplete;

public class Edit
{
	public String text;
	public int start, len;

	@Override
	public String toString() {
		return text == null ? "null" : text;
	}
}
