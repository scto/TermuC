package cn.rbc.codeeditor.view;

import android.content.Context;
import android.text.ClipboardManager;
import android.view.inputmethod.InputMethodManager;

import cn.rbc.codeeditor.lang.Language;
import cn.rbc.codeeditor.util.Tokenizer;
import cn.rbc.codeeditor.util.Pair;
import cn.rbc.codeeditor.util.TextWarriorException;

import java.util.List;

import static cn.rbc.codeeditor.util.DLog.log;
import android.util.*;
import cn.rbc.codeeditor.util.*;
import java.util.*;
import android.widget.*;

//*********************************************************************
//************************ Controller logic ***************************
//*********************************************************************
public class TextFieldController implements Tokenizer.LexCallback, Runnable {
    private final Tokenizer _lexer = new Tokenizer(this);
    public boolean _isInSelectionMode = false;
    private boolean _isInSelectionMode2;
	public boolean lexing;
    private FreeScrollingTextField field;
	private List<Pair> mRes;

    public TextFieldController(FreeScrollingTextField textField) {
        field = textField;
		lexing = false;
    }

    /**
     * Analyze the text for programming language keywords and redraws the
     * text view when done. The global programming language used is set with
     * the static method Lexer.setLanguage(Language)
     * <p>
     * Does nothing if the Lexer language is not a programming language
     */
    public void determineSpans() {
		lexing = true;
		_lexer.tokenize(field.hDoc);
    }

    public void cancelSpanning() {
		lexing = false;
        _lexer.cancelTokenize();
    }

    @Override
    //This is usually called from a non-UI thread
    public void lexDone(final List<Pair> results) {
		mRes = results;
        field.post(this);
    }

	public void run() {
		field.hDoc.setSpans(mRes);
		lexing = false;
		field.invalidate();
	}

    //- TextFieldController -----------------------------------------------
    //---------------------------- Key presses ----------------------------

    //TODO minimise invalidate calls from moveCaret(), insertion/deletion and word wrap
    public void onPrintableChar(char c) {
        // delete currently selected text, if any
		lexing = true;
        boolean selectionDeleted = false;
        if (_isInSelectionMode) {
            selectionDelete();
            selectionDeleted = true;
        }
		FreeScrollingTextField fld = field;
		int pos = fld.mCaretPosition;
		fld.hDoc.setTyping(true);
        switch (c) {
            case Language.BACKSPACE:
                if (selectionDeleted)
                    break;
                if (pos > 0) {
					//pos--;
					//char p;
					int l = pos > 1 && ((c = fld.hDoc.charAt(pos - 2)) == 0xd83d || c == 0xd83c) ? 2 : 1;
					//String s = 
                    fld.hDoc.deleteAt(pos - l, l, System.nanoTime());
                    /*if (pos>0 && field.hDoc.charAt(--pos) == 0xd83d || field.hDoc.charAt(pos) == 0xd83c) {
					 field.hDoc.deleteAt(pos, System.nanoTime());
					 moveCaretLeft(true);
					 }*/

                    fld.onDel(String.valueOf(c), fld.mCaretPosition, 1);
                    moveCaretLeft(true);
					if (l == 2)
						moveCaretLeft(true);
                }
                break;
            case Language.NEWLINE:
                if (fld.isAutoIndent) {
                    char[] indent = createAutoIndent();
                    field.hDoc.insertBefore(indent, pos, System.nanoTime());
                    moveCaret(field.mCaretPosition + indent.length);
					break;
                }
				field.hDoc.insertBefore(new char[]{c}, pos, System.nanoTime());
                moveCaretRight(true);
                field.onAdd(String.valueOf(c), pos, 1);
				break;
			case Language.TAB:
				if (fld.isUseSpace()) {
					int tl = fld.mTabLength;
					char[] cs = new char[tl - pos % tl];
					Arrays.fill(cs, ' ');
					field.hDoc.insertBefore(cs, pos, System.nanoTime());
					moveCaret(pos + cs.length);
					field.onAdd(new String(cs), pos, cs.length);
					break;
				}
            default:
                field.hDoc.insertBefore(new char[]{c}, pos, System.nanoTime());
                moveCaretRight(true);
                field.onAdd(String.valueOf(c), pos, 1);
                break;
        }

        field.setEdited(true);
        determineSpans();
		//tc
    }

