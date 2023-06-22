package eu.kaesebrot.dev.pizzabot.repository;

import eu.kaesebrot.dev.pizzabot.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    boolean existsByModifiedAtAfter(Timestamp timestamp);
    boolean existsByVenueInfoModifiedAtAfter(Timestamp timestamp);
}
