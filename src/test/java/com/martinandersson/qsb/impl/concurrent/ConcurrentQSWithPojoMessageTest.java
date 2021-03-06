package com.martinandersson.qsb.impl.concurrent;

import com.martinandersson.qsb.api.QueueService;
import com.martinandersson.qsb.impl.AbstractQSTest;
import java.time.Duration;
import java.util.function.Function;

/**
 * Unit tests for {@code ConcurrentQSWithPojoMessage}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ConcurrentQSWithPojoMessageTest extends AbstractQSTest
{
    @Override
    protected final Function<Duration, QueueService> getFactory() {
        return ConcurrentQSWithPojoMessage::new;
    }
}