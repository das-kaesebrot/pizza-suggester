package eu.kaesebrot.dev.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table(name = "venue")
public class Venue implements Serializable {
    @Version
    @Column(name = "version")
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotBlank(message = "{notEmpty}")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "{notEmpty}")
    @Column(name = "description", nullable = false)
    private String description;

    @OneToMany(mappedBy = "venue")
    private Set<Pizza> pizzaMenu;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "selectedVenue")
    private Set<CachedUser> usersUsingVenue;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "venueinfo_id")
    private VenueInfo venueInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private Timestamp modifiedAt;

    public Venue() {
    }

    public Venue(String name, String description, VenueInfo venueInfo) {
        this.name = name;
        this.description = description;
        this.venueInfo = venueInfo;
    }

    public Venue(String name, String description, Set<Pizza> pizzaMenu, VenueInfo venueInfo) {
        this.name = name;
        this.description = description;
        this.pizzaMenu = pizzaMenu;
        this.venueInfo = venueInfo;
    }

    public Long getVersion() {
        return version;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<Pizza> getPizzaMenu() {
        return pizzaMenu;
    }

    public Set<CachedUser> getUsersUsingVenue() {
        return usersUsingVenue;
    }

    public VenueInfo getVenueInfo() {
        return venueInfo;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getModifiedAt() {
        return modifiedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPizzaMenu(Set<Pizza> pizzaMenu) {
        this.pizzaMenu = pizzaMenu;
    }

    public void setVenueInfo(VenueInfo venueInfo) {
        this.venueInfo = venueInfo;
    }
}
