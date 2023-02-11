package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.model.CachedUser;
import org.jvnet.hk2.annotations.Service;
import org.springframework.data.jpa.repository.JpaRepository;

@Service
public interface CachedUserRepository extends JpaRepository<CachedUser, String> {

}
