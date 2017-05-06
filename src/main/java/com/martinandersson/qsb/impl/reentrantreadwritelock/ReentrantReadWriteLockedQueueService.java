package com.martinandersson.qsb.impl.reentrantreadwritelock;

import com.martinandersson.qsb.impl.AbstractQueueService;
import static com.martinandersson.qsb.impl.Configuration.message;
import static com.martinandersson.qsb.impl.Lockable.readWrite;
import com.martinandersson.qsb.impl.PojoMessage;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Queue service that use {@code ReentrantReadWriteLock} for synchronization.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ReentrantReadWriteLockedQueueService extends AbstractQueueService<PojoMessage>
{
    /**
     * Constructs a {@code ReentrantReadWriteLockedQueueService}.
     * 
     * @param timeout  message timeout
     */
    public ReentrantReadWriteLockedQueueService(Duration timeout) {
        super(message(PojoMessage::new).
              timeout(timeout).
              map(readWrite(new HashMap<>(), new ReentrantReadWriteLock())).
              queue(readWrite(ArrayDeque::new, ReentrantReadWriteLock::new)));
    }
}