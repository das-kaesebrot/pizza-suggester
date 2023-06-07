package eu.kaesebrot.dev.pizzabot.model;

import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
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
    private UserDiet userDiet;
    private EnumSet<UserState> userState;
    private String languageTag;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue selectedVenue;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "adminkey_id")
    private AdminKey adminKey;

    private boolean isSuperAdmin;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Timestamp modifiedAt;


    public CachedUser() {
        this.userState = EnumSet.noneOf(UserState.class);
    }

    public CachedUser(Long chatId) {
        this();
        this.chatId = chatId;
    }

    public void setUserDiet(UserDiet diet) {
        this.userDiet = diet;
    }

    public void setSelectedVenue(Venue selectedVenue) {
        this.selectedVenue = selectedVenue;
    }

    public void setLanguageTag(String languageTag) {
        this.languageTag = languageTag;
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

    public void setAdminKey(AdminKey adminKey) {
        if (adminKey.hasBeenClaimed()) {
            throw new RuntimeException("Admin key has already been claimed");
        }

        this.adminKey = adminKey;
    }

    public void setAdminKeyAsSuperAdmin(AdminKey adminKey) {
        setAdminKey(adminKey);

        this.isSuperAdmin = true;
    }

    public boolean isAdmin() {
        return adminKey != null;
    }

    public boolean isSuperAdmin() {
        return isAdmin() && isSuperAdmin;
    }

    public UserDiet getUserDiet() {
        return userDiet;
    }

    public Venue getSelectedVenue() {
        return selectedVenue;
    }

    public String getLanguageTag() {
        return languageTag;
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
