package eu.kaesebrot.dev.repository;

import eu.kaesebrot.dev.model.AdminKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface AdminKeyRepository extends JpaRepository<AdminKey, UUID> {
    
}
