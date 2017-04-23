package com.martinandersson.mqb.impl.serialized;

import com.martinandersson.mqb.api.QueueService;
import com.martinandersson.mqb.impl.AbstractQueueTest;
import java.time.Duration;
import java.util.function.Function;

/**
 * Unit tests for {@code SerializedQueueService}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class SynchronizedQueueServiceTest extends AbstractQueueTest
{
    @Override
    protected final Function<Duration, QueueService> getFactory() {
        return SynchronizedQueueService::new;
    }
}