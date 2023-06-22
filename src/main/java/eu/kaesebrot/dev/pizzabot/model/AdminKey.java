package eu.kaesebrot.dev.pizzabot.model;

import jakarta.persistence.*;

@Entity
public class AdminKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "adminKey")
    private CachedUser claimant;

    private String hashedKey;

    private boolean grantsSuperAdmin;

    public AdminKey() {
        grantsSuperAdmin = false;
    }

    public AdminKey(boolean grantsSuperAdmin, String hashedKey) {
        this.hashedKey = hashedKey;
        this.grantsSuperAdmin = grantsSuperAdmin;
    }

    public String getHashedKey() {
        return hashedKey;
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
        return String.format("AdminKey{%s}", hashedKey);
    }
}
