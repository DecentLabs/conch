package org.decent.conch;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProviderBase;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.autocomplete.ParameterizedCompletion.Parameter;

import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.SourceCodeAnalysis.Documentation;
import jdk.jshell.SourceCodeAnalysis.Suggestion;

class ConchCompletionProvider extends CompletionProviderBase {

    private final JShell shell;

    public ConchCompletionProvider(JShell shell) {
        this.shell = shell;
        setParameterizedCompletionParams('(', ", ", ')');
    }

    @Override
    public String getAlreadyEnteredText(JTextComponent comp) {
        var info = CompInfo.from(shell.sourceCodeAnalysis(), comp);
        return info.prefix;
    }

    @Override
    public List<Completion> getCompletionsAt(JTextComponent comp, Point p) {
        System.out.println("getCompletionsAt " + p);
        return null;
    }

    @Override
    public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent tc) {
        System.out.println("getParameterizedCompletions");
        return null;
    }

    @Override
    protected List<Completion> getCompletionsImpl(JTextComponent comp) {
        var info = CompInfo.from(shell.sourceCodeAnalysis(), comp);
        var ret = new ArrayList<Completion>();
        if (info.documentation.isEmpty()) {

            var set = new LinkedHashSet<String>();
            info.completions.forEach(s -> set.add(s.continuation()));
            for (var suggestion : set) {
                var c = new BasicCompletion(this, suggestion);
                c.setRelevance(10);
                ret.add(c);
            }

        } else {
            for (var sig : info.signatures) {
                var fn = new FunctionCompletion(this, sig.name, sig.returnType) {
                    @Override
                    public String toString() {
                        return getDefinitionString();
                    }
                };
                if (!sig.fqn.equals(sig.name)) {
                    fn.setDefinedIn(sig.fqn);
                }
                // if (d.javadoc() != null) {
                // fn.setShortDescription("<pre>" + d.javadoc() + "</pre>");
                // }
                if (sig.arity > 0) {
                    var params = new ArrayList<Parameter>(sig.arity);
                    for (int i = 0; i < sig.arity; i++) {
                        boolean last = i + 1 == sig.arity;
                        params.add(new Parameter(sig.paramTypes[i], sig.paramNames[i], last));
                    }
                    fn.setParams(params);
                }
                fn.setRelevance(100);
                ret.add(fn);
            }
        }
        return ret;
    }

    static class CompInfo {

        final String text;
        final String prefix;
        final int cursor;
        final int replaceStart;
        final List<Suggestion> completions;
        final List<Documentation> documentation;
        final List<Signature> signatures;

        static CompInfo from(SourceCodeAnalysis analyzer, JTextComponent comp) {
            Document doc = comp.getDocument();
            String text;
            try {
                text = doc.getText(0, doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
                text = EMPTY_STRING;
            }
            return new CompInfo(analyzer, text, comp.getCaretPosition());
        }

        CompInfo(SourceCodeAnalysis analyzer, String text, int cursor) {
            this.text = text;
            this.cursor = cursor;
            int at = cursor;
            // while (at + 1 < text.length() &&
            // Character.isJavaIdentifierPart(text.charAt(at + 1)))
            // at++;

            int[] rep = new int[1];
            this.completions = analyzer.completionSuggestions(text, at, rep);
            this.replaceStart = rep[0];
            this.documentation = analyzer.documentation(text, at, false);
            if (documentation.isEmpty()) {
                signatures = List.of();
            } else {
                signatures = new ArrayList<>(documentation.size());
                for (var d : documentation) {
                    var sig = Signature.tryParse(d.signature());
                    if (sig != null) {
                        signatures.add(sig);
                    } else {
                        System.err.println("SKIP sig " + d.signature());
                    }
                }
            }

            var match = text.substring(replaceStart, at);
            if (match.isEmpty() && !signatures.isEmpty()) {
                // override for method doc completions
                match = signatures.get(0).name + '(';
            }
            this.prefix = match;
        }

    }

    static class Signature {
        final String[] tokens;
        final String fqn;
        final String name;
        final String returnType;
        final int arity;
        final String[] paramNames;
        final String[] paramTypes;

        private Signature(String sig) {
            this.tokens = sig.substring(0, sig.lastIndexOf(')')).split("[ \\(\\),]+");
            int base = tokens.length % 2 == 0 ? 2 : 1; // 1 for constructor, 2 for method
            this.returnType = base == 1 ? "" : tokens[0];
            this.fqn = tokens[base - 1];
            var fqnTokens = fqn.split("\\.");
            this.name = fqnTokens[fqnTokens.length - 1];
            this.arity = (tokens.length - base) / 2;
            this.paramNames = new String[arity];
            this.paramTypes = new String[arity];
            for (int i = 0; i < arity; i++) {
                int offs = base + (i * 2);
                paramTypes[i] = tokens[offs];
                paramNames[i] = tokens[offs + 1];
            }
        }

        public static Signature tryParse(String sig) {
            if (!sig.contains("(")) {
                return null;
            }
            return new Signature(sig);
        }

    }

}