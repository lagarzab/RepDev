package com.repdev;

import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import com.repdev.DatabaseLayout.Record;
import com.repdev.DatabaseLayout.Field;
import com.repdev.RepgenParser.Variable;
import com.repdev.RepgenParser.Token;

public class SyntaxHighlighter implements ExtendedModifyListener, LineStyleListener {
	private static final String FONT_NAME = "Courier New";
	private static final int FONT_SIZE = 11;

	private static final RGB BACKGROUND = new RGB(255, 255, 255), FOREGROUND = new RGB(0, 0, 0);
	private static final EStyle NORMAL = new EStyle(null, null), COMMENTS = new EStyle(new RGB(127, 127, 127), null), VARIABLES = new EStyle(new RGB(0, 0, 0), null, SWT.BOLD), FUNCTIONS = new EStyle(new RGB(0, 0, 255), null, SWT.BOLD),
			KEYWORDS = new EStyle(new RGB(0, 0, 255), null), TYPE_CHAR = new EStyle(new RGB(255, 0, 0), null), TYPE_DATE = new EStyle(new RGB(255, 0, 0), null, SWT.BOLD), STRUCT1 = new EStyle(new RGB(255, 0, 255), null), STRUCT2 = new EStyle(
					new RGB(255, 128, 255), null), STRUCT1_INVALID = new EStyle(new RGB(255, 0, 255), new RGB(128, 0, 0), SWT.NONE), STRUCT2_INVALID = new EStyle(new RGB(255, 128, 255), new RGB(128, 0, 0), SWT.NONE);

	private static final Color FORECOLOR = new Color(Display.getCurrent(), FOREGROUND), BACKCOLOR = new Color(Display.getCurrent(), BACKGROUND);
	private static final Font FONT;

	private RepgenParser parser;
	private StyledText txt;
	private SymitarFile file;
	private int sym;

	static {
		Font cur = null;

		try {
			cur = new Font(Display.getCurrent(), FONT_NAME, FONT_SIZE, SWT.NORMAL);
		} catch (Exception e) {
		}

		FONT = cur;
	}

	public SyntaxHighlighter(RepgenParser parser) {
		this.parser = parser;
		this.txt = parser.getTxt();
		this.file = parser.getFile();
		this.sym = parser.getSym();
		
	
		txt.setForeground(FORECOLOR);
		txt.setBackground(BACKCOLOR);
		txt.addExtendedModifyListener(this);
		txt.addLineStyleListener(this);
		if (FONT != null)
			txt.setFont(FONT);
	}

	private static class EStyle {
		private Color fcolor = null, bgcolor = null;
		private int style;

		public EStyle(RGB frgb, RGB bgrgb, int style) {
			if (frgb != null)
				fcolor = new Color(Display.getCurrent(), frgb);
			if (bgrgb != null)
				bgcolor = new Color(Display.getCurrent(), bgrgb);
			this.style = style;
		}

		public EStyle(RGB frgb, RGB bgrgb) {
			this(frgb, bgrgb, SWT.NORMAL);
		}

		public StyleRange getRange(int start, int len) {
			return new StyleRange(start, len, fcolor, bgcolor, style);
		}
	}

	public void modifyText(ExtendedModifyEvent e) {
		parser.textModified(e.start, e.length, e.replacedText);
	}
	
	public StyleRange getStyle(Token tok) {
		boolean isVar = false;

		if (tok.getCDepth() != 0)
			return COMMENTS.getRange(tok.getStart(), tok.length());
		else if (tok.getInString())
			return TYPE_CHAR.getRange(tok.getStart(), tok.length());
		else if (tok.getInDate())
			return TYPE_DATE.getRange(tok.getStart(), tok.length());
		else if (tok.getAfter() != null && tok.getAfter().getStr().equals(":")) {
			if (tok.dbRecordValid())
				return STRUCT1.getRange(tok.getStart(), tok.length());
			else
				return STRUCT1_INVALID.getRange(tok.getStart(), tok.length());
		} else if (tok.getBefore() != null && tok.getBefore().getStr().equals(":")) {
			if (tok.dbFieldValid(RepgenParser.getDb().getTreeRecords()))
				return STRUCT2.getRange(tok.getStart(), tok.length());
			else
				return STRUCT2_INVALID.getRange(tok.getStart(), tok.length());
		} else if (RepgenParser.getFunctions().contains(tok.getStr()) && tok.getAfter() != null && tok.getAfter().getStr().equals("("))
			return FUNCTIONS.getRange(tok.getStart(), tok.length());
		else if (RepgenParser.getKeywords().contains(tok.getStr()))
			return KEYWORDS.getRange(tok.getStart(), tok.length());
		else if (RepgenParser.getSpecialvars().contains(tok.getStr()))
			return VARIABLES.getRange(tok.getStart(), tok.length());

		for (Variable var : parser.getLvars()) {
			if (var.getName().equals(tok.getStr()))
				isVar = true;
		}

		if (isVar)
			return VARIABLES.getRange(tok.getStart(), tok.length());
		else
			return NORMAL.getRange(tok.getStart(), tok.length());
		
	}


	public void lineGetStyle(LineStyleEvent event) {
		ArrayList<Token> ltokens = parser.getLtokens();
		
		int line = txt.getLineAtOffset(event.lineOffset);

		int ftoken;
		for (ftoken = 0; ftoken < ltokens.size(); ftoken++)
			if (ltokens.get(ftoken).getEnd() >= event.lineOffset)
				break;

		int ltoken = ltokens.size();
		if (line + 1 < txt.getLineCount()) {
			int pos = txt.getOffsetAtLine(line + 1);

			for (ltoken = ftoken; ltoken < ltokens.size(); ltoken++)
				if (ltokens.get(ltoken).getStart() > pos)
					break;
		}

		StyleRange[] result = new StyleRange[ltoken - ftoken];
		for (int i = ftoken; i < ltoken; i++)
			result[i - ftoken] = getStyle(ltokens.get(i));

		event.styles = result;

	}

}