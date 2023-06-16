package eu.kaesebrot.dev.pizzabot.repository;

import eu.kaesebrot.dev.pizzabot.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByModifiedAtGreaterThan(Timestamp timestamp);
}
