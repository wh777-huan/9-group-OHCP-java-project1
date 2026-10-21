package com.ohcp.project1.controller;

import com.ohcp.project1.service.DiagnoseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DiagnoseController {

    private final DiagnoseService diagnoseService;

    public DiagnoseController(DiagnoseService diagnoseService) {
        this.diagnoseService = diagnoseService;
    }

    @PostMapping("/diagnose")
    public DiagnoseResult diagnose(@RequestBody SymptomInput input) {
        DiagnoseService.DiagnoseResult result = diagnoseService.diagnose(input.symptom());
        return new DiagnoseResult(
                result.element(),
                result.confidence(),
                result.description(),
                result.organicFertilizer(),
                result.chemicalFertilizer(),
                result.safetyWithGuidance()
        );
    }

    public record SymptomInput(String symptom) {}
    public record DiagnoseResult(
            String element,
            double confidence,
            String description,
            String organicFertilizer,
            String chemicalFertilizer,
            String safetyWithGuidance
    ) {}
}
