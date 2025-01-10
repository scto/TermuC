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

import java.util.List;
import java.util.Vector;
import java.util.stream.*;
import cn.rbc.codeeditor.common.*;


//TODO Have all methods work with charOffsets and move all gap handling to logicalToRealIndex()
public class TextBuffer implements CharSequence
{

	// gap size must be > 0 to insert into full buffers successfully
	protected final static int MIN_GAP_SIZE = 50;
	protected char[] _contents;
	protected int _gapStartIndex;
	/** One past end of gap */
	protected int _gapEndIndex;
	protected int _lineCount;
	/** The number of times memory is allocated for the buffer */
	private int _allocMultiplier;
	private TextBufferCache _cache;
	private UndoStack _undoStack;

	/** Continuous seq of chars that have the same format (color, font, etc.) */
	protected List<Pair> _spans;
	protected List<ErrSpan> _diag;
	protected GapIntSet _marks;

	OnTextChangeListener _txLis;
	private boolean _typ;

	public TextBuffer() {
		_contents = new char[MIN_GAP_SIZE + 1]; // extra char for EOF
		_contents[MIN_GAP_SIZE] = Language.EOF;
		_allocMultiplier = 1;
		_gapStartIndex = 0;
		_gapEndIndex = MIN_GAP_SIZE;
		_lineCount = 1;
		_cache = new TextBufferCache();
		_undoStack = new UndoStack(this);
		_marks = new GapIntSet();
		_typ = false;
	}

	/**
	 * Calculate the implementation size of the char array needed to store
	 * textSize number of characters.
	 * The implementation size may be greater than textSize because of buffer
	 * space, cached characters and so on.
	 *
	 * @param textSize
	 * @return The size, measured in number of chars, required by the
	 * 		implementation to store textSize characters, or -1 if the request
	 * 		cannot be satisfied
	 */
	public static int memoryNeeded(int textSize){
		long bufferSize = textSize + MIN_GAP_SIZE + 1; // extra char for EOF
		if (bufferSize < Integer.MAX_VALUE)
			return (int) bufferSize;
		return -1;
	}

	synchronized public void setBuffer(char[] newBuffer, int textSize, int lineCount) {
		_contents = newBuffer;
		initGap(textSize);
		_lineCount = lineCount;
		_allocMultiplier = 1;
		_marks.clear();
	}

	synchronized public void setBuffer(char[] newBuffer){
		int lineCount=1;
		int len=newBuffer.length;
		for (int i=0;i<len;i++)
			if (newBuffer[i]=='\n')
				lineCount++;
		setBuffer(newBuffer,len,lineCount);
	}


	/**
	 * Returns a string of text corresponding to the line with index lineNumber.
	 *
	 * @param lineNumber The index of the line of interest
	 * @return The text on lineNumber, or an empty string if the line does not exist
	 */
	synchronized public String getLine(int lineNumber){
		int startIndex = getLineOffset(lineNumber);

		if (startIndex < 0)
			return new String();
		int lineSize = getLineSize(lineNumber);

		return subSequence(startIndex, lineSize).toString();
	}

	/**
	 * Get the offset of the first character of the line with index lineNumber.
	 * The offset is counted from the beginning of the text.
	 *
	 * @param lineNumber The index of the line of interest
	 * @return The character offset of lineNumber, or -1 if the line does not exist
	 */
	synchronized public int getLineOffset(int lineNumber){
		if (lineNumber < 0)
			return -1;

		// start search from nearest known lineIndex~charOffset pair
		Pair cachedEntry = _cache.getNearestLine(lineNumber);
		int cachedLine = cachedEntry.first;
		int cachedOffset = cachedEntry.second;

		int offset;
		if (lineNumber > cachedLine)
			offset = findCharOffset(lineNumber, cachedLine, cachedOffset);
		else if (lineNumber < cachedLine)
			offset = findCharOffsetBackward(lineNumber, cachedLine, cachedOffset);
		else
			offset = cachedOffset;

		if (offset >= 0)
			// seek successful
			_cache.updateEntry(lineNumber, offset);

		return offset;
	}

	public IntStream chars() {
		return null;
	}

	public IntStream codePoints() {
		return null;
	}

	/*
	 * Precondition: startOffset is the offset of startLine
	 */
	private int findCharOffset(int targetLine, int startLine, int startOffset){
		int workingLine = startLine;
		int offset = logicalToRealIndex(startOffset);

		TextWarriorException.assertVerbose(isValid(startOffset),
			"findCharOffsetBackward: Invalid startingOffset given");

		while((workingLine < targetLine) && (offset < _contents.length)){
			if (_contents[offset] == Language.NEWLINE)
				++workingLine;
			++offset;

			// skip the gap
			if (offset == _gapStartIndex)
				offset = _gapEndIndex;
		}

		if (workingLine != targetLine)
			return -1;
		return realToLogicalIndex(offset);
	}

