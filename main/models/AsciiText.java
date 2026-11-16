package models;

import java.util.Objects;

/**
 * ASCII helper functions shared by all modules.
 *
 * <p>The system only supports 7-bit ASCII text (D3, Non-goals). Case folding is therefore done
 * with plain ASCII rules: only {@code 'A'..'Z'} are mapped to {@code 'a'..'z'}; every other
 * character is left unchanged. This guarantees that folding never changes the length of a string
 * and that index {@code i} in the folded string corresponds to index {@code i} in the original
 * (S2). {@link String#toLowerCase()} without a locale must not be used for matching because it is
 * locale-dependent (for example the Turkish dotless i).</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe (stateless static methods).</p>
 */
public final class AsciiText {

    /** Highest code point that is ASCII. */
    public static final int MAX_ASCII = 0x7F;

    private AsciiText() {
        throw new AssertionError("AsciiText is a utility class and must not be instantiated");
    }

    /**
     * Folds one character to lower case using ASCII rules.
     *
     * @param character any character
     * @return {@code character + 32} if {@code character} is in {@code 'A'..'Z'}, otherwise
     *         {@code character} unchanged
     */
    public static char toLowerAscii(char character) {
        if (character >= 'A' && character <= 'Z') {
            return (char) (character + ('a' - 'A'));
        }
        return character;
    }

    /**
     * Folds a whole string to lower case using ASCII rules.
     *
     * <p>Postcondition: {@code result.length() == text.length()} and for every index {@code i},
     * {@code result.charAt(i) == toLowerAscii(text.charAt(i))}.</p>
     *
     * @param text the text to fold, not {@code null}
     * @return the folded text (may be the same instance if nothing changed)
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static String toLowerAscii(String text) {
        Objects.requireNonNull(text, "text");
        char[] folded = null;
        for (int index = 0; index < text.length(); index++) {
            char original = text.charAt(index);
            char lower = toLowerAscii(original);
            if (lower != original) {
                if (folded == null) {
                    folded = text.toCharArray();
                }
                folded[index] = lower;
            }
        }
        return folded == null ? text : new String(folded);
    }

    /**
     * Tells whether a byte value read from a file is ASCII (0x00 to 0x7F) (D3).
     *
     * @param byteValue a value as returned by {@code InputStream.read()} or a {@code byte}
     *                  widened to {@code int}
     * @return {@code true} if the low eight bits encode a value in 0x00..0x7F
     */
    public static boolean isAsciiByte(int byteValue) {
        return (byteValue & 0x80) == 0;
    }

    /**
     * Tells whether every character of the text is ASCII (0x00 to 0x7F).
     *
     * @param text the text to check, not {@code null}
     * @return {@code true} if all characters are ASCII; {@code true} for the empty string
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static boolean isAscii(CharSequence text) {
        Objects.requireNonNull(text, "text");
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) > MAX_ASCII) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether a character is printable ASCII, i.e. in the range {@code ' '} (0x20) to
     * {@code '~'} (0x7E). Used for title validation (decision N2).
     *
     * @param character any character
     * @return {@code true} if printable ASCII
     */
    public static boolean isPrintableAscii(char character) {
        return character >= 0x20 && character <= 0x7E;
    }
}
