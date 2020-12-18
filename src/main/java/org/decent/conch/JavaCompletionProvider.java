package org.decent.conch;

import org.fife.ui.autocomplete.DefaultCompletionProvider;

public class JavaCompletionProvider extends DefaultCompletionProvider {

    private final static String[] KEYWORDS = { "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double", "else", "extends", "false", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try",
            "void", "var", "volatile", "while" };

    public JavaCompletionProvider() {
        super(KEYWORDS);
    }

}
