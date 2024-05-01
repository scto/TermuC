package cn.rbc.codeeditor.lang.c;

import cn.rbc.codeeditor.lang.*;
import cn.rbc.codeeditor.util.*;

public class CLanguage extends Language{
	private static Language _theOne = null;

	/*private final static String[] types = {
		"char", "double", "float", "int", "long", "short", "void", "signed", "unsigned"
		/*"auto", "const", "extern", "register", "static", "volatile",
		"signed", "unsigned", "sizeof", "typedef",
		"enum", "struct", "union",
		"break", "case", "continue", "default", "do", "else", "for",
		"goto", "if", "return", "switch", "while"*
	};*/

	public static Language getInstance(){
		if(_theOne == null){
			_theOne = new CLanguage();
		}
		return _theOne;
	}

	@Override
	public boolean isProgLang() {
		return true;
	}

	@Override
	public Lexer newLexer(CharSeqReader reader) {
		return new CLexer(reader);
	}
}

