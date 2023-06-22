package eu.kaesebrot.dev.pizzabot.model;

import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

@Entity
@Table(name = "cached_user")
public class CachedUser implements Serializable {
    @Id
    @Column(updatable = false, nullable = false)
    private Long chatId;
    private UserDiet userDiet;

    private Integer pinnedInfoMessageId;
    private Integer lastInfoMessageTextHash;

    @ElementCollection(targetClass = UserState.class)
    @CollectionTable
    @Enumerated(EnumType.STRING)
    private Set<UserState> userState;
    private String languageTag;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue selectedVenue;
    @ElementCollection
    private List<Integer> selectedUserIngredients;
    private Integer currentIngredientMenuPage;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "adminkey_id")
    private AdminKey adminKey;
    private boolean isGlutenIntolerant;
    private boolean isLactoseIntolerant;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Timestamp modifiedAt;


    public CachedUser() {
        this.userState = Collections.synchronizedSet(EnumSet.noneOf(UserState.class));
        this.isGlutenIntolerant = false;
        this.isLactoseIntolerant = false;
        currentIngredientMenuPage = 0;
        selectedUserIngredients = new ArrayList<>();
        lastInfoMessageTextHash = -1;
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

    public void setGlutenIntolerant(boolean glutenIntolerant) {
        isGlutenIntolerant = glutenIntolerant;
    }

    public void toggleGlutenIntolerance() {
        isGlutenIntolerant = !isGlutenIntolerant;
    }

    public void setLactoseIntolerant(boolean lactoseIntolerant) {
        isLactoseIntolerant = lactoseIntolerant;
    }

    public void toggleLactoseIntolerance() {
        isLactoseIntolerant = !isLactoseIntolerant;
    }
    public void toggleSelectedIngredient(int ingredientIndex) {
        if (selectedUserIngredients.contains(ingredientIndex))
            selectedUserIngredients.remove((Integer) ingredientIndex); // cast to integer to remove by value instead of index
        else
            selectedUserIngredients.add(ingredientIndex);
    }

    public void clearSelectedIngredients() {
        selectedUserIngredients = new ArrayList<>();
    }

    public List<Integer> getSelectedUserIngredients() {
        return selectedUserIngredients;
    }

    public int getCurrentIngredientMenuPage() {
        return currentIngredientMenuPage;
    }

    public void setCurrentIngredientMenuPage(int page) {
        currentIngredientMenuPage = page;
    }

    public void removeState(UserState userState) {
        this.userState.remove(userState);
    }

    public void clearState() {
        this.setState(EnumSet.noneOf(UserState.class));
    }

    public void setPinnedInfoMessageId(Integer pinnedInfoMessageId) {
        this.pinnedInfoMessageId = pinnedInfoMessageId;
    }
    public void setLastInfoMessageTextHash(Integer infoMessageTextHash) {
        this.lastInfoMessageTextHash = infoMessageTextHash;
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

    public boolean isAdmin() {
        return adminKey != null;
    }

    public boolean isSuperAdmin() {
        return isAdmin() && adminKey.isSuperAdminKey();
    }

    public Integer getPinnedInfoMessageId() {
        return pinnedInfoMessageId;
    }
    public Integer getLastInfoMessageTextHash() {
        return lastInfoMessageTextHash;
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

    public Set<UserState> getState() {
        return userState;
    }
    public boolean hasState(UserState state) {
        return userState.contains(state);
    }

    public boolean isGlutenIntolerant() {
        return isGlutenIntolerant;
    }

    public boolean isLactoseIntolerant() {
        return isLactoseIntolerant;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getModifiedAt() {
        return modifiedAt;
    }
}
