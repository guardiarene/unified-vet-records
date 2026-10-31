package tfg.psygcv.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.exception.domain.ResourceNotFoundException;
import tfg.psygcv.exception.domain.UnauthorizedClinicAccessException;
import tfg.psygcv.shared.logging.LogSanitizer;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public String handleResourceNotFound(
      ResourceNotFoundException ex,
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes ra) {
    if (isPost(request)) {
      log.warn("Resource not found on POST [path={}]: {}", path(request),
          LogSanitizer.sanitize(ex.getMessage()));
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:" + referer(request);
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
      Model model,
      RedirectAttributes ra) {
    log.warn("Business rule violation [path={}]: {}", path(request),
        LogSanitizer.sanitize(ex.getMessage()));
    if (isPost(request)) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:" + referer(request);
    }
    response.setStatus(HttpStatus.BAD_REQUEST.value());
    model.addAttribute("errorMessage", ex.getMessage());
    return "error/400";
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

  private String path(HttpServletRequest request) {
    return LogSanitizer.sanitize(request.getRequestURI());
  }

  private boolean isPost(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod());
  }

  private String referer(HttpServletRequest request) {
    String referer = request.getHeader("Referer");
    return (referer != null && !referer.isBlank()) ? referer : "/";
  }
}
