package tfg.psygcv.medical.visit.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tfg.psygcv.medical.record.dto.response.MedicalRecordSummaryResponse;
import tfg.psygcv.medical.record.mapper.MedicalRecordMapper;
import tfg.psygcv.medical.record.service.MedicalRecordService;
import tfg.psygcv.medical.visit.dto.request.CreateVisitRequest;
import tfg.psygcv.medical.visit.dto.request.UpdateVisitRequest;
import tfg.psygcv.medical.visit.dto.response.VisitResponse;
import tfg.psygcv.medical.visit.entity.Visit;
import tfg.psygcv.medical.visit.entity.VisitType;
import tfg.psygcv.medical.visit.mapper.VisitMapper;
import tfg.psygcv.medical.visit.service.VisitService;
import tfg.psygcv.shared.authorization.ResourceAuthorizationService;
import tfg.psygcv.shared.controller.BaseController;

@RequiredArgsConstructor
@RequestMapping("/visits")
@Controller
public class VisitController extends BaseController {

  private final VisitService visitService;
  private final MedicalRecordService medicalRecordService;
  private final ResourceAuthorizationService resourceAuthorizationService;

  @GetMapping("/{id}")
  public String showVisitDetails(@PathVariable Long id, Model model,
                                 Authentication authentication) {
    VisitResponse visit = VisitMapper.toResponse(visitService.findCompleteById(id));
    model.addAttribute("visit", visit);
    model.addAttribute(
        "canEditVisit", resourceAuthorizationService.canModifyVisit(authentication, id));
    return "visits/details";
  }

  @GetMapping("/new")
  public String showNewVisitForm(@RequestParam Long medicalRecordId, Model model) {
    MedicalRecordSummaryResponse medicalRecord =
        MedicalRecordMapper.toSummary(medicalRecordService.findCompleteById(medicalRecordId));
    CreateVisitRequest visit = new CreateVisitRequest();
    visit.setMedicalRecordId(medicalRecordId);
    visit.setDate(LocalDate.now());
    visit.setVisitType(VisitType.CONSULTATION);
    model.addAttribute("visit", visit);
    model.addAttribute("medicalRecord", medicalRecord);
    model.addAttribute("visitTypes", VisitType.values());
    return "visits/new";
  }

  @PostMapping("/new")
  public String saveVisit(
      @Valid @ModelAttribute("visit") CreateVisitRequest request,
      BindingResult result,
      Authentication authentication,
      Model model) {
    if (result.hasErrors()) {
      MedicalRecordSummaryResponse medicalRecord =
          MedicalRecordMapper.toSummary(
              medicalRecordService.findCompleteById(request.getMedicalRecordId()));
      model.addAttribute("medicalRecord", medicalRecord);
      model.addAttribute("visitTypes", VisitType.values());
      return "visits/new";
    }
    Long veterinarianId = getAuthenticatedUser(authentication).getId();
    Visit savedVisit =
        visitService.createVisit(
            request.getMedicalRecordId(), VisitMapper.toCreateCommand(request), veterinarianId);
    return "redirect:/visits/" + savedVisit.getId();
  }

  @GetMapping("/{id}/edit")
  public String showEditForm(@PathVariable Long id, Model model) {
    Visit visit = visitService.findCompleteById(id);
    model.addAttribute("visit", VisitMapper.toUpdateRequest(visit));
    model.addAttribute("visitId", id);
    model.addAttribute("medicalRecordId", visit.getMedicalRecord().getId());
    model.addAttribute("visitTypes", VisitType.values());
    return "visits/edit";
  }

  @PostMapping("/{id}/edit")
  public String updateVisit(
      @PathVariable Long id,
      @Valid @ModelAttribute("visit") UpdateVisitRequest request,
      BindingResult result,
      Authentication authentication,
      Model model) {
    if (result.hasErrors()) {
      model.addAttribute("visitId", id);
      model.addAttribute("visitTypes", VisitType.values());
      return "visits/edit";
    }
    Long veterinarianId = getAuthenticatedUser(authentication).getId();
    Visit updatedVisit =
        visitService.updateVisit(id, VisitMapper.toUpdateCommand(request), veterinarianId);
    return "redirect:/visits/" + updatedVisit.getId();
  }

  @PostMapping("/{id}/delete")
  public String deleteVisit(@PathVariable Long id) {
    Long medicalRecordId = visitService.deleteVisit(id);
    return "redirect:/medical-records/" + medicalRecordId;
  }
}
