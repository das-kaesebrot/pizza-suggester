package eu.kaesebrot.dev.pizzabot.repository;

import eu.kaesebrot.dev.pizzabot.model.AdminKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminKeyRepository extends JpaRepository<AdminKey, Long> {
    boolean existsByHashedKey(String key);
    List<AdminKey> findAllByClaimantIsNull();
    long countAllByClaimantIsNull();
}
