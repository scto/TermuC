/*
 * Copyright (c) 2013 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package cn.rbc.codeeditor.util;

import cn.rbc.codeeditor.lang.Language;
import cn.rbc.codeeditor.lang.LanguageNonProg;

import java.util.ArrayList;
import java.util.List;

import android.util.*;

/**
 * Does lexical analysis of a text for C-like languages.
 * The programming language syntax used is set as a static class variable.
 */
public class Lexer {
    public final static int UNKNOWN = -1;
    public final static int NORMAL = 0;
    public final static int KEYWORD = 1;
    public final static int OPERATOR = 2;
    public final static int NAME = 3;
    public final static int NUMBER = 4;
	public final static int KEYNAME = 5;
	public final static int TYPE = 6;
	final static int NUM_HEX = 1;
	final static int NUM_FLOAT = 2;
	final static int NUM_EXP = 4;
    /**
     * A word that starts with a special symbol, inclusive.
     * Examples:
     * :ruby_symbol
     */
    public final static int SINGLE_SYMBOL_WORD = 10;
    /**
     * Tokens that extend from a single start symbol, inclusive, until the end of line.
     * Up to 2 types of symbols are supported per language, denoted by A and B
     * Examples:
     * #include "myCppFile"
     * #this is a comment in Python
     * %this is a comment in Prolog
     */
    public final static int SINGLE_SYMBOL_LINE_A = 20;
    public final static int SINGLE_SYMBOL_LINE_B = 21;
    /**
     * Tokens that extend from a two start symbols, inclusive, until the end of line.
     * Examples:
     * //this is a comment in C
     */
    public final static int DOUBLE_SYMBOL_LINE = 30;
    /**
     * Tokens that are enclosed between a start and end sequence, inclusive,
     * that can span multiple lines. The start and end sequences contain exactly
     * 2 symbols.
     * Examples:
     * {- this is a...
     * ...multi-line comment in Haskell -}
     */
    public final static int DOUBLE_SYMBOL_DELIMITED_MULTILINE = 40;
    /**
     * Tokens that are enclosed by the same single symbol, inclusive, and
     * do not span over more than one line.
     * Examples: 'c', "hello world"
     */
    public final static int SINGLE_SYMBOL_DELIMITED_A = 50;
    public final static int SINGLE_SYMBOL_DELIMITED_B = 51;
    private final static int MAX_KEYWORD_LENGTH = 31;
    private static Language _globalLanguage = LanguageNonProg.getInstance();
    LexCallback _callback = null;
    private Document _hDoc;
    private LexThread _workerThread = null;
    public Lexer(LexCallback callback) {
        _callback = callback;
    }

    synchronized public static Language getLanguage() {
        return _globalLanguage;
    }

    synchronized public static void setLanguage(Language lang) {
        _globalLanguage = lang;
    }

    public void tokenize(Document hDoc) {
        if (!Lexer.getLanguage().isProgLang())
            return;

        //tokenize will modify the state of hDoc; make a copy
        setDocument(hDoc);
        if (_workerThread == null) {
            _workerThread = new LexThread(this);
            _workerThread.start();
        } else
            _workerThread.restart();
    }

    void tokenizeDone(List<Pair> result) {
        if (_callback != null)
            _callback.lexDone(result);
        _workerThread = null;
    }

    public void cancelTokenize() {
        if (_workerThread != null) {
            _workerThread.abort();
            _workerThread = null;
        }
    }

    public synchronized Document getDocument() {
        return _hDoc;
    }

    public synchronized void setDocument(Document hDoc) {
        _hDoc = hDoc;
    }


    public interface LexCallback {
        public void lexDone(List<Pair> results);
    }

    private class LexThread extends Thread {
        private final Lexer _lexManager;
        /**
         * can be set by another thread to stop the scan immediately
         */
        private final Flag _abort;
        private boolean rescan = false;
        /**
         * A collection of Pairs, where Pair.first is the start
         * position of the token, and Pair.second is the type of the token.
         */
        private ArrayList<Pair> _tokens;

        public LexThread(Lexer p) {
            _lexManager = p;
            _abort = new Flag();
        }

