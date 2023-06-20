package eu.kaesebrot.dev.pizzabot.model;

import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "pizza")
public class Pizza implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    private String menuNumber;
    private String name;

    private BigDecimal price;

    @ElementCollection
    private List<String> ingredients;

    private UserDiet minimumUserDiet;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Timestamp modifiedAt;

    public Pizza() {
    }

    public Pizza(String menuNumber, String name, BigDecimal price, Venue venue) {
        this.menuNumber = menuNumber;
        this.name = name;
        this.price = price;
        this.venue = venue;
    }

    public Pizza(String menuNumber, String name, BigDecimal price, List<String> ingredients, UserDiet minimumUserDiet, Venue venue) {
        this.menuNumber = menuNumber;
        this.name = name;
        this.price = price;
        this.ingredients = ingredients;
        this.minimumUserDiet = minimumUserDiet;
        this.venue = venue;
    }

    public void setMenuNumber(String menuNumber) {
        this.menuNumber = menuNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public void setMinimumUserDiet(UserDiet diet) {
        minimumUserDiet = diet;
    }

    public Long getId() {
        return id;
    }

    public String getMenuNumber() {
        return menuNumber;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public UserDiet getMinimumUserDiet() {
        return minimumUserDiet;
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
