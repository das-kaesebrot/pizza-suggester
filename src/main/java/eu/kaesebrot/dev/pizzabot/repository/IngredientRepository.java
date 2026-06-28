package eu.kaesebrot.dev.pizzabot.repository;

import eu.kaesebrot.dev.pizzabot.model.Ingredient;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByName(String name);
    Set<Ingredient> findByVenue(Venue venue);
}
