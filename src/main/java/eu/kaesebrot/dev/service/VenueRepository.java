package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {

}
