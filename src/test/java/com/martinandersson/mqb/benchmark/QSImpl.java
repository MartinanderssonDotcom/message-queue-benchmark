package com.martinandersson.mqb.benchmark;

import com.martinandersson.mqb.api.QueueService;
import com.martinandersson.mqb.impl.concurrent.ConcurrentQSWithPojoMessage;
import com.martinandersson.mqb.impl.concurrent.stamped.ConcurrentQSWithStampedMessage;
import com.martinandersson.mqb.impl.reentrantreadwritelock.ReentrantReadWriteLockedQueueService;
import com.martinandersson.mqb.impl.serialized.SynchronizedQueueService;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public enum QSImpl implements Supplier<QueueService>
{
    Synchronized           (SynchronizedQueueService::new),
    ReentrantReadWriteLock (ReentrantReadWriteLockedQueueService::new),
    ConcurrentPojo         (ConcurrentQSWithPojoMessage::new),
    ConcurrentStamped      (ConcurrentQSWithStampedMessage::new);
    
    private static final Duration MSG_TIMEOUT = Duration.ofDays(999);
    
    private final Function<Duration, QueueService> delegate;

    private QSImpl(Function<Duration, QueueService> delegate) {
        this.delegate = delegate;
    }

    @Override
    public final QueueService get() {
        return delegate.apply(MSG_TIMEOUT);
    }
}