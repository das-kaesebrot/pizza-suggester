package eu.kaesebrot.dev.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table(name = "pizza")
public class Pizza implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String menuNumber;

    @Column(nullable = false)
    private BigDecimal price;
    @Column(nullable = false)
    @ElementCollection
    private Set<String> ingredients;

    @Column(nullable = false)
    private boolean isVegeterian;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private Timestamp modifiedAt;

    public Pizza() {
    }

    public Pizza(String menuNumber, BigDecimal price, Venue venue) {
        this.menuNumber = menuNumber;
        this.price = price;
        this.venue = venue;
    }

    public Pizza(String menuNumber, BigDecimal price, Set<String> ingredients, boolean isVegeterian, Venue venue) {
        this.menuNumber = menuNumber;
        this.price = price;
        this.ingredients = ingredients;
        this.isVegeterian = isVegeterian;
        this.venue = venue;
    }

    public void setMenuNumber(String menuNumber) {
        this.menuNumber = menuNumber;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setIngredients(Set<String> ingredients) {
        this.ingredients = ingredients;
    }

    public void setVegeterian(boolean vegeterian) {
        isVegeterian = vegeterian;
    }

    public Long getId() {
        return id;
    }

    public String getMenuNumber() {
        return menuNumber;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Set<String> getIngredients() {
        return ingredients;
    }

    public boolean isVegeterian() {
        return isVegeterian;
    }

    public Venue getVenue() {
        return venue;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getModifiedAt() {
        return modifiedAt;
    }
}
