package com.martinandersson.qsb.impl.concurrent.atomic;

import com.martinandersson.qsb.api.QueueService;
import com.martinandersson.qsb.impl.AbstractQSTest;
import java.time.Duration;
import java.util.function.Function;

/**
 * Unit tests for {@code ConcurrentQSWithAtomicMessage}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ConcurrentQSWithAtomicMessageTest extends AbstractQSTest
{
    @Override
    protected final Function<Duration, QueueService> getFactory() {
        return ConcurrentQSWithAtomicMessage::new;
    }
}