    /**
     * Return a char[] with a newline as the 0th element followed by the
     * leading spaces and tabs of the line that the caret is on
     * 创建自动缩进
     */
    private char[] createAutoIndent() {
		Document doc = field.hDoc;
		int pos = field.mCaretPosition;
        int lineNum = doc.findLineNumber(pos);
        int startOfLine = doc.getLineOffset(lineNum);
        int whitespaceCount = 0;
        //查找上一行的空白符个数
		int i, mL = doc.getTextLength(), mTL = field.mTabLength;
		char c;
        for (i = startOfLine; i < pos;) {
            c = doc.charAt(i++);
            if (c != ' ' && c != Language.TAB)
                break;
            if (c == Language.TAB)
                whitespaceCount += mTL-whitespaceCount%mTL;
            else if (c == ' ')
                ++whitespaceCount;
        }
        //寻找最后字符
        int endChar = 0;
        for (i = startOfLine;i < mL;) {
            c = doc.charAt(i++);
            if (c == Language.NEWLINE || c == Language.EOF)
                break;
            endChar = c;
        }
		//最后字符为'{',缩进
        if (endChar == '{')
            whitespaceCount += field.mAutoIndentWidth;
        if (whitespaceCount < 0)
            return new char[]{Language.NEWLINE};
		char[] indent;
		if (field.isUseSpace()) {
			indent = new char[1 + whitespaceCount];
			indent[0] = Language.NEWLINE;
			Arrays.fill(indent, 1, indent.length, ' ');
		} else {
			int tl = 1 + whitespaceCount / mTL;
			indent = new char[tl + whitespaceCount % mTL];
			indent[0] = Language.NEWLINE;
			Arrays.fill(indent, 1, tl, Language.TAB);
			Arrays.fill(indent, tl, indent.length, ' ');
		}
        return indent;
    }

    public void moveCaretDown() {
        if (!field.caretOnLastRowOfFile()) {
            int currCaret = field.mCaretPosition;
            int currRow = field.mCaretRow;
            int newRow = currRow + 1;
            int currColumn = field.getColumn(currCaret);
            int currRowLength = field.hDoc.getRowSize(currRow);
            int newRowLength = field.hDoc.getRowSize(newRow);

            if (currColumn < newRowLength)
			// Position at the same column as old row.
                field.mCaretPosition += currRowLength;
            else
			// Column does not exist in the new row (new row is too short).
			// Position at end of new row instead.
                field.mCaretPosition +=
					currRowLength - currColumn + newRowLength - 1;
            ++field.mCaretRow;

            updateSelectionRange(currCaret, field.mCaretPosition);
            if (!field.focusCaret())
                field.invalidateRows(currRow, newRow + 1);
            // 拖动yoyo球滚动时，保证yoyo球的坐标与光标一致
            field.crtLis.updateCaret(field.mCaretPosition);
            field.mRowListener.onRowChanged(newRow);
            stopTextComposing();
        }
    }

    public void moveCaretUp() {
        if (!field.caretOnFirstRowOfFile()) {
            int currCaret = field.mCaretPosition;
            int currRow = field.mCaretRow;
            int newRow = currRow - 1;
            int currColumn = field.getColumn(currCaret);
            int newRowLength = field.hDoc.getRowSize(newRow);

            if (currColumn < newRowLength)
			// Position at the same column as old row.
                field.mCaretPosition -= newRowLength;
            else
			// Column does not exist in the new row (new row is too short).
			// Position at end of new row instead.
                field.mCaretPosition -= (currColumn + 1);
            --field.mCaretRow;

            updateSelectionRange(currCaret, field.mCaretPosition);
            if (!field.focusCaret())
                field.invalidateRows(newRow, currRow + 1);
            // 拖动yoyo球滚动时，保证yoyo球的坐标与光标一致
            field.crtLis.updateCaret(field.mCaretPosition);
            field.mRowListener.onRowChanged(newRow);
            stopTextComposing();
        }
    }

