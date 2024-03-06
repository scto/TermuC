package cn.rbc.codeeditor.util;

public class ErrSpan
{
	public final static int
		ERROR = 0,
		WARNING = 1,
		INFOR = 2,
		HINT = 3;
	public String msg;
	public int stl, stc, enl, enc, severity = 0;

	@Override
	public String toString() {
		// TODO: Implement this method
		return new StringBuilder()
			.append('(')
			.append(stl)
			.append(':')
			.append(stc)
			.append(',')
			.append(enl)
			.append(':')
			.append(enc)
			.append(",sev=")
			.append(severity)
			.append(",msg=")
			.append(msg)
			.append(')').toString();
	}
}
