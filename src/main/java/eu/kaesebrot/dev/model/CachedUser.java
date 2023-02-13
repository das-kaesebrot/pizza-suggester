package eu.kaesebrot.dev.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.EnumSet;

@Entity
@Table(name = "cached_user")
public class CachedUser implements Serializable {
    @Id
    @Column(updatable = false, nullable = false)
    private Long chatId;

    private boolean isAdmin;
    private EnumSet<UserState> userState;

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
        this.userState = EnumSet.noneOf(UserState.class);
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

    public void setState(EnumSet<UserState> userStateSet) {
        this.userState = userStateSet;
    }
    public void addState(UserState userState) {
        this.userState.add(userState);
    }
    public void removeState(UserState userState) {
        this.userState.remove(userState);
    }

    public void clearState() {
        this.setState(EnumSet.noneOf(UserState.class));
    }

    public Long getChatId() {
        return chatId;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public Venue getSelectedVenue() {
        return selectedVenue;
    }

    public EnumSet<UserState> getState() {
        return userState;
    }
    public boolean hasState(UserState state) {
        return userState.contains(state);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getModifiedAt() {
        return modifiedAt;
    }
}
