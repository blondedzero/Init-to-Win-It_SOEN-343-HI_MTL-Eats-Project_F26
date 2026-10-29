package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "system_events")
public class SystemEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    protected SystemEvent() {}

    public SystemEvent(String event) {
        this.event = event;
        this.occurredAt = Instant.now();
    }

    public Long getEventId() {
        return eventId;
    }

    public String getEvent() {
        return event;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}


