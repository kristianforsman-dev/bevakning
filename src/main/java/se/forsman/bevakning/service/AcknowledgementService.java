package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.Acknowledgement;
import se.forsman.bevakning.domain.AlertHistoryEntry;
import se.forsman.bevakning.repository.AcknowledgementRepository;
import se.forsman.bevakning.repository.AlertHistoryRepository;

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

        acknowledgementRepository.save(new Acknowledgement(
                "ack-" + UUID.randomUUID().toString(),
                ruleId,
                status,
                comment,
                acknowledgedBy,
                now
        ));

        alertHistoryRepository.save(new AlertHistoryEntry(
                "hist-" + UUID.randomUUID().toString(),
                ruleId,
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
}
