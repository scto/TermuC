package cn.rbc.codeeditor.view;
import android.graphics.*;

public class ColorSchemeDark extends ColorScheme
{
	private static ColorScheme mColorScheme;

	private ColorSchemeDark() {
		setColor(Colorable.FOREGROUND, 0xFFF0F0F0);
		setColor(Colorable.BACKGROUND, 0xFF2B2B2B);
		setColor(Colorable.TYPE, 0xFF99CCEE);
		setColor(Colorable.KEYWORD, 0xFF6AB0E2);
		setColor(Colorable.NOTE, 0xFF8AB0E2);
		setColor(Colorable.OPERATOR, 0xFF8AB0E2);
		setColor(Colorable.SECONDARY, 0xFFAAAAAA);
		setColor(Colorable.COMMENT, 0xFF50BB50);
		setColor(Colorable.STRING, 0xFFFF8E8E);
		setColor(Colorable.NUMBER, 0xFFFF8E8E);
		setColor(Colorable.CARET_DISABLED, 0xFFF0F0F0);
		setColor(Colorable.CARET_BACKGROUND, 0xFF42A5F5);
		setColor(Colorable.NON_PRINTING_GLYPH, 0xFF686868);
		setColor(Colorable.LINE_HIGHLIGHT, 0xFF373737);
		setColor(Colorable.SELECTION_BACKGROUND, 0xFF505050);
	}

	public static ColorScheme getInstance() {
		if (mColorScheme==null)
			mColorScheme = new ColorSchemeDark();
		return mColorScheme;
	}

	@Override
	public boolean isDark() {
		return true;
	}
}
