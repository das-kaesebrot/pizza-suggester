package eu.kaesebrot.dev.pizzabot.repository;

import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    List<Pizza> findByVenue(Venue venue);
    List<Pizza> findByVenueAndMinimumUserDietGreaterThanEqual(Venue venue, UserDiet diet);
}
