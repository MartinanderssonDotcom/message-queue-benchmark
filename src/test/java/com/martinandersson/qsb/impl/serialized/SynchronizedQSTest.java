package com.martinandersson.qsb.impl.serialized;

import com.martinandersson.qsb.api.QueueService;
import com.martinandersson.qsb.impl.AbstractQueueTest;
import java.time.Duration;
import java.util.function.Function;

/**
 * Unit tests for {@code SerializedQueueService}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class SynchronizedQSTest extends AbstractQueueTest
{
    @Override
    protected final Function<Duration, QueueService> getFactory() {
        return SynchronizedQS::new;
    }
}