package cn.rbc.codeeditor.view.autocomplete;

public class Edit
{
	public String text;
	public int start, len;

	@Override
	public String toString() {
		// TODO: Implement this method
		return text == null ? "null" : text;
	}
}
