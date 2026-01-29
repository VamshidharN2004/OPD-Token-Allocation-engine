package com.medoc.opd.model;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@ToString(exclude = "tokens")
public class OpdSlot {
    private String id;
    private String doctorId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private int maxCapacity;
    private List<Token> tokens;

    public OpdSlot(String doctorId, LocalDate date, LocalTime startTime, LocalTime endTime, int maxCapacity) {
        this.id = UUID.randomUUID().toString();
        this.doctorId = doctorId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCapacity = maxCapacity;
        this.tokens = new ArrayList<>();
    }
}
