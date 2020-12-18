package org.decent.conch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import jdk.jshell.DeclarationSnippet;
import jdk.jshell.EvalException;
import jdk.jshell.JShell;
import jdk.jshell.JShellException;
import jdk.jshell.Snippet.Status;
import jdk.jshell.execution.LocalExecutionControlProvider;

public class ConchPanel extends JPanel {

    static final SimpleAttributeSet errorStyle = new SimpleAttributeSet();
    static {
        StyleConstants.setForeground(errorStyle, Color.red);
        StyleConstants.setBold(errorStyle, true);
    }

    private final JShell shell;

    private final RSyntaxTextArea editor;
    private final JTextPane output;
    private final NoticeParser parser;

    public ConchPanel(JShell shell) {

        this.shell = shell;
        setLayout(new BorderLayout());

        editor = new RSyntaxTextArea(20, 60);
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        editor.setCodeFoldingEnabled(false);
        editor.setParserDelay(100);

        var scrollPane = new RTextScrollPane(editor);
        this.parser = new NoticeParser(editor);
        editor.addParser(parser);
        var errorStrip = new ErrorStrip(editor);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane);
        panel.add(errorStrip, BorderLayout.LINE_END);

        var split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setTopComponent(panel);

        output = new JTextPane();
        output.setEditable(false);
        output.setPreferredSize(new Dimension(400, 150));
        output.setBackground(Color.white);
        output.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                var el = output.getStyledDocument().getCharacterElement(output.viewToModel2D(e.getPoint()));
                var pos = (Integer) el.getAttributes().getAttribute("pos");
                if (pos != null) {
                    editor.setCaretPosition(pos);
                    editor.grabFocus();
                }
            }
        });
        split.setBottomComponent(new JScrollPane(output));
        add(split);

        var provider = new ConchCompletionProvider(this.shell);
        provider.setParent(new JavaCompletionProvider());
        var ac = new AutoCompletion(provider);
        ac.setAutoCompleteEnabled(true);
        ac.setParameterAssistanceEnabled(true);
        ac.setShowDescWindow(true);
        ac.setChoicesWindowSize(250, 200);
        ac.setDescriptionWindowSize(500, 250);
        ac.install(editor);

        int meta = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        var cmdEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, meta);
        editor.getInputMap(JComponent.WHEN_FOCUSED).put(cmdEnter, "run");
        editor.getActionMap().put("run", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                var code = editor.getSelectedText();
                int startPos;
                if (code == null) {
                    startPos = 0;
                    code = editor.getText();
                } else {
                    startPos = editor.getSelectionStart();
                }
                // output.setText("");
                append("\n", null);
                parser.clear();
                run(code, msg -> append(msg + "\n", null), notice -> {
                    var n = Notice.wrap(startPos, notice);
                    append(notice);
                    parser.add(n);
                });
                editor.forceReparsing(parser);
            }
        });
    }

    private void append(Notice notice) {
        try {
            int line = editor.getLineOfOffset(notice.end()) + 1;
            var msg = notice.message(getLocale());
            var style = notice.isError() ? errorStyle : null;
            var linkStyle = new SimpleAttributeSet(style);
            linkStyle.addAttribute("pos", notice.end());
            append("Line " + line + ": " + msg + "\n", linkStyle);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private void append(String text, AttributeSet style) {
        try {
            var doc = output.getStyledDocument();
            doc.insertString(doc.getLength(), text, style);
            output.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private void run(String source, Consumer<String> log, Consumer<Notice> notices) {
        var remainingSource = source;
        int currentPos = 0;
        do {
            var c = shell.sourceCodeAnalysis().analyzeCompletion(remainingSource);
            var snippet = c.source() == null ? remainingSource : c.source();
            int startPos = currentPos;
            int endPos = currentPos + snippet.length();
            if (!c.completeness().isComplete()) {
                notices.accept(Notice.error(startPos, endPos, "Incomplete input"));
                break;
            }

            var snippetEvents = shell.eval(snippet);

            for (var e : snippetEvents) {
                if (e.status() == Status.REJECTED) {
                    shell.diagnostics(e.snippet())
                            .forEach(diag -> notices.accept(Notice.wrap(startPos, new DiagNotice(diag))));
                    return;
                }
                if (e.value() != null) {
                    if (e.snippet() instanceof DeclarationSnippet) {
                        var ds = (DeclarationSnippet) e.snippet();
                        log.accept(ds.name() + " => " + e.value());
                    } else {
                        log.accept(e.value());
                    }
                }
                if (e.exception() != null) {
                    var sb = trace(e.exception());
                    notices.accept(Notice.error(startPos, endPos, sb));
                }
            }
            remainingSource = c.remaining();
            currentPos += snippet.length();
        } while (!remainingSource.isEmpty());
    }

    private String trace(JShellException exc) {
        var sb = new StringBuilder();
        if (exc instanceof EvalException) {
            sb.append(((EvalException) exc).getExceptionClassName());
            sb.append(": ");
        }
        sb.append(exc.getMessage());
        // TODO full stack trace
        return sb.toString();
    }

    public JShell shell() {
        return shell;
    }

    public static void main(String[] args) {
        var p = new LocalExecutionControlProvider();
        var shell = JShell.builder().executionEngine(p, p.defaultParameters()).in(System.in).out(System.out)
                .err(System.err).build();
        SwingUtilities.invokeLater(() -> {
            var conch = new ConchPanel(shell);
            var frame = new JFrame("Conch");
            frame.setContentPane(conch);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

}
