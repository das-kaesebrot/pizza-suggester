package eu.kaesebrot.dev.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class AdminKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID key;

    @OneToOne(mappedBy = "adminKey")
    private CachedUser claimant;

    public AdminKey() {
    }

    public UUID getKey() {
        return key;
    }

    public CachedUser getClaimant() {
        return claimant;
    }

    public boolean hasBeenClaimed() {
        return !(claimant == null);
    }

    @Override
    public String toString() {
        return "AdminKey{" + key.toString().replace("-", "") + "}";
    }
}
