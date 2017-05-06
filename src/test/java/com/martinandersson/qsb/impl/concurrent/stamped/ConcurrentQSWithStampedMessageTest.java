package com.martinandersson.qsb.impl.concurrent.stamped;

import com.martinandersson.qsb.api.QueueService;
import com.martinandersson.qsb.impl.AbstractQueueTest;
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