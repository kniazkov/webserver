/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

/**
 * Contains low-level constants and utility methods used while parsing
 * HTTP messages.
 */
final class Lexer {

    /**
     * Carriage return character.
     */
    static final char CR = '\r';

    /**
     * Line feed character.
     */
    static final char LF = '\n';

    /**
     * Space character.
     */
    static final char SP = ' ';

    /**
     * Horizontal tab character.
     */
    static final char HTAB = '\t';

    /**
     * Null character.
     */
    static final char NUL = '\0';

    /**
     * HTTP line separator.
     */
    static final String CRLF = "\r\n";

    /**
     * Prevents instantiation.
     */
    private Lexer() {
    }

    /**
     * Returns whether the specified character is allowed in an HTTP token.
     *
     * @param ch
     *     the character.
     * @return
     *     {@code true} if the character is allowed in an HTTP token.
     */
    static boolean isTokenCharacter(final char ch) {
        return isAlphaNumeric(ch)
            || ch == '!'
            || ch == '#'
            || ch == '$'
            || ch == '%'
            || ch == '&'
            || ch == '\''
            || ch == '*'
            || ch == '+'
            || ch == '-'
            || ch == '.'
            || ch == '^'
            || ch == '_'
            || ch == '`'
            || ch == '|'
            || ch == '~';
    }

    /**
     * Returns whether the specified character is an ASCII letter or digit.
     *
     * @param ch
     *     the character.
     * @return
     *     {@code true} if the character is an ASCII letter or digit.
     */
    static boolean isAlphaNumeric(final char ch) {
        return ch >= '0' && ch <= '9'
            || ch >= 'A' && ch <= 'Z'
            || ch >= 'a' && ch <= 'z';
    }

    /**
     * Returns whether the specified character is optional whitespace.
     *
     * @param ch
     *     the character.
     * @return
     *     {@code true} if the character is a space or horizontal tab.
     */
    static boolean isWhitespace(final char ch) {
        return ch == SP || ch == HTAB;
    }

    /**
     * Converts an ASCII character to upper case.
     *
     * @param ch
     *     the character.
     * @return
     *     the converted character.
     */
    public static char toUpperCase(final char ch) {
        if (ch >= 'a' && ch <= 'z') {
            return (char) (ch - 'a' + 'A');
        }
        return ch;
    }

    /**
     * Converts an ASCII character to lower case.
     *
     * @param ch
     *     the character.
     * @return
     *     the converted character.
     */
    public static char toLowerCase(final char ch) {
        if (ch >= 'A' && ch <= 'Z') {
            return (char) (ch - 'A' + 'a');
        }
        return ch;
    }


    /**
     * Converts an HTTP header name to its canonical form.
     * <p>
     * The first character and every character following a hyphen are converted
     * to upper case. All other characters are converted to lower case.
     *
     * @param value
     *     the header name.
     * @return
     *     the canonical header name.
     */
    static String canonicalizeHeaderName(final String value) {
        final StringBuilder result = new StringBuilder(value.length());
        boolean upperCase = true;

        for (int index = 0; index < value.length(); index++) {
            final char ch = value.charAt(index);

            if (ch == '-') {
                result.append(ch);
                upperCase = true;
            } else if (upperCase) {
                result.append(toUpperCase(ch));
                upperCase = false;
            } else {
                result.append(toLowerCase(ch));
            }
        }

        return result.toString();
    }
}
