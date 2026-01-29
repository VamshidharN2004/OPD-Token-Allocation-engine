package com.medoc.opd.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class Token implements Comparable<Token> {
    private String id;
    private String patientName;
    private TokenSource source;
    private TokenStatus status;
    private String assignedSlotId;
    private LocalDateTime createdAt;

    // For tracking waiting time / order
    private long globalOrder;

    public Token(String patientName, TokenSource source) {
        this.id = UUID.randomUUID().toString();
        this.patientName = patientName;
        this.source = source;
        this.status = TokenStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public int compareTo(Token other) {
        // Lower priorityLevel wins (1 is Emergency, 5 is Walk-in)
        int priorityComparison = Integer.compare(this.source.getPriorityLevel(), other.source.getPriorityLevel());
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        // If same priority, FIFO (First In First Out)
        return this.createdAt.compareTo(other.createdAt);
    }
}
