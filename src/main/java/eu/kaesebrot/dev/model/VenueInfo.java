package eu.kaesebrot.dev.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.net.URL;

@Entity
@Table(name = "venue_info")
public class VenueInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public VenueInfo() {
    }

    public VenueInfo(double longitude, double latitude, String phoneNumber) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.phoneNumber = phoneNumber;
    }

    public VenueInfo(double longitude, double latitude, String phoneNumber, URL url) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.phoneNumber = phoneNumber;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public URL getUrl() {
        return url;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setUrl(URL url) {
        this.url = url;
    }
}
