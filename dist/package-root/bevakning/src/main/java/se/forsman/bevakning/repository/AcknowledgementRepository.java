package se.forsman.bevakning.repository;

import se.forsman.bevakning.domain.Acknowledgement;
import java.util.List;

public interface AcknowledgementRepository {
    List<Acknowledgement> findAll();
    void save(Acknowledgement acknowledgement);
}
