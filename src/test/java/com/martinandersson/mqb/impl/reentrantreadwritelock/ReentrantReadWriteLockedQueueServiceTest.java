package com.martinandersson.mqb.impl.reentrantreadwritelock;

import com.martinandersson.mqb.api.QueueService;
import com.martinandersson.mqb.impl.AbstractQueueTest;
import java.time.Duration;
import java.util.function.Function;

/**
 * Unit tests for {@code ReentrantReadWriteLockedQueueService}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ReentrantReadWriteLockedQueueServiceTest extends AbstractQueueTest
{
    @Override
    protected final Function<Duration, QueueService> getFactory() {
        return ReentrantReadWriteLockedQueueService::new;
    }
}