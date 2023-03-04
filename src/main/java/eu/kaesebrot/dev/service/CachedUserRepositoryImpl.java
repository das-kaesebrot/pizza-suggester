package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.model.CachedUser;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public abstract class CachedUserRepositoryImpl implements CachedUserRepository {

    private final ReadWriteLock lock;
    public CachedUserRepositoryImpl() {
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Always returns a CachedUser object. Creates a new user on the fly if it doesn't exist yet.
     * @param chatId to search by
     * @return The new or existing CachedUser
     */
    @Override
    public CachedUser findOrAddUserByChatId(Long chatId) {
        var userOptional = this.findById(chatId);

        if (userOptional.isPresent()) {
            return userOptional.get();
        }

        CachedUser user;
        try {
            lock.writeLock().lock();
            user = new CachedUser(chatId);
            user = this.save(user);
        }
        finally {
            lock.writeLock().unlock();
        }

        return user;
    }
}
