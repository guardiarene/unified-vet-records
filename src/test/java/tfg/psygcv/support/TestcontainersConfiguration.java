package tfg.psygcv.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides the MySQL datasource for tests that load a Spring context.
 *
 * <p>The container is a bean, so it lives as long as the application context: test classes that
 * share the same cached context share one container. A test with a different context configuration
 * (for example, a slice test such as {@code @DataJpaTest}) starts its own container. Flyway applies
 * the migrations on startup and {@code ddl-auto: validate} checks the entities against them.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4.13");

  @Bean
  @ServiceConnection
  MySQLContainer mysqlContainer() {
    return new MySQLContainer(MYSQL_IMAGE);
  }
}
