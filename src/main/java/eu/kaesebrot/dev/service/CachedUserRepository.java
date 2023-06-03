package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.model.CachedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CachedUserRepository extends JpaRepository<CachedUser, Long> {
}
