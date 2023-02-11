package eu.kaesebrot.dev.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(name = "pizza")
public class Pizza implements Serializable {
    @Id
    @Column(updatable = false, nullable = false)
    private String number;

    @Column(nullable = false)
    private BigDecimal price;
    @Column(nullable = false)
    @ElementCollection
    private Set<String> ingredients;

    private boolean isVegeterian;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;
}
