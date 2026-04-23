package eu.kaesebrot.dev.pizzabot.model;

import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(name = "ingredient")
public class Ingredient implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    private String name;

    private UserDiet compatibleUserDiet;

    private BigDecimal price;

    @ManyToMany(mappedBy = "ingredients")
    private Set<Pizza> pizzas;

    @ManyToMany(mappedBy = "ingredients")
    private Set<Venue> venues;

    public Ingredient() {
        price = BigDecimal.ZERO;
    }

    public Ingredient(String name, UserDiet compatibleUserDiet) {
        this();
        this.name = name;
        this.compatibleUserDiet = compatibleUserDiet;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserDiet getCompatibleUserDiet() {
        return compatibleUserDiet;
    }

    public void setCompatibleUserDiet(UserDiet diet) {
        this.compatibleUserDiet = diet;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Set<Pizza> getPizzas() {
        return pizzas;
    }

    public Set<Venue> getVenues() {
        return venues;
    }
}