        @Override
        public void run() {
            do {
                rescan = false;
                _abort.clear();
                tokenize();
            } while (rescan);

            if (!_abort.isSet())
                // lex complete
                _lexManager.tokenizeDone(_tokens);
        }

        public void restart() {
            rescan = true;
            _abort.set();
        }

        public void abort() {
            _abort.set();
        }

        /**
         * Scans the document referenced by _lexManager for tokens.
         * The result is stored internally.
         */
        public void tokenize() {
            Document hDoc = getDocument();
            Language language = Lexer.getLanguage();
            ArrayList<Pair> tokens = new ArrayList<Pair>();

            if (!language.isProgLang()) {
                tokens.add(new Pair(0, NORMAL));
                _tokens = tokens;
                return;
            }

            char[] candidateWord = new char[MAX_KEYWORD_LENGTH];
            int currCharInWord = 0;

            int spanStartPosition = 0;
            int workingPosition = 0;
            int state = UNKNOWN;
			int substate = 0;
            char prevChar = 0;
			boolean stateChanged;

            //hDoc.seekChar(0);
            for (int idx=0, mL=hDoc.getTextLength(); idx<mL && !_abort.isSet();) {
                char currChar = hDoc.charAt(idx++);

                switch (state) {
					case NUMBER:
						stateChanged = false;
						boolean t = (substate&NUM_HEX)==0;
						boolean f = (substate&NUM_FLOAT)==0;
						char upc = Character.toLowerCase(currChar);
						char plc;
						// 十六进制
						if (currCharInWord==1&&prevChar=='0'&&upc=='x') {
							substate |= NUM_HEX;
							stateChanged = true;
							// 浮点数
						} else if (f&&upc=='.') {
							substate |= NUM_FLOAT;
							stateChanged = true;
							// 科学计数法
						} else if ((substate&NUM_EXP)==0
								   && (t&&upc=='e'
								   || (!t)&&upc=='p')) {
							substate |= NUM_EXP;
							substate |= NUM_FLOAT;
							substate &= (~NUM_HEX);
							stateChanged = true;
							// 科学计数后
						} else if (((plc=Character.toLowerCase(prevChar))=='p'||plc=='e')
								   &&(upc=='-'||upc=='+')) {
							stateChanged = true;
							// 结尾
						} else if (f&&upc=='l' || (!f)&&upc=='f') {
							currChar = ' ';
						}
						if (stateChanged||Character.isDigit(currChar)||((!t)&&'a'<=upc&&'f'>=upc)) {
							currCharInWord++;
							break;
						}
						if (!Character.isJavaIdentifierStart(currChar)) {
							spanStartPosition = workingPosition - currCharInWord;
							tokens.add(new Pair(spanStartPosition, state));
						}
						currCharInWord = 0;
						state = UNKNOWN;
					case OPERATOR:
                    case UNKNOWN: //fall-through
                    case NORMAL: //fall-through
                    case KEYWORD: //fall-through
					case KEYNAME:
					case TYPE:
                    case SINGLE_SYMBOL_WORD:
                        int pendingState = state;
                        stateChanged = false;
                        if (language.isLineStart(prevChar, currChar)) {
                            pendingState = DOUBLE_SYMBOL_LINE;
                            stateChanged = true;
                        } else if (language.isMultilineStartDelimiter(prevChar, currChar)) {
                            pendingState = DOUBLE_SYMBOL_DELIMITED_MULTILINE;
                            stateChanged = true;
                        } else if (language.isDelimiterA(currChar)) {
                            pendingState = SINGLE_SYMBOL_DELIMITED_A;
                            stateChanged = true;
                        } else if (language.isDelimiterB(currChar)) {
                            pendingState = SINGLE_SYMBOL_DELIMITED_B;
                            stateChanged = true;
						} else if ((!Character.isJavaIdentifierPart(prevChar)) && Character.isDigit(currChar)) {
							state = NUMBER;
                        } else if (language.isLineAStart(currChar)) {
                            pendingState = SINGLE_SYMBOL_LINE_A;
                            stateChanged = true;
                        } else if (language.isLineBStart(currChar)) {
                            pendingState = SINGLE_SYMBOL_LINE_B;
                            stateChanged = true;
                        }

                        if (stateChanged) {
                            if (pendingState == DOUBLE_SYMBOL_LINE ||
                                    pendingState == DOUBLE_SYMBOL_DELIMITED_MULTILINE) {
                                // account for previous char
                                spanStartPosition = workingPosition - 1;
								//TODO consider less greedy approach and avoid adding token for previous char
                                if (tokens.get(tokens.size() - 1).first == spanStartPosition)
                                    tokens.remove(tokens.size() - 1);
                            } else
                                spanStartPosition = workingPosition;

                            // If a span appears mid-word, mark the chars preceding
                            // it as NORMAL, if the previous span isn't already NORMAL
                            if (currCharInWord > 0 && state != NORMAL)
                                tokens.add(new Pair(workingPosition - currCharInWord, NORMAL));

                            state = pendingState;
                            tokens.add(new Pair(spanStartPosition, state));
                            currCharInWord = 0;
                        } else if (language.isWhitespace(currChar) || language.isOperator(currChar)) {
                            if (currCharInWord > 0) {
                                // full word obtained; mark the beginning of the word accordingly
								stateChanged = false;
                                if (language.isWordStart(candidateWord[0])) {
                                    state = SINGLE_SYMBOL_WORD;
									stateChanged = true;
                                } else if (language.isKeyword(new String(candidateWord, 0, currCharInWord))) {
                                    state = KEYWORD;
									stateChanged = true;
                                } else {
									Integer i = language.type(new String(candidateWord, 0, currCharInWord));
									int _t;
									if (i != null && (_t=i.intValue()) != Lexer.NAME) {
										state = _t;
										stateChanged = true;
									} else if (state != NORMAL) {
                                    	state = NORMAL;
										stateChanged = true;
                                	}
								}
								if (stateChanged) {
									spanStartPosition = workingPosition - currCharInWord;
									tokens.add(new Pair(spanStartPosition, state));
								}
                                currCharInWord = 0;
                            }

                            // mark operators as normal
                            if (state != OPERATOR && language.isOperator(currChar)) {
                                state = OPERATOR;
                                tokens.add(new Pair(workingPosition, state));
                            }
                        } else if (currCharInWord < MAX_KEYWORD_LENGTH) {
                            // collect non-whitespace chars up to MAX_KEYWORD_LENGTH
                            candidateWord[currCharInWord] = currChar;
                            currCharInWord++;
                        }
                        break;


					case DOUBLE_SYMBOL_LINE:
                    case SINGLE_SYMBOL_LINE_A: // fall-through
						if (language.isEscapeChar(prevChar))
							break;
                    case SINGLE_SYMBOL_LINE_B:
                        if (currChar == '\n')
                            state = UNKNOWN;
                        break;


                    case SINGLE_SYMBOL_DELIMITED_A:
                        if ((language.isDelimiterA(currChar) || currChar == '\n')
                                && !language.isEscapeChar(prevChar))
                            state = UNKNOWN;
                        // consume escape of the escape character by assigning
                        // currChar as something else so that it would not be
                        // treated as an escape char in the next iteration
                        else if (language.isEscapeChar(currChar) && language.isEscapeChar(prevChar))
                            currChar = ' ';
                        break;
                    case SINGLE_SYMBOL_DELIMITED_B:
                        if ((language.isDelimiterB(currChar) || currChar == '\n')
                                && !language.isEscapeChar(prevChar))
                            state = UNKNOWN;
                        // consume escape of the escape character by assigning
                        // currChar as something else so that it would not be
                        // treated as an escape char in the next iteration
                        else if (language.isEscapeChar(currChar)
                                && language.isEscapeChar(prevChar))
                            currChar = ' ';
                        break;

                    case DOUBLE_SYMBOL_DELIMITED_MULTILINE:
                        if (language.isMultilineEndDelimiter(prevChar, currChar))
                            state = UNKNOWN;
                        break;

                    default:
                        TextWarriorException.fail("Invalid state in TokenScanner");
                        break;
                }
                ++workingPosition;
                prevChar = currChar;
            }
            // end state machine

            if (tokens.isEmpty())
                // return value cannot be empty
                tokens.add(new Pair(0, NORMAL));

            _tokens = tokens;
        }

    }//end inner class
}
