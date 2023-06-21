package eu.kaesebrot.dev.pizzabot.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "venue")
public class Venue implements Serializable {
    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    private String name;

    private String description;

    @OneToMany(mappedBy = "venue")
    private List<Pizza> pizzaMenu;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "selectedVenue")
    private Set<CachedUser> usersUsingVenue;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "venueinfo_id")
    private VenueInfo venueInfo;

    private BigDecimal glutenFreeMarkup;
    private BigDecimal lactoseIntoleranceMarkup;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Timestamp modifiedAt;

    public Venue() {
        venueInfo = new VenueInfo();
    }

    public Venue(String name, String description, VenueInfo venueInfo) {
        this.name = name;
        this.description = description;
        this.venueInfo = venueInfo;
    }

    public Venue(String name, String description, List<Pizza> pizzaMenu, VenueInfo venueInfo) {
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

    public List<Pizza> getPizzaMenu() {
        return pizzaMenu;
    }

    public Set<CachedUser> getUsersUsingVenue() {
        return usersUsingVenue;
    }

    public BigDecimal getGlutenFreeMarkup() {
        return glutenFreeMarkup;
    }

    public BigDecimal getLactoseIntoleranceMarkup() {
        return lactoseIntoleranceMarkup;
    }

    public boolean supportsGlutenFree() {
        return glutenFreeMarkup != null;
    }

    public boolean supportsLactoseIntolerance() {
        return lactoseIntoleranceMarkup != null;
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

    public void setPizzaMenu(List<Pizza> pizzaMenu) {
        this.pizzaMenu = pizzaMenu;
    }

    public void setVenueInfo(VenueInfo venueInfo) {
        this.venueInfo = venueInfo;
    }

    public void setGlutenFreeMarkup(BigDecimal glutenFreeMarkup) {
        this.glutenFreeMarkup = glutenFreeMarkup;
    }

    public void setLactoseIntoleranceMarkup(BigDecimal lactoseIntoleranceMarkup) {
        this.lactoseIntoleranceMarkup = lactoseIntoleranceMarkup;
    }

    public void disableGlutenFreeSupport() {
        setGlutenFreeMarkup(null);
    }

    public void disableLactoseIntoleranceSupport() {
        setLactoseIntoleranceMarkup(null);
    }
}
