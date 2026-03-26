package se.forsman.bevakning.repository;

import se.forsman.bevakning.domain.MonitoringRule;

import java.util.List;

public interface MonitoringRuleRepository {
    List<MonitoringRule> findAllRules();
}
