package com.medoc.opd.repository;

import com.medoc.opd.model.OpdSlot;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class SlotRepository {
    private final Map<String, OpdSlot> slots = new ConcurrentHashMap<>();

    public OpdSlot save(OpdSlot slot) {
        slots.put(slot.getId(), slot);
        return slot;
    }

    public Optional<OpdSlot> findById(String id) {
        return Optional.ofNullable(slots.get(id));
    }

    public List<OpdSlot> findByDoctorId(String doctorId) {
        return slots.values().stream()
                .filter(s -> s.getDoctorId().equals(doctorId))
                .sorted(Comparator.comparing(OpdSlot::getDate)
                        .thenComparing(OpdSlot::getStartTime))
                .collect(Collectors.toList());
    }

    public List<OpdSlot> findAll() {
        return new ArrayList<>(slots.values());
    }

    public boolean existsById(String id) {
        return slots.containsKey(id);
    }

    public void deleteById(String id) {
        slots.remove(id);
    }
}
