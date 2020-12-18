package org.decent.conch;

import java.util.Locale;

import jdk.jshell.Diag;

final class DiagNotice implements Notice {

    private final Diag diag;

    public DiagNotice(Diag diag) {
        this.diag = diag;
    }

    @Override
    public boolean isError() {
        return diag.isError();
    }

    @Override
    public int start() {
        return Math.toIntExact(diag.getStartPosition());
    }

    @Override
    public int end() {
        return Math.toIntExact(diag.getEndPosition());
    }

    @Override
    public String message(Locale locale) {
        return diag.getMessage(locale);
    }

}