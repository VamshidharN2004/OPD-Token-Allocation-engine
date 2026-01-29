package com.medoc.opd.controller;

import com.medoc.opd.model.Doctor;
import com.medoc.opd.model.OpdSlot;
import com.medoc.opd.model.Token;
import com.medoc.opd.model.TokenSource;
import com.medoc.opd.service.OpdService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OpdController {
    private final OpdService opdService;

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<java.util.Map<String, String>> handleException(RuntimeException e) {
        return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }

    @PostMapping("/doctors")
    public ResponseEntity<Doctor> createDoctor(@RequestParam String name, @RequestParam String specialization) {
        return ResponseEntity.ok(opdService.onboardDoctor(name, specialization));
    }

    @PostMapping("/slots")
    public ResponseEntity<OpdSlot> createSlot(@RequestBody SlotRequest request) {
        return ResponseEntity.ok(opdService.createSlot(
                request.getDoctorId(),
                LocalDate.parse(request.getDate()),
                LocalTime.parse(request.getStartTime()),
                LocalTime.parse(request.getEndTime()),
                request.getCapacity()));
    }

    @PostMapping("/bookings")
    public ResponseEntity<Token> bookToken(@RequestBody BookingRequest request) {
        return ResponseEntity
                .ok(opdService.bookToken(request.getPatientName(), request.getSource(), request.getDoctorId(),
                        LocalDate.parse(request.getDate())));
    }

    @PostMapping("/tokens/{id}/cancel")
    public ResponseEntity<Void> cancelToken(@PathVariable String id) {
        opdService.cancelToken(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/doctors/{doctorId}/slots")
    public ResponseEntity<List<OpdSlot>> getDoctorSlots(@PathVariable String doctorId) {
        return ResponseEntity.ok(opdService.getDoctorSlots(doctorId));
    }

    @PostMapping("/slots/{id}/delay")
    public ResponseEntity<Void> delaySlot(@PathVariable String id, @RequestParam int minutes) {
        opdService.delaySlot(id, minutes);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tokens/{id}/noshow")
    public ResponseEntity<Void> toggleNoShow(@PathVariable String id) {
        opdService.toggleNoShow(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable String id) {
        opdService.deleteSlot(id);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class SlotRequest {
        private String doctorId;
        private String date;
        private String startTime;
        private String endTime;
        private int capacity;
    }

    @Data
    public static class BookingRequest {
        private String doctorId;
        private String date;
        private String patientName;
        private TokenSource source;
    }
}
