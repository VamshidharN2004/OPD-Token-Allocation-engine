package com.medoc.opd.repository;

import com.medoc.opd.model.Doctor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DoctorRepository {
    private final Map<String, Doctor> doctors = new ConcurrentHashMap<>();

    public Doctor save(Doctor doctor) {
        doctors.put(doctor.getId(), doctor);
        return doctor;
    }

    public Optional<Doctor> findById(String id) {
        return Optional.ofNullable(doctors.get(id));
    }

    public List<Doctor> findAll() {
        return new ArrayList<>(doctors.values());
    }

    public boolean existsById(String id) {
        return doctors.containsKey(id);
    }
}
