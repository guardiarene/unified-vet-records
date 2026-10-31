package tfg.psygcv.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogSanitizerTest {

  @Test
  void should_maskEmail_when_standardEmail() {
    assertThat(LogSanitizer.maskEmail("juan@dominio.com")).isEqualTo("j***@dominio.com");
  }

  @Test
  void should_returnNull_when_nullEmail() {
    assertThat(LogSanitizer.maskEmail(null)).isNull();
  }

  @Test
  void should_returnEmpty_when_emptyEmail() {
    assertThat(LogSanitizer.maskEmail("")).isEmpty();
  }

  @Test
  void should_maskWithoutAt_when_noAtSign() {
    assertThat(LogSanitizer.maskEmail("secretvalue")).isEqualTo("s***");
  }

  @Test
  void should_maskSingleCharLocal() {
    assertThat(LogSanitizer.maskEmail("a@dominio.com")).isEqualTo("a***@dominio.com");
  }

  @Test
  void should_sanitizeControlCharsInMaskedResult() {
    assertThat(LogSanitizer.maskEmail("j\r\nuan@dominio.com")).isEqualTo("j***@dominio.com");
  }

  @Test
  void should_maskWholeValue_when_domainContainsInjectedText() {
    String result = LogSanitizer.maskEmail("x@example.com FORGED Successful login");
    assertThat(result).isEqualTo("x***").doesNotContain("FORGED");
  }

  @Test
  void should_truncateLongMaskedResult() {
    String longDomain = "a".repeat(200) + ".com";
    String result = LogSanitizer.maskEmail("x@" + longDomain);
    assertThat(result).hasSize(100);
  }

  @Test
  void should_sanitizeText_removingControlChars() {
    assertThat(LogSanitizer.sanitize("line1\r\nline2")).isEqualTo("line1line2");
  }

  @Test
  void should_returnNull_when_nullText() {
    assertThat(LogSanitizer.sanitize(null)).isNull();
  }
}
