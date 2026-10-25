package tfg.psygcv.medical.visit.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.exception.domain.ResourceNotFoundException;
import tfg.psygcv.medical.record.entity.MedicalRecord;
import tfg.psygcv.medical.record.repository.MedicalRecordRepository;
import tfg.psygcv.medical.visit.command.CreateVisitCommand;
import tfg.psygcv.medical.visit.command.UpdateVisitCommand;
import tfg.psygcv.medical.visit.entity.Anamnesis;
import tfg.psygcv.medical.visit.entity.ClinicalExam;
import tfg.psygcv.medical.visit.entity.Diagnostic;
import tfg.psygcv.medical.visit.entity.Treatment;
import tfg.psygcv.medical.visit.entity.Vaccine;
import tfg.psygcv.medical.visit.entity.Visit;
import tfg.psygcv.medical.visit.repository.AnamnesisRepository;
import tfg.psygcv.medical.visit.repository.ClinicalExamRepository;
import tfg.psygcv.medical.visit.repository.DiagnosticRepository;
import tfg.psygcv.medical.visit.repository.TreatmentRepository;
import tfg.psygcv.medical.visit.repository.VaccineRepository;
import tfg.psygcv.medical.visit.repository.VisitRepository;
import tfg.psygcv.pet.entity.Pet;
import tfg.psygcv.user.entity.User;
import tfg.psygcv.user.service.UserService;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class VisitServiceImpl implements VisitService {

  private final VisitRepository visitRepository;
  private final MedicalRecordRepository medicalRecordRepository;
  private final ClinicalExamRepository clinicalExamRepository;
  private final AnamnesisRepository anamnesisRepository;
  private final DiagnosticRepository diagnosticRepository;
  private final TreatmentRepository treatmentRepository;
  private final VaccineRepository vaccineRepository;
  private final VisitValidator visitValidator;
  private final UserService userService;
  private final Clock clock;

  @Override
  @Transactional
  public Visit createVisit(Long medicalRecordId, CreateVisitCommand command, Long veterinarianId) {
    MedicalRecord medicalRecord =
        medicalRecordRepository
            .findById(medicalRecordId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Medical record not found with ID: " + medicalRecordId));
    Pet pet = medicalRecord.getPet();
    User veterinarianWithClinicContext = userService.findByIdWithClinicContext(veterinarianId);
    visitValidator.validateForCreation(command, veterinarianWithClinicContext, pet);
    Visit visit = buildVisitFromCommand(command);
    setupVisitRelationships(visit, medicalRecord, veterinarianWithClinicContext);
    return visitRepository.save(visit);
  }

  @Override
  @Transactional
  public Visit updateVisit(Long visitId, UpdateVisitCommand command, Long veterinarianId) {
    User veterinarianWithClinicContext = userService.findByIdWithClinicContext(veterinarianId);
    visitValidator.validateForUpdate(visitId, command, veterinarianWithClinicContext);
    Visit existingVisit = findCompleteById(visitId);
    updateVisitFields(existingVisit, command);
    updateRelatedEntities(existingVisit, command);
    return visitRepository.save(existingVisit);
  }

  @Override
  @Transactional(readOnly = true)
  public Visit findCompleteById(Long visitId) {
    visitValidator.validateId(visitId);
    Visit visit =
        visitRepository
            .findCompleteById(visitId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Visit not found with ID: " + visitId));
    visitRepository.findWithDiagnostics(visitId);
    visitRepository.findWithTreatments(visitId);
    visitRepository.findWithVaccines(visitId);
    if (!visit.getDiagnostics().isEmpty()) {
      List<Long> diagnosticIds =
          visit.getDiagnostics().stream().map(Diagnostic::getId).filter(Objects::nonNull).toList();
      if (!diagnosticIds.isEmpty()) {
        diagnosticRepository.findWithProblemsByIds(diagnosticIds);
      }
    }
    return visit;
  }

  @Override
  public List<Visit> findByMedicalRecord(Long medicalRecordId) {
    if (medicalRecordId == null || medicalRecordId <= 0) {
      throw new BusinessRuleException("Medical record ID must be a positive number");
    }
    List<Visit> visits = visitRepository.findCompleteByMedicalRecordId(medicalRecordId);
    if (!visits.isEmpty()) {
      List<Long> visitIds = visits.stream().map(Visit::getId).toList();
      visitRepository.findWithDiagnosticsByIds(visitIds);
      visitRepository.findWithTreatmentsByIds(visitIds);
      visitRepository.findWithVaccinesByIds(visitIds);
      List<Long> diagnosticIds =
          visits.stream().flatMap(v -> v.getDiagnostics().stream()).map(Diagnostic::getId).toList();
      if (!diagnosticIds.isEmpty()) {
        diagnosticRepository.findWithProblemsByIds(diagnosticIds);
      }
    }
    return visits;
  }

  @Override
  public Page<Visit> findByMedicalRecord(Long medicalRecordId, Pageable pageable) {
    if (medicalRecordId == null || medicalRecordId <= 0) {
      throw new BusinessRuleException("Medical record ID must be a positive number");
    }
    return visitRepository.findByMedicalRecordIdAndActiveOrderByDateDesc(medicalRecordId, pageable);
  }

  @Override
  public List<Visit> findByMedicalRecordAndDateRange(
      Long medicalRecordId, LocalDate startDate, LocalDate endDate) {
    if (medicalRecordId == null || medicalRecordId <= 0) {
      throw new BusinessRuleException("Medical record ID must be a positive number");
    }
    if (startDate == null || endDate == null) {
      throw new BusinessRuleException("Start date and end date cannot be null");
    }
    if (startDate.isAfter(endDate)) {
      throw new BusinessRuleException("Start date cannot be after end date");
    }
    return visitRepository.findByMedicalRecordIdAndDateRange(medicalRecordId, startDate, endDate);
  }

  @Override
  public Visit findLatestVisit(Long medicalRecordId) {
    if (medicalRecordId == null || medicalRecordId <= 0) {
      throw new BusinessRuleException("Medical record ID must be a positive number");
    }
    return visitRepository.findLatestByMedicalRecordId(medicalRecordId).orElse(null);
  }

  @Override
  @Transactional
  public Long deleteVisit(Long visitId) {
    visitValidator.validateId(visitId);
    Visit visit =
        visitRepository
            .findById(visitId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Visit not found with ID: " + visitId));
    visit.setActive(false);
    visitRepository.save(visit);
    log.info("Visit deleted [visitId={}]", visitId);
    return visit.getMedicalRecord().getId();
  }

  private Visit buildVisitFromCommand(CreateVisitCommand command) {
    Visit visit = new Visit();
    visit.setDate(command.getDate());
    visit.setReasonForVisit(command.getReasonForVisit());
    visit.setVisitType(command.getVisitType());
    visit.setObservations(command.getObservations());
    visit.setClinicalExam(buildClinicalExam(command.getClinicalExam()));
    visit.setAnamnesis(buildAnamnesis(command.getAnamnesis()));
    if (command.getDiagnostics() != null) {
      command.getDiagnostics().stream().map(this::buildDiagnostic).forEach(d -> d.setVisit(visit));
    }
    if (command.getTreatments() != null) {
      command.getTreatments().stream().map(this::buildTreatment).forEach(t -> t.setVisit(visit));
    }
    if (command.getVaccines() != null) {
      command.getVaccines().stream().map(this::buildVaccine).forEach(v -> v.setVisit(visit));
    }
    return visit;
  }

  private ClinicalExam buildClinicalExam(CreateVisitCommand.ClinicalExamData data) {
    if (data == null) {
      return null;
    }
    ClinicalExam exam = new ClinicalExam();
    exam.setTemperature(data.getTemperature());
    exam.setHeartRate(data.getHeartRate());
    exam.setRespiratoryRate(data.getRespiratoryRate());
    exam.setWeight(data.getWeight());
    exam.setPulse(data.getPulse());
    exam.setMucosalMembranes(data.getMucosalMembranes());
    exam.setTemperament(data.getTemperament());
    exam.setDescription(data.getDescription());
    return exam;
  }

  private Anamnesis buildAnamnesis(CreateVisitCommand.AnamnesisData data) {
    if (data == null) {
      return null;
    }
    Anamnesis anamnesis = new Anamnesis();
    anamnesis.setAllergies(data.getAllergies());
    anamnesis.setPreviousDiseases(data.getPreviousDiseases());
    anamnesis.setSurgeries(data.getSurgeries());
    anamnesis.setCurrentMedications(data.getCurrentMedications());
    anamnesis.setDiet(data.getDiet());
    anamnesis.setReproductiveStatus(data.getReproductiveStatus());
    anamnesis.setLastDewormingDate(data.getLastDewormingDate());
    anamnesis.setLastHeatDate(data.getLastHeatDate());
    anamnesis.setLastBirthDate(data.getLastBirthDate());
    return anamnesis;
  }

  private Diagnostic buildDiagnostic(CreateVisitCommand.DiagnosticData data) {
    Diagnostic diagnostic = new Diagnostic();
    if (data.getProblems() != null) {
      diagnostic.setProblems(new ArrayList<>(data.getProblems()));
    }
    return diagnostic;
  }

  private Treatment buildTreatment(CreateVisitCommand.TreatmentData data) {
    Treatment treatment = new Treatment();
    treatment.setProduct(data.getProduct());
    treatment.setRoute(data.getRoute());
    treatment.setFrequency(data.getFrequency());
    treatment.setStartDate(data.getStartDate());
    treatment.setEndDate(data.getEndDate());
    return treatment;
  }

  private Vaccine buildVaccine(CreateVisitCommand.VaccineData data) {
    Vaccine vaccine = new Vaccine();
    vaccine.setApplicationDate(data.getApplicationDate());
    vaccine.setBrand(data.getBrand());
    vaccine.setDose(data.getDose());
    vaccine.setBatch(data.getBatch());
    return vaccine;
  }

  private void setupVisitRelationships(
      Visit visit, MedicalRecord medicalRecord, User veterinarian) {
    visit.setMedicalRecord(medicalRecord);
    visit.setVeterinarian(veterinarian);
    visit.setActive(true);
    if (visit.getDate() == null) {
      visit.setDate(LocalDate.now(clock));
    }
    if (visit.getClinicalExam() != null) {
      visit.getClinicalExam().setVisit(visit);
      visit.getClinicalExam().setActive(true);
      if (visit.getClinicalExam().getWeight() != null && medicalRecord.getPet() != null) {
        medicalRecord.getPet().setWeight(visit.getClinicalExam().getWeight());
      }
    }
    if (visit.getAnamnesis() != null) {
      visit.getAnamnesis().setVisit(visit);
      visit.getAnamnesis().setActive(true);
    }
    visit
        .getDiagnostics()
        .forEach(
            d -> {
              d.setVisit(visit);
              d.setActive(true);
            });
    visit
        .getTreatments()
        .forEach(
            t -> {
              t.setVisit(visit);
              t.setActive(true);
            });
    visit
        .getVaccines()
        .forEach(
            v -> {
              v.setVisit(visit);
              v.setMedicalRecord(medicalRecord);
              v.setActive(true);
            });
  }

  private void updateVisitFields(Visit existing, UpdateVisitCommand command) {
    existing.setReasonForVisit(command.getReasonForVisit());
    existing.setVisitType(command.getVisitType());
    existing.setObservations(command.getObservations());
    if (command.getDate() != null) {
      existing.setDate(command.getDate());
    }
  }

  private void updateRelatedEntities(Visit existing, UpdateVisitCommand command) {
    updateClinicalExam(existing, command.getClinicalExam());
    updateAnamnesis(existing, command.getAnamnesis());
    updateDiagnostics(existing, command.getDiagnostics());
    updateTreatments(existing, command.getTreatments());
    updateVaccines(existing, command.getVaccines());
  }

  private void updateClinicalExam(Visit visit, UpdateVisitCommand.ClinicalExamData data) {
    if (data == null) {
      return;
    }
    ClinicalExam exam = visit.getClinicalExam();
    if (exam == null) {
      exam = new ClinicalExam();
      exam.setActive(true);
      visit.setClinicalExam(exam);
    }
    exam.setTemperature(data.getTemperature());
    exam.setHeartRate(data.getHeartRate());
    exam.setRespiratoryRate(data.getRespiratoryRate());
    exam.setWeight(data.getWeight());
    exam.setPulse(data.getPulse());
    exam.setMucosalMembranes(data.getMucosalMembranes());
    exam.setTemperament(data.getTemperament());
    exam.setDescription(data.getDescription());
    if (exam.getWeight() != null
        && visit.getMedicalRecord() != null
        && visit.getMedicalRecord().getPet() != null) {
      visit.getMedicalRecord().getPet().setWeight(exam.getWeight());
    }
  }

  private void updateAnamnesis(Visit visit, UpdateVisitCommand.AnamnesisData data) {
    if (data == null) {
      return;
    }
    Anamnesis anamnesis = visit.getAnamnesis();
    if (anamnesis == null) {
      anamnesis = new Anamnesis();
      anamnesis.setActive(true);
      visit.setAnamnesis(anamnesis);
    }
    anamnesis.setAllergies(data.getAllergies());
    anamnesis.setPreviousDiseases(data.getPreviousDiseases());
    anamnesis.setSurgeries(data.getSurgeries());
    anamnesis.setCurrentMedications(data.getCurrentMedications());
    anamnesis.setDiet(data.getDiet());
    anamnesis.setReproductiveStatus(data.getReproductiveStatus());
    anamnesis.setLastDewormingDate(data.getLastDewormingDate());
    anamnesis.setLastHeatDate(data.getLastHeatDate());
    anamnesis.setLastBirthDate(data.getLastBirthDate());
  }

  private void updateDiagnostics(Visit visit, List<UpdateVisitCommand.DiagnosticData> diagnostics) {
    if (diagnostics == null) {
      return;
    }
    List<Long> newIds =
        diagnostics.stream()
            .map(UpdateVisitCommand.DiagnosticData::getId)
            .filter(Objects::nonNull)
            .toList();
    visit.getDiagnostics().removeIf(d -> d.getId() != null && !newIds.contains(d.getId()));
    for (UpdateVisitCommand.DiagnosticData data : diagnostics) {
      if (data.getId() == null) {
        Diagnostic diag = new Diagnostic();
        diag.setProblems(
            data.getProblems() != null ? new ArrayList<>(data.getProblems()) : new ArrayList<>());
        diag.setVisit(visit);
        diag.setActive(true);
        visit.getDiagnostics().add(diag);
      } else {
        Diagnostic diag =
            visit.getDiagnostics().stream()
                .filter(d -> d.getId().equals(data.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic not found"));
        diag.setProblems(
            data.getProblems() != null ? new ArrayList<>(data.getProblems()) : new ArrayList<>());
      }
    }
  }

  private void updateTreatments(Visit visit, List<UpdateVisitCommand.TreatmentData> treatments) {
    if (treatments == null) {
      return;
    }
    List<Long> newIds =
        treatments.stream()
            .map(UpdateVisitCommand.TreatmentData::getId)
            .filter(Objects::nonNull)
            .toList();
    visit.getTreatments().removeIf(t -> t.getId() != null && !newIds.contains(t.getId()));
    for (UpdateVisitCommand.TreatmentData data : treatments) {
      if (data.getId() == null) {
        Treatment treatment = new Treatment();
        treatment.setProduct(data.getProduct());
        treatment.setRoute(data.getRoute());
        treatment.setFrequency(data.getFrequency());
        treatment.setStartDate(data.getStartDate());
        treatment.setEndDate(data.getEndDate());
        treatment.setVisit(visit);
        treatment.setActive(true);
        visit.getTreatments().add(treatment);
      } else {
        Treatment treatment =
            visit.getTreatments().stream()
                .filter(t -> t.getId().equals(data.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Treatment not found"));
        treatment.setProduct(data.getProduct());
        treatment.setRoute(data.getRoute());
        treatment.setFrequency(data.getFrequency());
        treatment.setStartDate(data.getStartDate());
        treatment.setEndDate(data.getEndDate());
      }
    }
  }

  private void updateVaccines(Visit visit, List<UpdateVisitCommand.VaccineData> vaccines) {
    if (vaccines == null) {
      return;
    }
    List<Vaccine> allVaccines = visit.getMedicalRecord().getVaccines();
    Map<Long, Vaccine> existingVaccinesMap =
        allVaccines.stream()
            .filter(v -> v.getVisit() != null && v.getVisit().getId().equals(visit.getId()))
            .collect(Collectors.toMap(Vaccine::getId, Function.identity()));
    List<Long> newIds =
        vaccines.stream()
            .map(UpdateVisitCommand.VaccineData::getId)
            .filter(Objects::nonNull)
            .toList();
    allVaccines.removeIf(
        v ->
            v.getVisit() != null
                && v.getVisit().getId().equals(visit.getId())
                && v.getId() != null
                && !newIds.contains(v.getId()));
    for (UpdateVisitCommand.VaccineData data : vaccines) {
      if (data.getId() == null) {
        Vaccine vaccine = new Vaccine();
        vaccine.setApplicationDate(data.getApplicationDate());
        vaccine.setBrand(data.getBrand());
        vaccine.setDose(data.getDose());
        vaccine.setBatch(data.getBatch());
        vaccine.setVisit(visit);
        vaccine.setMedicalRecord(visit.getMedicalRecord());
        vaccine.setActive(true);
        allVaccines.add(vaccine);
      } else {
        Vaccine vaccine =
            Optional.ofNullable(existingVaccinesMap.get(data.getId()))
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "Vaccine not found with ID: " + data.getId()));
        vaccine.setApplicationDate(data.getApplicationDate());
        vaccine.setBrand(data.getBrand());
        vaccine.setDose(data.getDose());
        vaccine.setBatch(data.getBatch());
      }
    }
  }
}
