package com.medoc.opd.service;

import com.medoc.opd.model.*;
import com.medoc.opd.repository.DoctorRepository;
import com.medoc.opd.repository.SlotRepository;
import com.medoc.opd.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpdService {
    private final DoctorRepository doctorRepository;
    private final SlotRepository slotRepository;
    private final TokenRepository tokenRepository;

    public Doctor onboardDoctor(String name, String specialization) {
        String id;
        do {
            long num = (long) (Math.floor(Math.random() * 90_000_000L) + 10_000_000L);
            id = String.valueOf(num);
        } while (doctorRepository.existsById(id));

        Doctor doctor = new Doctor(id, name, specialization);
        return doctorRepository.save(doctor);
    }

    public OpdSlot createSlot(String doctorId, LocalDate date, LocalTime start, LocalTime end, int capacity) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new RuntimeException("Doctor ID not found");
        }
        if (!end.isAfter(start)) {
            throw new RuntimeException("End time must be after start time");
        }
        OpdSlot slot = new OpdSlot(doctorId, date, start, end, capacity);
        return slotRepository.save(slot);
    }

    public Token bookToken(String patientName, TokenSource source, String doctorId, LocalDate date) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new RuntimeException("Doctor ID not found");
        }
        List<OpdSlot> doctorSlots = slotRepository.findByDoctorId(doctorId).stream()
                .filter(s -> s.getDate().equals(date))
                .toList();

        if (doctorSlots.isEmpty()) {
            throw new RuntimeException("No slots available for this doctor on " + date);
        }

        Token newToken = new Token(patientName, source);
        tokenRepository.save(newToken);

        boolean allocated = false;
        for (OpdSlot slot : doctorSlots) {
            if (tryAllocateToSlot(newToken, slot)) {
                allocated = true;
                break;
            }
        }

        if (!allocated) {
            if (source == TokenSource.EMERGENCY) {
                OpdSlot firstSlot = doctorSlots.get(0);
                log.info("Emergency Allocation: Forcing token {} into slot {}", newToken.getId(), firstSlot.getId());
                firstSlot.getTokens().add(newToken);
                newToken.setAssignedSlotId(firstSlot.getId());
                newToken.setStatus(TokenStatus.ACTIVE);
            } else {
                newToken.setStatus(TokenStatus.CANCELLED);
                log.warn("Could not allocate token {} - Slots full and priority too low", newToken.getId());
                throw new RuntimeException("Slots full. Please try again later (or higher priority needed).");
            }
        }

        return newToken;
    }

    private boolean tryAllocateToSlot(Token incomingToken, OpdSlot slot) {
        List<Token> currentTokens = slot.getTokens();

        long activeCount = currentTokens.stream()
                .filter(t -> t.getStatus() == TokenStatus.ACTIVE)
                .count();

        if (activeCount < slot.getMaxCapacity()) {
            currentTokens.add(incomingToken);
            Collections.sort(currentTokens);
            incomingToken.setAssignedSlotId(slot.getId());
            return true;
        }

        Token lowestToken = currentTokens.get(currentTokens.size() - 1);

        if (incomingToken.compareTo(lowestToken) < 0) {
            log.info("Reallocation: Bumping token {} (Source: {}) to make room for {} (Source: {})",
                    lowestToken.getId(), lowestToken.getSource(), incomingToken.getId(), incomingToken.getSource());

            currentTokens.remove(currentTokens.size() - 1);

            currentTokens.add(incomingToken);
            Collections.sort(currentTokens);
            incomingToken.setAssignedSlotId(slot.getId());

            reallocateBumpedToken(lowestToken, slot.getDoctorId());
            return true;
        }

        return false;
    }

    private void reallocateBumpedToken(Token token, String doctorId) {
        List<OpdSlot> allSlots = slotRepository.findByDoctorId(doctorId);
        String oldSlotId = token.getAssignedSlotId();
        int currentSlotIndex = -1;
        for (int i = 0; i < allSlots.size(); i++) {
            if (allSlots.get(i).getId().equals(oldSlotId)) {
                currentSlotIndex = i;
                break;
            }
        }

        boolean reallocated = false;
        for (int i = currentSlotIndex + 1; i < allSlots.size(); i++) {
            if (tryAllocateToSlot(token, allSlots.get(i))) {
                reallocated = true;
                break;
            }
        }

        if (!reallocated) {
            token.setStatus(TokenStatus.CANCELLED);
            token.setAssignedSlotId(null);
            log.warn("Bumped token {} could not be reallocated. Marking CANCELLED.", token.getId());
        }
    }

    public void cancelToken(String tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (token.getStatus() != TokenStatus.ACTIVE) {
            throw new RuntimeException("Token is not active");
        }

        String slotId = token.getAssignedSlotId();
        if (slotId != null) {
            OpdSlot slot = slotRepository.findById(slotId)
                    .orElseThrow(() -> new RuntimeException("Slot not found"));
            slot.getTokens().remove(token);
        }

        token.setStatus(TokenStatus.CANCELLED);
        token.setAssignedSlotId(null);
        log.info("Token {} cancelled.", tokenId);
    }

    public List<OpdSlot> getDoctorSlots(String doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new RuntimeException("Doctor ID not found");
        }
        return slotRepository.findByDoctorId(doctorId);
    }

    public void delaySlot(String slotId, int minutes) {
        OpdSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        slot.setStartTime(slot.getStartTime().plusMinutes(minutes));
        slot.setEndTime(slot.getEndTime().plusMinutes(minutes));
    }

    public void toggleNoShow(String tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (token.getStatus() == TokenStatus.ACTIVE) {
            token.setStatus(TokenStatus.NO_SHOW);
            log.info("Token {} marked as NO_SHOW (Capacity freed).", tokenId);

            if (token.getAssignedSlotId() != null) {
                OpdSlot currentSlot = slotRepository.findById(token.getAssignedSlotId())
                        .orElseThrow(() -> new RuntimeException("Slot not found"));

                List<OpdSlot> allSlots = slotRepository.findByDoctorId(currentSlot.getDoctorId());
                int currentIndex = -1;
                for (int i = 0; i < allSlots.size(); i++) {
                    if (allSlots.get(i).getId().equals(currentSlot.getId())) {
                        currentIndex = i;
                        break;
                    }
                }

                if (currentIndex != -1 && currentIndex + 1 < allSlots.size()) {
                    OpdSlot nextSlot = allSlots.get(currentIndex + 1);

                    if (nextSlot.getDate().equals(currentSlot.getDate())) {
                        Token candidate = null;
                        for (Token t : nextSlot.getTokens()) {
                            if (t.getStatus() == TokenStatus.ACTIVE) {
                                candidate = t;
                                break;
                            }
                        }

                        if (candidate != null) {
                            log.info("Smart Fill: Pulling token {} from Next Slot ({}) into Current Slot ({})",
                                    candidate.getPatientName(), nextSlot.getStartTime(), currentSlot.getStartTime());

                            nextSlot.getTokens().remove(candidate);
                            candidate.setAssignedSlotId(currentSlot.getId());
                            currentSlot.getTokens().add(candidate);
                            Collections.sort(currentSlot.getTokens());
                        }
                    }
                }
            }
        } else if (token.getStatus() == TokenStatus.NO_SHOW) {
            token.setStatus(TokenStatus.ACTIVE);
            log.info("Token {} toggled back to ACTIVE (Capacity consumed).", tokenId);

            if (token.getAssignedSlotId() != null) {
                OpdSlot slot = slotRepository.findById(token.getAssignedSlotId())
                        .orElseThrow(() -> new RuntimeException("Slot not found"));

                long activeCount = slot.getTokens().stream()
                        .filter(t -> t.getStatus() == TokenStatus.ACTIVE)
                        .count();

                if (activeCount > slot.getMaxCapacity()) {
                    Collections.sort(slot.getTokens());

                    Token bumpCandidate = null;
                    for (int i = slot.getTokens().size() - 1; i >= 0; i--) {
                        Token t = slot.getTokens().get(i);
                        if (t.getStatus() == TokenStatus.ACTIVE) {
                            bumpCandidate = t;
                            break;
                        }
                    }

                    if (bumpCandidate != null) {
                        log.info("Undo No-Show caused overflow. Bumping token {}", bumpCandidate.getId());
                        slot.getTokens().remove(bumpCandidate);
                        reallocateBumpedToken(bumpCandidate, slot.getDoctorId());
                    }
                }
            }
        } else {
            throw new RuntimeException("Token must be ACTIVE or NO_SHOW to toggle.");
        }
    }

    public void reduceSlotCapacity(String slotId, int newCapacity) {
        OpdSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (newCapacity < 0)
            throw new RuntimeException("Capacity cannot be negative");

        log.info("Reducing capacity of slot {} to {}", slotId, newCapacity);
        slot.setMaxCapacity(newCapacity);

        List<Token> currentTokens = slot.getTokens();
        while (currentTokens.size() > newCapacity) {
            Token overflowToken = currentTokens.remove(currentTokens.size() - 1);
            log.info("Capacity Crunch: Pushing token {} (Source: {}) to next slot",
                    overflowToken.getPatientName(), overflowToken.getSource());

            reallocateBumpedToken(overflowToken, slot.getDoctorId());
        }
    }

    public void deleteSlot(String slotId) {
        OpdSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        log.info("Deleting slot: {} - {}", slot.getStartTime(), slot.getEndTime());
        slotRepository.deleteById(slotId);
    }
}
