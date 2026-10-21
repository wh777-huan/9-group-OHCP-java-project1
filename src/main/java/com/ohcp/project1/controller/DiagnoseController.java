package com.ohcp.project1.controller;

import com.ohcp.project1.service.DiagnoseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DiagnoseController {

    private final DiagnoseService diagnoseService;

    public DiagnoseController(DiagnoseService diagnoseService) {
        this.diagnoseService = diagnoseService;
    }

    @PostMapping("/diagnose")
    public DiagnoseResult diagnose(@RequestBody SymptomInput input) {
        if (input == null || input.symptom() == null || input.symptom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先描述作物和症状。");
        }
        DiagnoseService.DiagnoseResult result = diagnoseService.diagnose(input.symptom());
        return new DiagnoseResult(
                result.element(),
                result.confidence(),
                result.description(),
                result.organicFertilizer(),
                result.chemicalFertilizer(),
                result.safetyWithGuidance(),
                result.agronomicAdvice(),
                result.suggestion(),
                result.mode(),
                result.candidates(),
                result.sources()
        );
    }

    public record SymptomInput(String symptom) {}
    public record DiagnoseResult(
            String element,
            double confidence,
            String description,
            String organicFertilizer,
            String chemicalFertilizer,
            String safetyWithGuidance,
            String agronomicAdvice,
            String suggestion,
            String mode,
            List<DiagnoseService.CandidateResult> candidates,
            List<DiagnoseService.SourceResult> sources
    ) {}
}
