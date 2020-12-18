package org.decent.conch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level;

public class NoticeParser extends AbstractParser {

    private final RSyntaxTextArea textArea;
    private final List<Notice> notices = new ArrayList<>();

    public NoticeParser(RSyntaxTextArea textArea) {
        this.textArea = textArea;
        textArea.getDocument().addDocumentListener(onDocumentChange(e -> clear()));
    }

    @Override
    public ParseResult parse(RSyntaxDocument doc, String style) {
        var res = new DefaultParseResult(this);
        for (var n : notices) {
            try {
                int line = textArea.getLineOfOffset(n.start());
                var notice = new DefaultParserNotice(this, n.message(null), line, n.start(), n.length());
                notice.setLevel(n.isError() ? Level.ERROR : Level.WARNING);
                res.addNotice(notice);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }

    public void clear() {
        notices.clear();
    }

    public void add(Notice notice) {
        this.notices.add(notice);
    }

    private static DocumentListener onDocumentChange(Consumer<DocumentEvent> consumer) {
        return new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                consumer.accept(e);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                consumer.accept(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                consumer.accept(e);
            }

        };
    }

}