    /**
     * @param isTyping Whether caret is moved to a consecutive position as
     *                 a result of entering text
     */
    public void moveCaretRight(boolean isTyping) {
        if (!field.caretOnEOF()) {
            int originalRow = field.mCaretRow;
            ++field.mCaretPosition;
            updateCaretRow();
            updateSelectionRange(field.mCaretPosition - 1, field.mCaretPosition);
            if (!field.focusCaret())
                field.invalidateRows(originalRow, field.mCaretRow + 1);

            if (!isTyping)
                stopTextComposing();
            // 拖动yoyo球滚动时，保证yoyo球的坐标与光标一致
            field.crtLis.updateCaret(field.mCaretPosition);
        }
    }

    /**
     * @param isTyping Whether caret is moved to a consecutive position as
     *                 a result of deleting text
     */
    public void moveCaretLeft(boolean isTyping) {
        if (field.mCaretPosition > 0) {
            int originalRow = field.mCaretRow;
            --field.mCaretPosition;
            updateCaretRow();
            updateSelectionRange(field.mCaretPosition + 1, field.mCaretPosition);
            if (!field.focusCaret())
                field.invalidateRows(field.mCaretRow, originalRow + 1);

            if (!isTyping)
                stopTextComposing();
            // 拖动yoyo球滚动时，保证yoyo球的坐标与光标一致
            field.crtLis.updateCaret(field.mCaretPosition);
        }
    }

    public void moveCaret(int i) {
        if (i < 0 || i >= field.hDoc.getTextLength()) {
            TextWarriorException.fail("Invalid caret position");
            return;
        }
        updateSelectionRange(field.mCaretPosition, i);

        field.mCaretPosition = i;
        updateAfterCaretJump();
    }

    private void updateAfterCaretJump() {
        int oldRow = field.mCaretRow;
        updateCaretRow();
        if (!field.focusCaret()) {
            field.invalidateRows(oldRow, oldRow + 1); //old caret row
            field.invalidateCaretRow(); //new caret row
        }
        stopTextComposing();
    }

    /**
     * This helper method should only be used by internal methods after setting
     * mTextFiledl.mCaretPosition, in order to to recalculate the new row the caret is on.
     */
    void updateCaretRow() {
        int newRow = field.hDoc.findRowNumber(field.mCaretPosition);
        if (field.mCaretRow != newRow) {
            field.mCaretRow = newRow;
            field.mRowListener.onRowChanged(newRow);
        }
    }

    public void stopTextComposing() {
        InputMethodManager im = (InputMethodManager) field.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        // This is an overkill way to inform the InputMethod that the caret
        // might have changed position and it should re-evaluate the
        // caps mode to use.
        im.restartInput(field);

        if (field.mInputConnection != null && field.mInputConnection.isComposingStarted())
            field.mInputConnection.resetComposingState();
    }

    //- TextFieldController -----------------------------------------------
    //-------------------------- Selection mode ---------------------------
    public final boolean isSelectText() {
        return _isInSelectionMode;
    }

    /**
     * Enter or exit select mode.
     * Does not invalidate view.
     *
     * @param mode If true, enter select mode; else exit select mode
     */
    public void setSelectText(boolean mode) {
        if (mode == _isInSelectionMode)
            return;

        if (mode) {
            field.mSelectionAnchor = field.mCaretPosition;
            field.mSelectionEdge = field.mCaretPosition;
        } else {
            field.mSelectionAnchor = -1;
            field.mSelectionEdge = -1;
        }
        _isInSelectionMode = mode;
        _isInSelectionMode2 = mode;
        field.selLis.onSelectionChanged(mode, field.getSelectionStart(), field.getSelectionEnd());
    }

    public final boolean isSelectText2() {
        return _isInSelectionMode2;
    }

    public boolean inSelectionRange(int charOffset) {
        if (field.mSelectionAnchor < 0)
            return false;
        return (field.mSelectionAnchor <= charOffset && charOffset < field.mSelectionEdge);
    }

    /**
     * Selects numChars count of characters starting from beginPosition.
     * Invalidates necessary areas.
     *
     * @param beginPosition
     * @param numChars
     * @param scrollToStart If true, the start of the selection will be scrolled
     *                      into view. Otherwise, the end of the selection will be scrolled.
     */

