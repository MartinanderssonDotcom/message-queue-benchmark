package com.martinandersson.mqb.impl.concurrent.stamped;

import com.martinandersson.mqb.api.QueueService;
import com.martinandersson.mqb.impl.AbstractQueueTest;
import java.time.Duration;
import java.util.function.Function;

/**
 * Unit tests for {@code ConcurrentQSWithStampedMessage}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ConcurrentQSWithStampedMessageTest extends AbstractQueueTest
{
    @Override
    protected final Function<Duration, QueueService> getFactory() {
        return ConcurrentQSWithStampedMessage::new;
    }
}