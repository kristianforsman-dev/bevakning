package se.forsman.bevakning.repository;

import se.forsman.bevakning.domain.FlowEvent;

import java.util.List;

public interface FlowRepository {
    List<FlowEvent> findAllFlowEvents();
}
