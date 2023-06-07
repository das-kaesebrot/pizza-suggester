package eu.kaesebrot.dev.repository;

import eu.kaesebrot.dev.model.Venue;
import org.jvnet.hk2.annotations.Service;
import org.springframework.data.jpa.repository.JpaRepository;

@Service
public interface VenueRepository extends JpaRepository<Venue, Long> {

}