    public void setSelectionRange(int beginPosition, int numChars, boolean scrollToStart, boolean mode) {
        TextWarriorException.assertVerbose(
			(beginPosition >= 0) && numChars <= (field.hDoc.getTextLength() - 1) && numChars >= 0,
			"Invalid range to select");

        if (_isInSelectionMode)
		// unhighlight previous selection
            field.invalidateSelectionRows();
        else {
            // unhighlight caret
            field.invalidateCaretRow();
            if (mode)
                setSelectText(true);
            else
                _isInSelectionMode = true;
        }

        field.mSelectionAnchor = beginPosition;
        field.mSelectionEdge = field.mSelectionAnchor + numChars;

        field.mCaretPosition = field.mSelectionEdge;
        stopTextComposing();
        updateCaretRow();
        if (mode)
            field.selLis.onSelectionChanged(isSelectText(), field.mSelectionAnchor, field.mSelectionEdge);
        boolean scrolled = field.makeCharVisible(field.mSelectionEdge);

        if (scrollToStart)
		//TODO reduce unnecessary scrolling and write a method to scroll
		// the beginning of multi-line selections as far left as possible
            scrolled = field.makeCharVisible(field.mSelectionAnchor);

        if (!scrolled)
            field.invalidateSelectionRows();
    }

    /**
     * Moves the caret to an edge of selected text and scrolls it to view.
     *
     * @param start If true, moves the caret to the beginning of
     *              the selection. Otherwise, moves the caret to the end of the selection.
     *              In all cases, the caret is scrolled to view if it is not visible.
     */
    public void focusSelection(boolean start) {
        if (_isInSelectionMode) {
            if (start && field.mCaretPosition != field.mSelectionAnchor) {
                field.mCaretPosition = field.mSelectionAnchor;
                updateAfterCaretJump();
            } else if (!start && field.mCaretPosition != field.mSelectionEdge) {
                field.mCaretPosition = field.mSelectionEdge;
                updateAfterCaretJump();
            }
        }
    }


    /**
     * Used by internal methods to update selection boundaries when a new
     * caret position is set.
     * Does nothing if not in selection mode.
     */
    private void updateSelectionRange(int oldCaretPosition, int newCaretPosition) {

        if (!_isInSelectionMode)
            return;

        if (oldCaretPosition < field.mSelectionEdge) {
            if (newCaretPosition > field.mSelectionEdge) {
                field.mSelectionAnchor = field.mSelectionEdge;
                field.mSelectionEdge = newCaretPosition;
            } else
                field.mSelectionAnchor = newCaretPosition;

        } else if (newCaretPosition < field.mSelectionAnchor) {
            field.mSelectionEdge = field.mSelectionAnchor;
            field.mSelectionAnchor = newCaretPosition;
        } else
            field.mSelectionEdge = newCaretPosition;
    }

    //- TextFieldController -----------------------------------------------
    //------------------------ Cut, copy, paste, delete ---------------------------

    /**
     * Convenience method for consecutive copy and paste calls
     */
    public void cut(ClipboardManager cb) {
        copy(cb);
        selectionDelete();
		determineSpans();
		//tc
    }

    /**
     * Copies the selected text to the clipboard.
     * <p>
     * Does nothing if not in select mode.
     */
    public void copy(ClipboardManager cb) {
        //TODO catch OutOfMemoryError
        if (_isInSelectionMode &&
			field.mSelectionAnchor < field.mSelectionEdge) {
            CharSequence contents = field.hDoc.subSequence(field.mSelectionAnchor,
														   field.mSelectionEdge - field.mSelectionAnchor);
            cb.setText(contents);
        }
    }

    /**
     * Inserts text at the caret position.
     * Existing selected text will be deleted and select mode will end.
     * The deleted area will be invalidated.
     * <p>
     * After insertion, the inserted area will be invalidated.
     */
    public void paste(String text) {
        if (text == null)
            return;

		Document doc = field.hDoc;
		doc.setTyping(true);
        doc.beginBatchEdit();
        selectionDelete();

        doc.insertBefore(text.toCharArray(), field.mCaretPosition, System.nanoTime());
        field.onAdd(text, field.mCaretPosition, text.length());
        doc.endBatchEdit();

        field.mCaretPosition += text.length();
        updateCaretRow();

        field.setEdited(true);
        determineSpans();
		//tc
        stopTextComposing();
    }

