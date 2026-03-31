package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.Acknowledgement;
import se.forsman.bevakning.domain.AlertHistoryEntry;
import se.forsman.bevakning.repository.AcknowledgementRepository;
import se.forsman.bevakning.repository.AlertHistoryRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AcknowledgementService {
    private final AcknowledgementRepository acknowledgementRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    public AcknowledgementService(AcknowledgementRepository acknowledgementRepository,
                                  AlertHistoryRepository alertHistoryRepository) {
        this.acknowledgementRepository = acknowledgementRepository;
        this.alertHistoryRepository = alertHistoryRepository;
    }

    public void acknowledge(String ruleId, String status, String comment, String acknowledgedBy) {
        String now = LocalDateTime.now().toString();
        String occurrenceKey = resolveOccurrenceKey(ruleId, status);

        if (alreadyAcknowledged(ruleId, occurrenceKey, status)) {
            throw new IllegalStateException("Händelsen är redan kvitterad för denna bevakningsinstans");
        }

        acknowledgementRepository.save(new Acknowledgement(
                "ack-" + UUID.randomUUID().toString(),
                ruleId,
                occurrenceKey,
                status,
                comment,
                acknowledgedBy,
                now
        ));

        alertHistoryRepository.save(new AlertHistoryEntry(
                "hist-" + UUID.randomUUID().toString(),
                ruleId,
                occurrenceKey,
                "ACKNOWLEDGED",
                status,
                comment,
                now,
                acknowledgedBy
        ));
    }

    public List<Acknowledgement> findAllAcknowledgements() {
        return acknowledgementRepository.findAll();
    }

    public List<AlertHistoryEntry> findAllHistory() {
        return alertHistoryRepository.findAll();
    }

    private boolean alreadyAcknowledged(String ruleId, String occurrenceKey, String status) {
        List<Acknowledgement> acknowledgements = acknowledgementRepository.findAll();

        for (Acknowledgement ack : acknowledgements) {
            if (ack == null) continue;
            if (!safe(ruleId).equals(safe(ack.getRuleId()))) continue;
            if (!safe(occurrenceKey).equals(safe(ack.getOccurrenceKey()))) continue;
            if (!safe(status).equalsIgnoreCase(safe(ack.getStatus()))) continue;
            return true;
        }

        return false;
    }

    private String resolveOccurrenceKey(String ruleId, String status) {
        List<AlertHistoryEntry> history = alertHistoryRepository.findAll();
        AlertHistoryEntry best = null;

        for (AlertHistoryEntry entry : history) {
            if (entry == null) continue;
            if (!safe(ruleId).equals(safe(entry.getRuleId()))) continue;
            if (!safe(status).equalsIgnoreCase(safe(entry.getStatus()))) continue;
            if ("ACKNOWLEDGED".equalsIgnoreCase(safe(entry.getEventType()))) continue;

            if (best == null || safe(entry.getCreatedAt()).compareTo(safe(best.getCreatedAt())) > 0) {
                best = entry;
            }
        }

        if (best != null && !safe(best.getOccurrenceKey()).isEmpty()) {
            return best.getOccurrenceKey();
        }

        return ruleId + "|" + LocalDate.now().toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