	/*
	 * Precondition: startOffset is the offset of startLine
	 */
	private int findCharOffsetBackward(int targetLine, int startLine, int startOffset){
		if (targetLine == 0)
			return 0;

		TextWarriorException.assertVerbose(isValid(startOffset),
			"findCharOffsetBackward: Invalid startOffset given");

		int workingLine = startLine;
		int offset = logicalToRealIndex(startOffset);
		while(workingLine > (targetLine-1) && offset >= 0){
			// skip behind the gap
			if(offset == _gapEndIndex)
				offset = _gapStartIndex;
			offset--;

			if (_contents[offset] == Language.NEWLINE)
				workingLine--;
		}

		int charOffset;
		if (offset >= 0){
			// now at the '\n' of the line before targetLine
			charOffset = realToLogicalIndex(offset);
			++charOffset;
		} else {
			TextWarriorException.assertVerbose(false,
				"findCharOffsetBackward: Invalid cache entry or line arguments");
			charOffset = -1;
		}

		return charOffset;
	}

	/**
	 * Get the line number that charOffset is on
	 *
	 * @return The line number that charOffset is on, or -1 if charOffset is invalid
	 */
	synchronized public int findLineNumber(int charOffset){
		if (!isValid(charOffset))
			return -1;

		Pair cachedEntry = _cache.getNearestCharOffset(charOffset);
		int line = cachedEntry.first;
		int offset = logicalToRealIndex(cachedEntry.second);
		int targetOffset = logicalToRealIndex(charOffset);
		int lastKnownLine = -1;
		int lastKnownCharOffset = -1;

		if (targetOffset > offset){
			// search forward
			while ((offset < targetOffset) && (offset < _contents.length)){
				if (_contents[offset] == Language.NEWLINE){
					++line;
					lastKnownLine = line;
					lastKnownCharOffset = realToLogicalIndex(offset) + 1;
				}

				++offset;
				// skip the gap
				if (offset == _gapStartIndex)
					offset = _gapEndIndex;
			}
		} else if (targetOffset < offset)
			// search backward
			while((offset > targetOffset) && (offset > 0)){
				// skip behind the gap
				if(offset == _gapEndIndex)
					offset = _gapStartIndex;
				offset--;

				if (_contents[offset] == Language.NEWLINE){
					lastKnownLine = line;
					lastKnownCharOffset = realToLogicalIndex(offset) + 1;
					line--;
				}
			}

		if (offset == targetOffset) {
			if(lastKnownLine != -1)
				// cache the lookup entry
				_cache.updateEntry(lastKnownLine, lastKnownCharOffset);
			return line;
		} else
			return -1;
	}


	/**
	 * Finds the number of char on the specified line.
	 * All valid lines contain at least one char, which may be a non-printable
	 * one like \n, \t or EOF.
	 *
	 * @return The number of chars in lineNumber, or 0 if the line does not exist.
	 */
	synchronized public int getLineSize(int lineNumber){
		int lineLength = 0;
		int pos = getLineOffset(lineNumber);

		if (pos != -1) {
			pos = logicalToRealIndex(pos);
			//TODO consider adding check for (pos < _contents.length) in case EOF is not properly set
			while (_contents[pos] != Language.NEWLINE &&
			 _contents[pos] != Language.EOF) {
				++lineLength;
				++pos;

				// skip the gap
				if (pos == _gapStartIndex)
					pos = _gapEndIndex;
			}
			++lineLength; // account for the line terminator char
		}

		return lineLength;
	}

	/**
	 * Gets the char at charOffset
	 * Does not do bounds-checking.
	 *
	 * @return The char at charOffset. If charOffset is invalid, the result
	 * 		is undefined.
	 */
	synchronized public char charAt(int charOffset){
		return _contents[logicalToRealIndex(charOffset)];
	}

	/**
	 * Gets up to maxChars number of chars starting at charOffset
	 *
	 * @return The chars starting from charOffset, up to a maximum of maxChars.
	 * 		An empty array is returned if charOffset is invalid or maxChars is
	 *		non-positive.
	 */
	synchronized public CharSequence subSequence(int charOffset, int maxChars){
		if (!isValid(charOffset) || maxChars <= 0)
			return new String();
		int totalChars = maxChars;
		if ((charOffset + totalChars) > length())
			totalChars = length() - charOffset;
		int realIndex = logicalToRealIndex(charOffset);
		char[] chars = new char[totalChars];

		for (int i = 0; i < totalChars; ++i){
			chars[i] = _contents[realIndex];
			++realIndex;
			// skip the gap
			if(realIndex == _gapStartIndex)
				realIndex = _gapEndIndex;
		}

		return new String(chars);
	}

