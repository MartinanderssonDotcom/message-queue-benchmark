package com.martinandersson.qsb.benchmark;

import com.martinandersson.qsb.api.QueueService;
import com.martinandersson.qsb.impl.concurrent.ConcurrentQSWithPojoMessage;
import com.martinandersson.qsb.impl.concurrent.atomic.ConcurrentQSWithAtomicMessage;
import com.martinandersson.qsb.impl.readwritelock.ReadWriteLockedQS;
import com.martinandersson.qsb.impl.serialized.SynchronizedQueueService;
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
    Synchronized     (SynchronizedQueueService::new),
    ReadWriteLock    (ReadWriteLockedQS::new),
    ConcurrentPojo   (ConcurrentQSWithPojoMessage::new),
    ConcurrentAtomic (ConcurrentQSWithAtomicMessage::new);
    
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