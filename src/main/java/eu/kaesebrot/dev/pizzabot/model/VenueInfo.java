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
    public String getCoordinatesString() {
        var string = new StringBuilder();

        if (latitude >= 0.0)
            string.append("N");
        else
            string.append("S");

        string.append(latitude).append(", ");

        if (longitude < 0.0)
            string.append("W");
        else
            string.append("E");

        string.append(longitude);

        return string.toString();
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
        if (Math.abs(latitude) > 90.0)
            throw new IllegalArgumentException(String.format("Latitude can't be bigger than 90.0 or smaller than -90.0! Given value: %f", latitude));

        this.latitude = latitude;
    }
    public void setLongitude(Double longitude) {
        if (Math.abs(longitude) > 180)
            throw new IllegalArgumentException(String.format("Latitude can't be bigger than 180.0 or smaller than -180.0! Given value: %f", longitude));

        this.longitude = longitude;
    }

    public void setCoordinatesByString(String coordinatesString) {
        var latLong = coordinatesString
                .replaceAll(" ", "")
                .split(",");

        if (latLong.length != 2)
            throw new IllegalArgumentException(String.format("Coordinates provided aren't in the right format! Given data: %s", coordinatesString));

        String REGEX_LATITUDE_NORTH = "^(?i)([N]\\d+.\\d+)|(\\d+.\\d+[N])$";
        String REGEX_LATITUDE_SOUTH = "^(?i)([S]\\d+.\\d+)|(\\d+.\\d+[S])$";

        String REGEX_LONGITUDE_WEST = "^(?i)([W]\\d+.\\d+)|(\\d+.\\d+[W])$";
        String REGEX_LONGITUDE_EAST = "^(?i)([E]\\d+.\\d+)|(\\d+.\\d+[E])$";

        var latitudeString = latLong[0];
        var longitudeString = latLong[1];

        if (latitudeString.matches(REGEX_LATITUDE_NORTH)
                || latitudeString.matches(REGEX_LATITUDE_SOUTH)) {

            if (latitudeString.matches(REGEX_LATITUDE_SOUTH))
                latitudeString = "-" + latitudeString;

            latitudeString = latitudeString.replaceAll("(?i)[NS]", "");
        }

        this.setLatitude(Double.parseDouble(latitudeString));

        if (longitudeString.matches(REGEX_LONGITUDE_WEST)
                || longitudeString.matches(REGEX_LONGITUDE_EAST)) {

            if (longitudeString.matches(REGEX_LONGITUDE_EAST))
                longitudeString = "-" + longitudeString;

            longitudeString = longitudeString.replaceAll("(?i)[WE]", "");
        }

        this.setLongitude(Double.parseDouble(longitudeString));
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