	/**
	 * Gets charCount number of consecutive characters starting from _gapStartIndex.
	 *
	 * Only UndoStack should use this method. No error checking is done.
	 */
	char[] gapSubSequence(int charCount){
		char[] chars = new char[charCount];

		for (int i = 0; i < charCount; ++i)
			chars[i] = _contents[_gapStartIndex + i];

		return chars;
	}

	public void markLine(int l) {
		_marks.toggle(l);
	}

	public int[] getMarks() {
		return _marks.getData();
	}

	public int getMarksCount() {
		return _marks.size();
	}

	public int getMark(int i) {
		return _marks.get(i);
	}

	public int findMark(int m) {
		return _marks.find(m);
	}

	public boolean isInMarkGap(int l) {
		return _marks.isInGap(l);
	}
	/**
	 * Insert all characters in c into position charOffset.
	 *
	 * No error checking is done
	 */
	public synchronized void insert(char[] c, int charOffset, long timestamp,
			boolean undoable){
		if (undoable) {
			_undoStack.captureInsert(charOffset, c.length, timestamp);
			if (_txLis != null) {
				_txLis.onChanged(new String(c), charOffset, true, _typ);
				_typ = false;
			}
		}

		int insertIndex = logicalToRealIndex(charOffset);

		// shift gap to insertion point
		if (insertIndex<_gapStartIndex)
			shiftGapLeft(insertIndex);
		else if (insertIndex>_gapStartIndex)
			shiftGapRight(insertIndex);

		if (c.length >= gapSize())
			growBufferBy(c.length - gapSize());

		int lines = 0;
		for (int i = 0; i < c.length; ++i){
			if(c[i] == Language.NEWLINE)
				lines++;
			_contents[_gapStartIndex++] = c[i];
		}
		_lineCount += lines;
		if (lines != 0)
			_marks.shift(findLineNumber(charOffset)+1, lines);
		_cache.invalidateCache(charOffset);
	}

	/**
	 * Deletes up to totalChars number of char starting from position
	 * charOffset, inclusive.
	 *
	 * No error checking is done
	 */
	public synchronized void delete(int charOffset, int totalChars, long timestamp,
			boolean undoable){
		if (undoable) {
			_undoStack.captureDelete(charOffset, totalChars, timestamp);
			if (_txLis != null) {
				_txLis.onChanged(subSequence(charOffset, totalChars), charOffset, false, _typ);
				_typ = false;
			}
		}

		int newGapStart = charOffset + totalChars;

		// shift gap to deletion point
		if (newGapStart != _gapStartIndex){
			if (newGapStart<_gapStartIndex)
				shiftGapLeft(newGapStart);
			else
				shiftGapRight(newGapStart + gapSize());
		}
		// increase gap size
		int lines = 0;
		for(int i = 0; i < totalChars; ++i){
			if (_contents[--_gapStartIndex] == Language.NEWLINE)
				lines--;
		}
		_lineCount += lines;
        if (lines != 0)
            _marks.shift(findLineNumber(newGapStart)+1, lines);
		_cache.invalidateCache(charOffset);
	}

	/**
	 * Moves _gapStartIndex by displacement units. Note that displacement can be
	 * negative and will move _gapStartIndex to the left.
	 *
	 * Only UndoStack should use this method to carry out a simple undo/redo
	 * of insertions/deletions. No error checking is done.
	 */
	synchronized void shiftGapStart(int displacement){
        int lines;
		if(displacement >= 0)
			lines = countNewlines(_gapStartIndex, displacement);
		else
			lines = -countNewlines(_gapStartIndex + displacement, -displacement);

        if (lines != 0)
            _marks.shift(findLineNumber(_gapStartIndex)+1, lines);
        _lineCount += lines;
		_gapStartIndex += displacement;
		_cache.invalidateCache(realToLogicalIndex(_gapStartIndex - 1) + 1);
	}

	//does NOT skip the gap when examining consecutive positions
	private int countNewlines(int start, int totalChars){
		int newlines = 0;
		for(int i = start; i < (start + totalChars); ++i)
			if(_contents[i] == Language.NEWLINE)
				++newlines;

		return newlines;
	}

	/**
	 * Adjusts gap so that _gapStartIndex is at newGapStart
	 */
	final protected void shiftGapLeft(int newGapStart){
		while(_gapStartIndex > newGapStart){
			--_gapEndIndex;
			--_gapStartIndex;
			_contents[_gapEndIndex] = _contents[_gapStartIndex];
		}
	}

