package eu.kaesebrot.dev.pizzabot.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class AdminKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID key;

    @OneToOne(mappedBy = "adminKey")
    private CachedUser claimant;

    private boolean grantsSuperAdmin;

    public AdminKey() {
        grantsSuperAdmin = false;
    }

    public AdminKey(boolean grantsSuperAdmin) {
        this.grantsSuperAdmin = grantsSuperAdmin;
    }

    public UUID getKey() {
        return key;
    }

    public String getKeyString() {
        return key.toString().replace("-", "");
    }

    public CachedUser getClaimant() {
        return claimant;
    }

    public boolean hasBeenClaimed() {
        return !(claimant == null);
    }

    public boolean isSuperAdminKey() {
        return grantsSuperAdmin;
    }

    @Override
    public String toString() {
        return "AdminKey{" + key.toString().replace("-", "") + "}";
    }
}
