package eu.kaesebrot.dev.pizzabot.repository;

import eu.kaesebrot.dev.pizzabot.model.Venue;
import org.jvnet.hk2.annotations.Service;
import org.springframework.data.jpa.repository.JpaRepository;

@Service
public interface VenueRepository extends JpaRepository<Venue, Long> {

}
