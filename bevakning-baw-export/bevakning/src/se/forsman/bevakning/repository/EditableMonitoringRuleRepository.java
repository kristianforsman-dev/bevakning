package se.forsman.bevakning.repository;

import se.forsman.bevakning.domain.MonitoringRule;

public interface EditableMonitoringRuleRepository extends MonitoringRuleRepository {
    void save(MonitoringRule rule);
    void update(MonitoringRule rule);
    void delete(String ruleId);
    MonitoringRule findById(String ruleId);
}
