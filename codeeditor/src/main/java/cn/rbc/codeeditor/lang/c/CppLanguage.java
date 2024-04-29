package cn.rbc.codeeditor.lang.c;

import cn.rbc.codeeditor.lang.*;
import cn.rbc.codeeditor.util.*;

public class CppLanguage extends Language{
	private static Language _theOne = null;

	public static Language getInstance(){
		if(_theOne == null){
			_theOne = new CppLanguage();
		}
		return _theOne;
	}

	@Override
	public boolean isProgLang() {
		return true;
	}

	@Override
	public Lexer newLexer(CharSeqReader reader) {
		return new CppLexer(reader);
	}
}

