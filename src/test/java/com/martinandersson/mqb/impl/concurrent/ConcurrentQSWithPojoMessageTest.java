package com.martinandersson.mqb.impl.concurrent;

import com.martinandersson.mqb.api.QueueService;
import com.martinandersson.mqb.impl.AbstractQueueTest;
import java.time.Duration;
import java.util.function.Function;

/**
 * Unit tests for {@code ConcurrentQSWithPojoMessage}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ConcurrentQSWithPojoMessageTest extends AbstractQueueTest
{
    @Override
    protected final Function<Duration, QueueService> getFactory() {
        return ConcurrentQSWithPojoMessage::new;
    }
}