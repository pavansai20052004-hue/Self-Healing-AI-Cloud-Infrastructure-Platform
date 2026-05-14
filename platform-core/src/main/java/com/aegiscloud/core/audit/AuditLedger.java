package com.aegiscloud.core.audit;

import java.util.ArrayList;
import java.util.List;

public final class AuditLedger {
    private final List<AuditEvent> events = new ArrayList<>();

    public void append(AuditEvent event) {
        events.add(event);
    }

    public List<AuditEvent> events() {
        return List.copyOf(events);
    }
}

