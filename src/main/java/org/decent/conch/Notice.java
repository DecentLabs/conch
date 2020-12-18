package org.decent.conch;

import java.util.Locale;

public interface Notice {

    boolean isError();

    int start();

    int end();

    String message(Locale locale);

    default int length() {
        return end() - start();
    }

    static Notice info(int start, int end, String message) {
        return new NoticeImpl(false, start, end, message);
    }

    static Notice error(int start, int end, String message) {
        return new NoticeImpl(true, start, end, message);
    }

    static Notice wrap(int start, Notice notice) {
        return new Notice() {

            @Override
            public boolean isError() {
                return notice.isError();
            }

            @Override
            public String message(Locale locale) {
                return notice.message(locale);
            }

            @Override
            public int start() {
                return start + notice.start();
            }

            @Override
            public int end() {
                return start + notice.start();
            }

        };

    }

}
