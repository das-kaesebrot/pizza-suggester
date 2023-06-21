package eu.kaesebrot.dev.pizzabot.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;

@Entity
@Table(name = "venue_info")
public class VenueInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    private Double latitude;
    private Double longitude;

    private String phoneNumber;

    private String address;

    private URL url;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Timestamp modifiedAt;

    public VenueInfo() {
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    public VenueInfo(String address) {
        this();
        this.address = address;
    }

    public VenueInfo(Double latitude, Double longitude, String phoneNumber, URL url) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public Double getLatitude() {
        return latitude;
    }
    public Double getLongitude() {
        return longitude;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public URL getUrl() {
        return url;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getModifiedAt() {
        return modifiedAt;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setPhoneNumber(String phoneNumber) {
        phoneNumber = phoneNumber.replaceAll(" ", "");

        if (!phoneNumber.matches("^\\+[0-9 ]+$"))
            throw new RuntimeException("Phone number needs to start with an area code!");

        this.phoneNumber = phoneNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setUrl(String url) throws MalformedURLException {
        this.url = new URL(url);
    }
}
