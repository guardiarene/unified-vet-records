package tfg.psygcv.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.exception.domain.LocalizedException;
import tfg.psygcv.exception.domain.ResourceNotFoundException;
import tfg.psygcv.exception.domain.UnauthorizedClinicAccessException;
import tfg.psygcv.shared.logging.LogSanitizer;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private static final String ROOT = "/";

  private final MessageSource messageSource;

  @ExceptionHandler(ResourceNotFoundException.class)
  public String handleResourceNotFound(
      ResourceNotFoundException ex,
      HttpServletRequest request,
      HttpServletResponse response,
      Locale locale,
      RedirectAttributes ra) {
    if (isPost(request)) {
      log.warn("Resource not found on POST [path={}]: {}", path(request),
          LogSanitizer.sanitize(ex.getMessage()));
      ra.addFlashAttribute("error", userMessage(ex, MessageKeys.NOT_FOUND_GENERIC, locale));
      return "redirect:" + redirectTarget(request);
    }
    log.warn("Resource not found [path={}]: {}", path(request),
        LogSanitizer.sanitize(ex.getMessage()));
    response.setStatus(HttpStatus.NOT_FOUND.value());
    return "error/404";
  }

  @ExceptionHandler({UnauthorizedClinicAccessException.class, AccessDeniedException.class})
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public String handleAccessDenied(RuntimeException ex, HttpServletRequest request) {
    log.warn("Access denied [path={}]: {}", path(request),
        LogSanitizer.sanitize(ex.getMessage()));
    return "error/access_denied";
  }

  @ExceptionHandler(BusinessRuleException.class)
  public String handleBusinessRule(
      BusinessRuleException ex,
      HttpServletRequest request,
      HttpServletResponse response,
      Locale locale,
      Model model,
      RedirectAttributes ra) {
    log.warn("Business rule violation [path={}]: {}", path(request),
        LogSanitizer.sanitize(ex.getMessage()));
    String userMessage = userMessage(ex, MessageKeys.BUSINESS_GENERIC, locale);
    return badRequest(userMessage, request, response, model, ra);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public String handleMissingParameter(
      MissingServletRequestParameterException ex,
      HttpServletRequest request,
      HttpServletResponse response,
      Locale locale,
      Model model,
      RedirectAttributes ra) {
    log.warn("Missing request parameter [path={}, parameter={}]", path(request),
        LogSanitizer.sanitize(ex.getParameterName()));
    String userMessage = messageSource.getMessage(MessageKeys.BUSINESS_GENERIC, null, locale);
    return badRequest(userMessage, request, response, model, ra);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
    log.warn("No resource found [path={}]: {}", path(request),
        LogSanitizer.sanitize(ex.getMessage()));
    return "error/404";
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    Class<?> requiredType = ex.getRequiredType();
    log.warn("Argument type mismatch [path={}, parameter={}, expectedType={}]",
        path(request),
        LogSanitizer.sanitize(ex.getName()),
        requiredType != null ? requiredType.getSimpleName() : "unknown");
    return "error/404";
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleUnexpected(Exception ex, HttpServletRequest request, Model model) {
    String correlationId = UUID.randomUUID().toString();
    log.error(
        "Unexpected error [correlationId={}, path={}, method={}]: {}",
        correlationId,
        path(request),
        LogSanitizer.sanitize(request.getMethod()),
        LogSanitizer.sanitize(ex.getMessage()),
        ex);
    model.addAttribute("correlationId", correlationId);
    return "error/500";
  }

  /**
   * On POST, redirects to the request's own page with the message as the {@code error} flash
   * attribute; otherwise renders {@code error/400} with status 400.
   */
  private String badRequest(
      String userMessage,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model,
      RedirectAttributes ra) {
    if (isPost(request)) {
      ra.addFlashAttribute("error", userMessage);
      return "redirect:" + redirectTarget(request);
    }
    response.setStatus(HttpStatus.BAD_REQUEST.value());
    model.addAttribute("errorMessage", userMessage);
    return "error/400";
  }

  /**
   * Resolves the user-facing text: the exception's own key when it has one and it exists in
   * {@code messages.properties}, otherwise the generic key of the exception type. Never returns
   * {@code ex.getMessage()}, which stays in English for the logs.
   */
  private String userMessage(LocalizedException ex, String genericKey, Locale locale) {
    String key = ex.getMessageKey();
    if (key != null) {
      String resolved = messageSource.getMessage(key, ex.getArgs(), null, locale);
      if (resolved != null) {
        return resolved;
      }
      log.warn("Message key not found, using generic message [key={}]",
          LogSanitizer.sanitize(key));
    }
    return messageSource.getMessage(genericKey, null, locale);
  }

  private String path(HttpServletRequest request) {
    return LogSanitizer.sanitize(request.getRequestURI());
  }

  private boolean isPost(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod());
  }

  /**
   * Returns the path and query of the {@code Referer}, without scheme or host, so a redirect
   * never leaves the application. Falls back to {@code /} when the header is missing, cannot be
   * parsed, or its path is not a local absolute path ({@code //host} and {@code /\host} are
   * protocol-relative for browsers).
   */
  private String redirectTarget(HttpServletRequest request) {
    String referer = request.getHeader("Referer");
    if (referer == null || referer.isBlank()) {
      return ROOT;
    }
    URI uri;
    try {
      uri = new URI(referer);
    } catch (URISyntaxException ex) {
      return ROOT;
    }
    String path = uri.getRawPath();
    if (path == null
        || !path.startsWith("/")
        || path.startsWith("//")
        || path.startsWith("/\\")) {
      return ROOT;
    }
    String query = uri.getRawQuery();
    return query != null ? path + "?" + query : path;
  }
}
