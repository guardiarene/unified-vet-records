package tfg.psygcv.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MessageKeysTest {

  private static Properties messages;

  @BeforeAll
  static void loadMessages() throws IOException {
    messages = new Properties();
    try (InputStream in = MessageKeysTest.class.getResourceAsStream("/messages.properties")) {
      assertThat(in).as("messages.properties on the classpath").isNotNull();
      try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        messages.load(reader);
      }
    }
  }

  @Test
  void should_declareKeys_when_classIsLoaded() {
    assertThat(keyConstants()).isNotEmpty();
  }

  @Test
  void should_haveNonBlankMessage_when_keyIsDeclaredInMessageKeys() {
    for (String key : keyConstants()) {
      assertThat(messages.getProperty(key))
          .as("message for key '%s' in messages.properties", key)
          .isNotBlank();
    }
  }

  @Test
  void should_decodeAsUtf8_when_messagesContainAccents() {
    // A file saved in another encoding decodes with replacement characters under UTF-8.
    for (String key : keyConstants()) {
      assertThat(messages.getProperty(key))
          .as("message for key '%s' decoded as UTF-8", key)
          .doesNotContain("�");
    }
  }

  private static List<String> keyConstants() {
    return Arrays.stream(MessageKeys.class.getDeclaredFields())
        .filter(MessageKeysTest::isStringConstant)
        .map(MessageKeysTest::valueOf)
        .toList();
  }

  private static boolean isStringConstant(Field field) {
    int modifiers = field.getModifiers();
    return Modifier.isPublic(modifiers)
        && Modifier.isStatic(modifiers)
        && Modifier.isFinal(modifiers)
        && field.getType() == String.class;
  }

  private static String valueOf(Field field) {
    try {
      return (String) field.get(null);
    } catch (IllegalAccessException ex) {
      throw new IllegalStateException("Cannot read " + field.getName(), ex);
    }
  }
}
