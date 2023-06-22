package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.model.AdminKey;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.repository.AdminKeyRepository;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class AdminKeyServiceImpl implements AdminKeyService {
    private final AdminKeyRepository adminKeyRepository;
    private final CachedUserRepository cachedUserRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminKeyServiceImpl(AdminKeyRepository adminKeyRepository, CachedUserRepository cachedUserRepository) {
        this.adminKeyRepository = adminKeyRepository;
        this.cachedUserRepository = cachedUserRepository;
    }

    @Override
    public String generateNewAdminKey(boolean grantsSuperAdminRights) {

        while (true) {
            var seed = secureRandom.generateSeed(secureRandom.nextInt(24, 32));
            var rawKey = HexFormat.of().formatHex(seed);

            var hashedKey = passwordEncoder.encode(rawKey);

            if (!adminKeyRepository.existsByHashedKey(hashedKey)) {
                var keyObject = new AdminKey(grantsSuperAdminRights, hashedKey);

                adminKeyRepository.saveAndFlush(keyObject);

                return rawKey;
            }
        }
    }

    @Override
    public boolean isKeyRepositoryEmpty() {
        return adminKeyRepository.count() == 0;
    }

    @Override
    public void redeemAdminKeyForUser(CachedUser user, String rawKey) {

        for (var keyObject: adminKeyRepository.findAllByClaimantIsNull()) {
            if (passwordEncoder.matches(rawKey, keyObject.getHashedKey())) {
                user.setAdminKey(keyObject);

                cachedUserRepository.save(user);

                return;
            }
        }

        throw new RuntimeException(String.format("Couldn't find admin key %s in repository", rawKey));
    }

}