    /**
     * Deletes selected text, exits select mode and invalidates deleted area.
     * If the selected range is empty, this method exits select mode and
     * invalidates the caret.
     * <p>
     * Does nothing if not in select mode.
     */
    public void selectionDelete() {
        if (!_isInSelectionMode)
            return;

        int totalChars = field.mSelectionEdge - field.mSelectionAnchor;

        if (totalChars > 0) {
            int originalRow = field.hDoc.findRowNumber(field.mSelectionAnchor);
            int originalOffset = field.hDoc.getRowOffset(originalRow);
            boolean isSingleRowSel = field.hDoc.findRowNumber(field.mSelectionEdge) == originalRow;
            field.hDoc.deleteAt(field.mSelectionAnchor, totalChars, System.nanoTime());
            field.onDel("", field.mCaretPosition, totalChars);
            field.mCaretPosition = field.mSelectionAnchor;
            updateCaretRow();
            field.setEdited(true);
            setSelectText(false);
            stopTextComposing();

            if (!field.focusCaret()) {
                int invalidateStartRow = originalRow;
                //invalidate previous row too if its wrapping changed
                if (field.hDoc.isWordWrap() &&
					originalOffset != field.hDoc.getRowOffset(originalRow)) {
                    --invalidateStartRow;
                }

                if (isSingleRowSel && !field.hDoc.isWordWrap())
				//pasted text only affects current row
                    field.invalidateRows(invalidateStartRow, invalidateStartRow + 1);
                else
				//TODO invalidate damaged rows only
                    field.invalidateFromRow(invalidateStartRow);
            }
        } else {
            setSelectText(false);
            field.invalidateCaretRow();
        }
    }

    void replaceText(int from, int charCount, CharSequence text) {
        int invalidateStartRow, originalOffset;
        boolean isInvalidateSingleRow = true;
        boolean dirty = false;
        //delete selection
        if (_isInSelectionMode) {
            invalidateStartRow = field.hDoc.findRowNumber(field.mSelectionAnchor);
            originalOffset = field.hDoc.getRowOffset(invalidateStartRow);

            int totalChars = field.mSelectionEdge - field.mSelectionAnchor;

            if (totalChars > 0) {
                field.mCaretPosition = field.mSelectionAnchor;
                field.hDoc.deleteAt(field.mSelectionAnchor, totalChars, System.nanoTime());

                if (invalidateStartRow != field.mCaretRow)
                    isInvalidateSingleRow = false;
                dirty = true;
            }

            setSelectText(false);
        } else {
            invalidateStartRow = field.mCaretRow;
            originalOffset = field.hDoc.getRowOffset(field.mCaretRow);
        }

        //delete requested chars
        if (charCount > 0) {
            int delFromRow = field.hDoc.findRowNumber(from);
            if (delFromRow < invalidateStartRow) {
                invalidateStartRow = delFromRow;
                originalOffset = field.hDoc.getRowOffset(delFromRow);
            }

            if (invalidateStartRow != field.mCaretRow)
                isInvalidateSingleRow = false;

            field.mCaretPosition = from;
            field.hDoc.deleteAt(from, charCount, System.nanoTime());
            dirty = true;
        }

        //insert
        if (text != null && text.length() > 0) {
            int insFromRow = field.hDoc.findRowNumber(from);
            if (insFromRow < invalidateStartRow) {
                invalidateStartRow = insFromRow;
                originalOffset = field.hDoc.getRowOffset(insFromRow);
            }
            field.hDoc.insertBefore(text.toString().toCharArray(), field.mCaretPosition, System.nanoTime());
            field.mCaretPosition += text.length();
            dirty = true;
        }

        if (dirty) {
            field.setEdited(true);
            determineSpans();
			field.focusCaret();
			return;
        }

        int originalRow = field.mCaretRow;
        updateCaretRow();
        if (originalRow != field.mCaretRow)
            isInvalidateSingleRow = false;

        if (!field.focusCaret()) {
            //invalidate previous row too if its wrapping changed
            if (field.hDoc.isWordWrap() &&
				originalOffset != field.hDoc.getRowOffset(invalidateStartRow))
                --invalidateStartRow;

            if (isInvalidateSingleRow && !field.hDoc.isWordWrap())
			//replaced text only affects current row
                field.invalidateRows(field.mCaretRow, field.mCaretRow + 1);
            else
			//TODO invalidate damaged rows only
                field.invalidateFromRow(invalidateStartRow);
        }
    }

    //- TextFieldController -----------------------------------------------
    //----------------- Helper methods for InputConnection ----------------

