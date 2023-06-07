package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class CachedUserServiceImpl implements CachedUserService {

    private final ReadWriteLock lock;
    private final CachedUserRepository cachedUserRepository;

    public CachedUserServiceImpl(CachedUserRepository cachedUserRepository) {
        this.lock = new ReentrantReadWriteLock();
        this.cachedUserRepository = cachedUserRepository;
    }

    /**
     * Always returns a CachedUser object. Creates a new user on the fly if it doesn't exist yet.
     * @param chatId to search by
     * @return The new or existing CachedUser
     */
    @Override
    public CachedUser findOrAddUserByChatId(Long chatId) {
        var userOptional = cachedUserRepository.findById(chatId);

        if (userOptional.isPresent()) {
            return userOptional.get();
        }

        CachedUser user;
        try {
            lock.writeLock().lock();
            user = new CachedUser(chatId);
            user = cachedUserRepository.save(user);
        }
        finally {
            lock.writeLock().unlock();
        }

        return user;
    }
}
