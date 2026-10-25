package tfg.psygcv.config.time;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  @Bean
  public Clock clock(@Value("${app.time-zone}") String timeZone) {
    return Clock.system(ZoneId.of(timeZone));
  }

  @Bean
  public ValidationConfigurationCustomizer clockProviderCustomizer(Clock clock) {
    return configuration -> configuration.clockProvider(() -> clock);
  }
}