    /**
     * Deletes existing selected text, then deletes charCount number of
     * characters starting at from, and inserts text in its place.
     * <p>
     * Unlike paste or selectionDelete, does not signal the end of
     * text composing to the IME.
     */
    void replaceComposingText(int from, int charCount, String text) {
        int invalidateStartRow, originalOffset;
        boolean isInvalidateSingleRow = true;
        boolean dirty = false;

		Document doc = field.hDoc;
		doc.setTyping(true);
        //delete selection
        if (_isInSelectionMode) {
            invalidateStartRow = doc.findRowNumber(field.mSelectionAnchor);
            originalOffset = doc.getRowOffset(invalidateStartRow);

            int totalChars = field.mSelectionEdge - field.mSelectionAnchor;

            if (totalChars > 0) {
                field.mCaretPosition = field.mSelectionAnchor;
                doc.deleteAt(field.mSelectionAnchor, totalChars, System.nanoTime());

                if (invalidateStartRow != field.mCaretRow)
                    isInvalidateSingleRow = false;
                dirty = true;
            }

            setSelectText(false);
        } else {
            invalidateStartRow = field.mCaretRow;
            originalOffset = doc.getRowOffset(field.mCaretRow);
        }

        //delete requested chars
        if (charCount > 0) {
            int delFromRow = doc.findRowNumber(from);
            if (delFromRow < invalidateStartRow) {
                invalidateStartRow = delFromRow;
                originalOffset = doc.getRowOffset(delFromRow);
            }

            if (invalidateStartRow != field.mCaretRow)
                isInvalidateSingleRow = false;

            field.mCaretPosition = from;
            doc.deleteAt(from, charCount, System.nanoTime());
            dirty = true;
        }

        //insert
        if (text != null && text.length() > 0) {
            int insFromRow = doc.findRowNumber(from);
            if (insFromRow < invalidateStartRow) {
                invalidateStartRow = insFromRow;
                originalOffset = doc.getRowOffset(insFromRow);
            }

            log("inserted text:" + text);
            doc.insertBefore(text.toCharArray(), field.mCaretPosition, System.nanoTime());
            field.mCaretPosition += text.length();
            dirty = true;

        }

        field.onAdd(text, field.mCaretPosition, text.length() - charCount);
        if (dirty) {
            field.setEdited(true);
            determineSpans();
			field.focusCaret();
			return;
        }

        int originalRow = field.mCaretRow;
        updateCaretRow();
        if (originalRow != field.mCaretRow)
            isInvalidateSingleRow = false;

        if (!field.focusCaret()) {
            //invalidate previous row too if its wrapping changed
            if (doc.isWordWrap() &&
				originalOffset != doc.getRowOffset(invalidateStartRow))
                --invalidateStartRow;

            if (isInvalidateSingleRow && !doc.isWordWrap())
			//replaced text only affects current row
                field.invalidateRows(field.mCaretRow, field.mCaretRow + 1);
            else
			//TODO invalidate damaged rows only
                field.invalidateFromRow(invalidateStartRow);
        }
    }

    /**
     * Delete leftLength characters of text before the current caret
     * position, and delete rightLength characters of text after the current
     * cursor position.
     * <p>
     * Unlike paste or selectionDelete, does not signal the end of
     * text composing to the IME.
     */
    void deleteAroundComposingText(int left, int right) {
        int start = field.mCaretPosition - left;
        if (start < 0)
            start = 0;
        int end = field.mCaretPosition + right;
        int docLength = field.hDoc.getTextLength();
        if (end > docLength - 1) //exclude the terminal EOF
            end = docLength - 1;
        replaceComposingText(start, end - start, "");
    }

    String getTextAfterCursor(int maxLen) {
        int docLength = field.hDoc.getTextLength();
        if ((field.mCaretPosition + maxLen) > (docLength - 1))
		//exclude the terminal EOF
            return field.hDoc.subSequence(field.mCaretPosition, docLength - field.mCaretPosition - 1).toString();

        return field.hDoc.subSequence(field.mCaretPosition, maxLen).toString();
    }

    String getTextBeforeCursor(int maxLen) {
        int start = field.mCaretPosition - maxLen;
        if (start < 0)
            start = 0;
        return field.hDoc.subSequence(start, field.mCaretPosition - start).toString();
    }
}//end inner controller class
