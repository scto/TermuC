package cn.rbc.termuc;
import android.os.*;
import cn.rbc.codeeditor.util.*;
import java.util.*;
import android.app.*;
import android.util.*;
import android.widget.*;
import java.io.*;
import cn.rbc.codeeditor.view.autocomplete.*;
import static android.util.JsonToken.*;
import java.nio.charset.*;

public class MainHandler extends Handler implements Comparator<ErrSpan> {
	private MainActivity ma;
	private static final String
	ADDEDIT = "additionalTextEdits",
	CAPA = "capabilities",
	COMPLE = "completionProvider",
	DG = "diagnostics",
	END = "end",
	IT = "items",
	KIND = "kind",
	LABEL = "label",
	L = "line",
	MSG = "message",
	NEWTX = "newText",
	PARA = "params",
	RNG = "range",
	RESU = "result",
	SEVE = "severity",
	TEDIT = "textEdit",
	TG = "triggerCharacters";

	public MainHandler(MainActivity ma) {
		super();
		this.ma = ma;
	}

	public void updateActivity(MainActivity ma) {
		this.ma = ma;
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
			case Lsp.INITIALIZE:
				ma.lsp.initialized();
				break;
			case Lsp.CLOSE:
				return;
		}
		try {
			JsonReader jr = (JsonReader)msg.obj;
			jr.beginObject();
			Deque<String> stack = new ArrayDeque<>();
			int sl = 0, sc = 0, el = 0, ec = 0;
			Object tmp1 = null, tmp2 = null, tmp3 = null;
	LOOP:	while (true) {
				switch (jr.peek()) {
					case NAME:
						String n = jr.nextName();
						switch (n) {
							case NEWTX:
								//if (tmp3 instanceof Edit)
								((Edit)tmp3).text = jr.nextString();
								break;
							case LABEL:
								if (tmp2 instanceof ListItem)
									((ListItem)tmp2).label = jr.nextString();
								break;
							case KIND:
								if (tmp2 instanceof ListItem)
									((ListItem)tmp2).kind = jr.nextInt();
								break;
							case MSG:
								if (tmp2 instanceof ErrSpan)
									((ErrSpan)tmp2).msg = jr.nextString();
								break;
							case SEVE:
								if (tmp2 instanceof ErrSpan)
									((ErrSpan)tmp2).severity = jr.nextInt() - 1;
								break;
							case IT:
							case DG:
								tmp1 = new ArrayList();
							case ADDEDIT:
								jr.beginArray();
								stack.push(n);
								break;
							case TG:
								jr.beginArray();
								StringBuilder sb = new StringBuilder();
								while (jr.hasNext())
									sb.append(jr.nextString());
								jr.close();
								ma.lsp.setCompTrigs(sb.toString().toCharArray());
								break LOOP;
							case RNG:
								jr.beginObject();
								while (jr.hasNext()) {
									String tp = jr.nextName();
									jr.beginObject();
									if (END.equals(tp))
										while (jr.hasNext())
											if (L.equals(jr.nextName()))
												el = jr.nextInt();
											else
												ec = jr.nextInt();
									else
										while (jr.hasNext())
											if (L.equals(jr.nextName()))
												sl = jr.nextInt();
											else
												sc = jr.nextInt();
									jr.endObject();
								}
								jr.endObject();
								break;
							case TEDIT:
								tmp3 = new Edit();
							case COMPLE:
							case CAPA:
							case PARA:
							case RESU:
								jr.beginObject();
								stack.push(n);
								break;
							default:
								jr.skipValue();
								break;
						}
						break;
					case BEGIN_OBJECT:
						jr.beginObject();
						if (!stack.isEmpty()) {
						switch (stack.peek()) {
							case ADDEDIT:
								tmp3 = new Edit();
								break;
							case IT:
								tmp2 = new ListItem();
								break;
							case DG:
								tmp2 = new ErrSpan();
								break;
						}
						}
						break;
					case END_OBJECT:
						jr.endObject();
						if (!stack.isEmpty())
						switch (stack.peek()) {
							case ADDEDIT:
								Edit _p = (Edit)tmp3;
								Document te = ma.getEditor().getText();
								_p.start = te.getLineOffset(sl) + sc;
								_p.len = te.getLineOffset(el) + ec - _p.start;
								((ListItem)tmp2).edits.addLast(_p);
								break;
							case TEDIT:
								_p = (Edit)tmp3;
								te = ma.getEditor().getText();
								_p.start = te.getLineOffset(sl) + sc;
								_p.len = te.getLineOffset(el) + ec - _p.start;
								((ListItem)tmp2).edits.addFirst(_p);
								stack.pop();
								break;
							case IT:
								((ArrayList)tmp1).add(tmp2);
								break;
							case DG:
								if (sc != ec || sl != el) {
									ErrSpan e = (ErrSpan)tmp2;
									e.stl = sl + 1;
									e.stc = sc;
									e.enl = el + 1;
									e.enc = ec;
									((ArrayList<ErrSpan>)tmp1).add(e);
								}
								break;
							default:
								stack.pop();
						}
						break;
					case END_ARRAY:
						jr.endArray();
						if (!stack.isEmpty())
						switch (stack.peek()) {
							case ADDEDIT:
								stack.pop();
								break;
							case IT:
								ma.getEditor().getAutoCompletePanel().update((ArrayList<ListItem>)tmp1);
								break LOOP;
							case DG:
								jr.close();
								ArrayList<ErrSpan> a = (ArrayList<ErrSpan>)tmp1;
								a.sort(this);
								TextEditor te = ma.getEditor();
								te.getText().setDiag(a);
								te.invalidate();
								break LOOP;
						}
						break;
					case END_DOCUMENT:
						jr.close();
						break LOOP;
					default:
						jr.skipValue();
				}
			}
		} catch (IOException j) {
			Log.e("LSP", j.getMessage());
		}
	}

	public int compare(ErrSpan p1, ErrSpan p2) {
		return p1.stl - p2.stl;
	}
}
