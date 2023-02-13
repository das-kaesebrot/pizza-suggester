package eu.kaesebrot.dev.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cached_user")
public class CachedUser implements Serializable {
    @Id
    @Column(updatable = false, nullable = false)
    private Long chatId;

    private boolean isAdmin;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue selectedVenue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private Timestamp modifiedAt;


    public CachedUser() {
        this.isAdmin = false;
    }

    public CachedUser(Long chatId) {
        this();
        this.chatId = chatId;
    }

    public void setAdminStatus(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public void setSelectedVenue(Venue selectedVenue) {
        this.selectedVenue = selectedVenue;
    }

    public String getChatId() {
        return chatId;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public Venue getSelectedVenue() {
        return selectedVenue;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getModifiedAt() {
        return modifiedAt;
    }
}
