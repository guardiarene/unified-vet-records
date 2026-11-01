package tfg.psygcv.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.exception.domain.DuplicateEmailException;
import tfg.psygcv.exception.domain.ResourceNotFoundException;
import tfg.psygcv.exception.domain.UnauthorizedClinicAccessException;

class GlobalExceptionHandlerTest {

  private static final Locale SPANISH = Locale.of("es");
  private static final String FORM_PATH = "/appointments/new";

  private static final String BUSINESS_GENERIC =
      "No se pudo completar la operación. Revise los datos e intente nuevamente.";
  private static final String NOT_FOUND_GENERIC =
      "El elemento solicitado no existe o ya no está disponible.";

  private GlobalExceptionHandler handler;
  private MockHttpServletResponse response;
  private ExtendedModelMap model;
  private RedirectAttributesModelMap redirectAttributes;

  @BeforeEach
  void setUp() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setFallbackToSystemLocale(false);
    handler = new GlobalExceptionHandler(messageSource);
    response = new MockHttpServletResponse();
    model = new ExtendedModelMap();
    redirectAttributes = new RedirectAttributesModelMap();
  }

  @Nested
  class ResourceNotFound {

    @Test
    void should_redirectToRefererWithGenericFlash_when_post() {
      String view = handler.handleResourceNotFound(
          new ResourceNotFoundException("Pet not found", 42L),
          post(FORM_PATH), response, SPANISH, redirectAttributes);

      assertThat(view).isEqualTo("redirect:" + FORM_PATH);
      assertThat(flashError()).isEqualTo(NOT_FOUND_GENERIC);
    }

    @Test
    void should_render404WithStatus404_when_get() {
      String view = handler.handleResourceNotFound(
          new ResourceNotFoundException("Pet not found", 42L),
          get(), response, SPANISH, redirectAttributes);

      assertThat(view).isEqualTo("error/404");
      assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
      assertThat(redirectAttributes.getFlashAttributes()).isEmpty();
    }
  }

  @Nested
  class BusinessRule {

    @Test
    void should_flashResolvedMessage_when_postAndExceptionHasKey() {
      String view = handleBusinessRule(
          new BusinessRuleException(MessageKeys.APPOINTMENT_DATE_PAST, "Date in the past"),
          post(FORM_PATH));

      assertThat(view).isEqualTo("redirect:" + FORM_PATH);
      assertThat(flashError()).isEqualTo("La fecha del turno no puede ser anterior a hoy.");
    }

    @Test
    void should_formatArguments_when_messageHasPlaceholder() {
      handleBusinessRule(
          new BusinessRuleException(MessageKeys.USER_PASSWORD_TOO_SHORT, "Too short", 8),
          post(FORM_PATH));

      assertThat(flashError()).isEqualTo("La contraseña debe tener al menos 8 caracteres.");
    }

    @Test
    void should_flashGenericMessage_when_postAndExceptionHasNoKey() {
      handleBusinessRule(
          new BusinessRuleException("Invalid ID: must be positive and not null"),
          post(FORM_PATH));

      assertThat(flashError()).isEqualTo(BUSINESS_GENERIC);
    }

    @Test
    void should_flashGenericMessage_when_keyDoesNotExist() {
      handleBusinessRule(
          new BusinessRuleException("missing.key", "Internal message"), post(FORM_PATH));

      assertThat(flashError()).isEqualTo(BUSINESS_GENERIC);
    }

    @Test
    void should_flashResolvedMessage_when_duplicateEmail() {
      handleBusinessRule(
          new DuplicateEmailException(MessageKeys.USER_EMAIL_DUPLICATE, "Duplicate email"),
          post(FORM_PATH));

      assertThat(flashError()).isEqualTo("Ya existe una cuenta registrada con este email.");
    }

    @Test
    void should_render400WithResolvedMessage_when_getAndExceptionHasKey() {
      String view = handleBusinessRule(
          new BusinessRuleException(MessageKeys.USER_CLINIC_NONE, "No clinic for ID: 7"), get());

      assertThat(view).isEqualTo("error/400");
      assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(model.getAttribute("errorMessage"))
          .isEqualTo("El usuario no tiene una clínica asociada.");
      assertThat(redirectAttributes.getFlashAttributes()).isEmpty();
    }

    @Test
    void should_render400WithGenericMessage_when_getAndExceptionHasNoKey() {
      handleBusinessRule(new BusinessRuleException("Schedule command cannot be null"), get());

      assertThat(model.getAttribute("errorMessage")).isEqualTo(BUSINESS_GENERIC);
    }
  }

  @Nested
  class MissingParameter {

    @Test
    void should_redirectWithGenericFlash_when_post() {
      String view = handler.handleMissingParameter(
          new MissingServletRequestParameterException("startDate", "LocalDate"),
          post("/reports/appointments"), response, SPANISH, model, redirectAttributes);

      assertThat(view).isEqualTo("redirect:/reports/appointments");
      assertThat(flashError()).isEqualTo(BUSINESS_GENERIC);
    }

    @Test
    void should_render400WithGenericMessage_when_get() {
      String view = handler.handleMissingParameter(
          new MissingServletRequestParameterException("startDate", "LocalDate"),
          get(), response, SPANISH, model, redirectAttributes);

      assertThat(view).isEqualTo("error/400");
      assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(model.getAttribute("errorMessage")).isEqualTo(BUSINESS_GENERIC);
    }
  }

  @Nested
  class OtherExceptions {

    @Test
    void should_renderAccessDenied_when_unauthorizedClinicAccess() throws Exception {
      String view = handler.handleAccessDenied(
          new UnauthorizedClinicAccessException("Service of another clinic"), get());

      assertThat(view).isEqualTo("error/access_denied");
      assertThat(declaredStatus("handleAccessDenied", RuntimeException.class))
          .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void should_renderAccessDenied_when_accessDeniedOnPost() {
      String view = handler.handleAccessDenied(
          new AccessDeniedException("Access Denied"), post(FORM_PATH));

      assertThat(view).isEqualTo("error/access_denied");
    }

    @Test
    void should_render404_when_noResourceFound() throws Exception {
      String view = handler.handleNoResource(
          new NoResourceFoundException(HttpMethod.GET, "/missing", "missing"), get());

      assertThat(view).isEqualTo("error/404");
      assertThat(declaredStatus("handleNoResource", NoResourceFoundException.class))
          .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_render404_when_argumentTypeMismatch() throws Exception {
      MethodParameter parameter = MethodParameter.forExecutable(
          GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleEndpoint", Long.class), 0);
      String view = handler.handleTypeMismatch(
          new MethodArgumentTypeMismatchException("abc", Long.class, "id", parameter, null),
          get());

      assertThat(view).isEqualTo("error/404");
      assertThat(declaredStatus("handleTypeMismatch", MethodArgumentTypeMismatchException.class))
          .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_render500WithCorrelationId_when_unexpectedException()
        throws NoSuchMethodException {
      String view = handler.handleUnexpected(
          new IllegalStateException("boom"), post(FORM_PATH), model);

      assertThat(view).isEqualTo("error/500");
      assertThat(model.getAttribute("correlationId"))
          .asString()
          .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
      assertThat(declaredStatus("handleUnexpected", Exception.class))
          .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Nested
  class RedirectTarget {

    @Test
    void should_redirectToRoot_when_postHasNoReferer() {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", FORM_PATH);

      assertThat(handleBusinessRule(new BusinessRuleException("x"), request))
          .isEqualTo("redirect:/");
    }

    @ParameterizedTest
    @CsvSource({
        "https://evil.example/x, /x",
        "https://evil.example/x?a=1&b=2, /x?a=1&b=2",
        "//evil.example/x, /x",
        "http://localhost:8080/reports/appointments, /reports/appointments",
        "/appointments/new#section, /appointments/new",
        "/appointments/new, /appointments/new"
    })
    void should_keepOnlyPathAndQuery_when_refererIsParseable(String referer, String expected) {
      assertThat(handleBusinessRule(new BusinessRuleException("x"), postWithReferer(referer)))
          .isEqualTo("redirect:" + expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "//evil.example",
        "/\\evil.example",
        "https://evil.example//evil.other/x",
        "https://evil.example",
        "evil.example/x",
        "javascript:alert(1)",
        "/x\r\nLocation: https://evil.example",
        "   "
    })
    void should_redirectToRoot_when_refererIsNotALocalPath(String referer) {
      assertThat(handleBusinessRule(new BusinessRuleException("x"), postWithReferer(referer)))
          .isEqualTo("redirect:/");
    }
  }

  private String handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
    return handler.handleBusinessRule(ex, request, response, SPANISH, model, redirectAttributes);
  }

  private Object flashError() {
    return redirectAttributes.getFlashAttributes().get("error");
  }

  private static MockHttpServletRequest get() {
    return new MockHttpServletRequest("GET", "/pets/42");
  }

  private static MockHttpServletRequest post(String refererPath) {
    return postWithReferer("http://localhost:8080" + refererPath);
  }

  private static MockHttpServletRequest postWithReferer(String referer) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/submit");
    request.addHeader("Referer", referer);
    return request;
  }

  /**
   * Reads the {@code @ResponseStatus} of a handler method: the framework applies it to the
   * response, so a unit test cannot observe it on {@link MockHttpServletResponse}.
   */
  private static HttpStatus declaredStatus(String handlerMethod, Class<?> exceptionType)
      throws NoSuchMethodException {
    Method method = Arrays.stream(GlobalExceptionHandler.class.getMethods())
        .filter(m -> m.getName().equals(handlerMethod))
        .filter(m -> m.getParameterTypes()[0] == exceptionType)
        .findFirst()
        .orElseThrow(() -> new NoSuchMethodException(handlerMethod));
    ResponseStatus status =
        AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
    assertThat(status).as("@ResponseStatus on %s", handlerMethod).isNotNull();
    return status.code();
  }

  @SuppressWarnings("unused") // Target of the MethodParameter in the type-mismatch test.
  private static void sampleEndpoint(Long id) {
  }
}
