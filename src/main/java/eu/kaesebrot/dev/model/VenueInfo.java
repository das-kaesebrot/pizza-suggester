package eu.kaesebrot.dev.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.net.URL;

@Entity
@Table(name = "venue_info")
public class VenueInfo implements Serializable {
    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false)
    private double longitude;
    @Column(nullable = false)
    private double latitude;
    @Column(nullable = false)
    private String phoneNumber;
    @Column(nullable = false)
    private URL url;

    @OneToOne(mappedBy = "VenueInfo")
    private Venue venue;
}
