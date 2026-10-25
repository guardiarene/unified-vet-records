package tfg.psygcv.statistics.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tfg.psygcv.config.util.JsonUtil;
import tfg.psygcv.shared.controller.BaseController;
import tfg.psygcv.statistics.service.StatisticsService;

@RequiredArgsConstructor
@RequestMapping("/reports")
@Controller
public class ReportController extends BaseController {

  private final StatisticsService statisticsService;
  private final JsonUtil jsonUtil;
  private final Clock clock;

  @GetMapping("/diseases-treatments")
  public String showDiseasesTreatmentsReport(Model model) {
    initializeReportModel(model);
    return "reports/diseases_treatments";
  }

  @PostMapping("/diseases-treatments")
  public String generateDiseasesTreatmentsReport(
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate,
      Authentication authentication,
      Model model) {
    Long userId = getAuthenticatedUser(authentication).getId();
    Map<String, Long> diseases = statisticsService.getCommonDiseases(userId, startDate, endDate);
    Map<String, Long> treatments =
        statisticsService.getFrequentTreatments(userId, startDate, endDate);
    populateReportModel(model, diseases, treatments, startDate, endDate);
    return "reports/diseases_treatments";
  }

  @GetMapping("/appointments")
  public String showAppointmentsReport(Model model) {
    model.addAttribute("startDate", LocalDate.now(clock).minusMonths(1));
    model.addAttribute("endDate", LocalDate.now(clock));
    model.addAttribute("appointments", new LinkedHashMap<>());
    model.addAttribute("appointmentsJson", "{}");
    return "reports/appointments";
  }

  @PostMapping("/appointments")
  public String generateAppointmentsReport(
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate,
      Authentication authentication,
      Model model) {
    Long userId = getAuthenticatedUser(authentication).getId();
    Map<LocalDate, Long> appointments =
        statisticsService.getAppointmentsByDate(userId, startDate, endDate);
    model.addAttribute("appointments", appointments);
    model.addAttribute("appointmentsJson", jsonUtil.toJson(appointments));
    model.addAttribute("startDate", startDate);
    model.addAttribute("endDate", endDate);
    return "reports/appointments";
  }

  private void initializeReportModel(Model model) {
    model.addAttribute("startDate", LocalDate.now(clock).minusMonths(1));
    model.addAttribute("endDate", LocalDate.now(clock));
    model.addAttribute("diseases", new LinkedHashMap<>());
    model.addAttribute("treatments", new LinkedHashMap<>());
    model.addAttribute("diseasesJson", "{}");
    model.addAttribute("treatmentsJson", "{}");
  }

  private void populateReportModel(
      Model model,
      Map<String, Long> diseases,
      Map<String, Long> treatments,
      LocalDate startDate,
      LocalDate endDate) {
    model.addAttribute("diseases", diseases);
    model.addAttribute("treatments", treatments);
    model.addAttribute("diseasesJson", jsonUtil.toJson(diseases));
    model.addAttribute("treatmentsJson", jsonUtil.toJson(treatments));
    model.addAttribute("startDate", startDate);
    model.addAttribute("endDate", endDate);
  }
}
