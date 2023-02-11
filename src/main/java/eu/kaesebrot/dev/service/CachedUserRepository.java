package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.model.CachedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CachedUserRepository extends JpaRepository<CachedUser, String> {
}
