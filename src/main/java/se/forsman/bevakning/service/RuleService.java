package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.repository.EditableMonitoringRuleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RuleService {
    private final EditableMonitoringRuleRepository repository;

    public RuleService(EditableMonitoringRuleRepository repository) {
        this.repository = repository;
    }

    public List<MonitoringRule> findAll() {
        return repository.findAllRules();
    }

    public MonitoringRule findById(String id) {
        return repository.findById(id);
    }

    public MonitoringRule createDailyRule(String sender, String receiver, String msgType, String description,
                                          int minExpected, int maxExpected, String deadline,
                                          int warningMinutesBeforeDeadline) {
        MonitoringRule rule = new MonitoringRule(
                "rule-" + UUID.randomUUID().toString(),
                trim(sender),
                trim(receiver),
                trim(msgType),
                trim(description),
                "DAILY",
                true,
                minExpected,
                maxExpected,
                trim(deadline),
                warningMinutesBeforeDeadline,
                new ArrayList<se.forsman.bevakning.domain.MonitoringWindow>()
        );
        repository.save(rule);
        return rule;
    }

    public MonitoringRule updateDailyRule(String id, String sender, String receiver, String msgType, String description,
                                          int minExpected, int maxExpected, String deadline,
                                          int warningMinutesBeforeDeadline, boolean active) {
        MonitoringRule rule = new MonitoringRule(
                id,
                trim(sender),
                trim(receiver),
                trim(msgType),
                trim(description),
                "DAILY",
                active,
                minExpected,
                maxExpected,
                trim(deadline),
                warningMinutesBeforeDeadline,
                new ArrayList<se.forsman.bevakning.domain.MonitoringWindow>()
        );
        repository.update(rule);
        return rule;
    }

    public void delete(String id) {
        repository.delete(id);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
