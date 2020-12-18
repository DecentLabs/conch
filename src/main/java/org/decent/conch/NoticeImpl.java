package org.decent.conch;

import java.util.Locale;

public final class NoticeImpl implements Notice {
    private final boolean error;
    private final int start;
    private final int end;
    private final String message;

    NoticeImpl(boolean error, int start, int end, String message) {
        this.error = error;
        this.start = start;
        this.end = end;
        this.message = message;
    }

    
    @Override
    public boolean isError() {
        return error;
    }
    @Override
    public int start() {
        return start;
    }

    @Override
    public int end() {
        return end;
    }

    @Override
    public String message(Locale locale) {
        return message;
    }

}