	/**
	 * Adjusts gap so that _gapEndIndex is at newGapEnd
	 */
	final protected void shiftGapRight(int newGapEnd){
		while(_gapEndIndex < newGapEnd){
			_contents[_gapStartIndex] = _contents[_gapEndIndex];
			++_gapStartIndex;
			++_gapEndIndex;
		}
	}

	/**
	 * Create a gap at the start of _contents[] and tack a EOF at the end.
	 * Precondition: real contents are from _contents[0] to _contents[contentsLength-1]
	 */
	protected void initGap(int contentsLength){
		int toPosition = _contents.length - 1;
		_contents[toPosition--] = Language.EOF; // mark end of file
		int fromPosition = contentsLength - 1;
		while(fromPosition >= 0){
			_contents[toPosition--] = _contents[fromPosition--];
		}
		_gapStartIndex = 0;
		_gapEndIndex = toPosition + 1; // went one-past in the while loop
	}

	public void setTyping(boolean tp) {
		_typ = tp;
	}
	/**
	 * Copies _contents into a buffer that is larger by
	 * 		minIncrement + INITIAL_GAP_SIZE * _allocCount bytes.
	 *
	 * _allocMultiplier doubles on every call to this method, to avoid the
	 * overhead of repeated allocations.
	 */
	protected void growBufferBy(int minIncrement){
		//TODO handle new size > MAX_INT or allocation failure
		int increasedSize = minIncrement + MIN_GAP_SIZE * _allocMultiplier;
		char[] temp = new char[_contents.length + increasedSize];
		int i = 0;
		while(i < _gapStartIndex){
			temp[i] = _contents[i];
			++i;
		}

		i = _gapEndIndex;
		while(i < _contents.length){
			temp[i + increasedSize] = _contents[i];
			++i;
		}

		_gapEndIndex += increasedSize;
		_contents = temp;
		_allocMultiplier <<= 1;
	}

	/**
	 * Returns the total number of characters in the text, including the
	 * EOF sentinel char
	 */
	 @Override
	final synchronized public int length(){
		return _contents.length - gapSize();
	}

	synchronized public int getLineCount(){
		return _lineCount;
	}

	final synchronized public boolean isValid(int charOffset){
		return (charOffset >= 0 && charOffset < length());
	}

	final protected int gapSize(){
		return _gapEndIndex - _gapStartIndex;
	}

	final protected int logicalToRealIndex(int i){
		return i<_gapStartIndex ? i : i + gapSize();
	}

	final protected int realToLogicalIndex(int i){
		return i<_gapStartIndex ? i : i - gapSize();
	}

	public void clearSpans(){
		_spans = new Vector<Pair>();
	    _spans.add(new Pair(0, Tokenizer.NORMAL));
	}

	public List<Pair> getSpans(){
		return _spans;
	}

	/**
	 * Sets the spans to use in the document.
	 * Spans are continuous sequences of characters that have the same format
	 * like color, font, etc.
	 *
	 * @param spans A collection of Pairs, where Pair.first is the start
	 * 		position of the token, and Pair.second is the type of the token.
	 */
	public void setSpans(List<Pair> spans){
		_spans = spans;
	}

	public List<ErrSpan> getDiag(){
		return _diag;
	}

	public void setDiag(List<ErrSpan> diag) {
		_diag = diag;
	}
	/**
	 * Returns true if in batch edit mode
	 */
	public boolean isBatchEdit(){
		return _undoStack.isBatchEdit();
	}

	/**
	 * Signals the beginning of a series of insert/delete operations that can be
	 * undone/redone as a single unit
	 */
	public void beginBatchEdit() {
		_undoStack.beginBatchEdit();
	}

	/**
	 * Signals the end of a series of insert/delete operations that can be
	 * undone/redone as a single unit
	 */
	public void endBatchEdit() {
		_undoStack.endBatchEdit();
	}

	public boolean canUndo() {
		return _undoStack.canUndo();
	}

	public boolean canRedo() {
		return _undoStack.canRedo();
	}

	public int undo(){
		return _undoStack.undo();
	}

	public int redo(){
		return _undoStack.redo();
	}

	@Override
	public String toString() {
		int len=length();
		StringBuffer buf=new StringBuffer();
		for (int i=0; i < len;i++) {
			char c=charAt(i);
			if (c==Language.EOF)
				break;
			buf.append(c);
		}
		return buf.toString();
	}

	public void setOnTextChangeListener(OnTextChangeListener txLis) {
		_txLis = txLis;
	}
}
