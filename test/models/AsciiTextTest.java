package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AsciiText} (S2). Owner: Eric. */
class AsciiTextTest {

    /** E-43: only A-Z are folded; the result does not depend on the default locale. */
    @Test
    @DisplayName("E-43 toLowerAscii folds only A-Z, keeps length, ignores the default locale")
    void toLowerAscii_mixedText_foldsOnlyAsciiLetters() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            String folded = AsciiText.toLowerAscii("TITLE_[Z]@ 9");

            assertEquals("title_[z]@ 9", folded);
            assertEquals('i', AsciiText.toLowerAscii('I'));
            String alreadyLower = "abc";
            assertSame(alreadyLower, AsciiText.toLowerAscii(alreadyLower));
        } finally {
            Locale.setDefault(original);
        }
    }

    /** E-44: ASCII detection for strings and bytes. */
    @Test
    @DisplayName("E-44 isAscii and isAsciiByte detect values above 0x7F")
    void isAscii_textWithNonAscii_returnsFalse() {
        assertTrue(AsciiText.isAscii(""));
        assertTrue(AsciiText.isAscii("plain\r\n\t~"));
        assertFalse(AsciiText.isAscii("na\u00efve"));
        assertTrue(AsciiText.isAsciiByte(0x7F));
        assertFalse(AsciiText.isAsciiByte((byte) 0xE9));
        assertFalse(AsciiText.isAsciiByte(0x80));
    }

    /** E-45: printable ASCII range is 0x20..0x7E. */
    @Test
    @DisplayName("E-45 isPrintableAscii accepts 0x20..0x7E only")
    void isPrintableAscii_boundaries_matchRange() {
        assertTrue(AsciiText.isPrintableAscii(' '));
        assertTrue(AsciiText.isPrintableAscii('~'));
        assertFalse(AsciiText.isPrintableAscii('\u007f'));
        assertFalse(AsciiText.isPrintableAscii('\t'));
    }
}
