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
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotBlank(message = "{notEmpty}")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "{notEmpty}")
    @Column(name = "description", nullable = false)
    private String description;

    @OneToMany(mappedBy = "Venue")
    private Set<Pizza> pizzas;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "venueinfo_id")
    private VenueInfo venueInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private Timestamp modifiedAt;
}
