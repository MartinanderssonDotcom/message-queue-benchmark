package com.martinandersson.qsb.impl.readwritelock;

import com.martinandersson.qsb.api.QueueService;
import com.martinandersson.qsb.impl.AbstractQSTest;
import java.time.Duration;
import java.util.function.Function;

/**
 * Unit tests for {@code ReadWriteLockedQS}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ReadWriteLockedQSTest extends AbstractQSTest
{
    @Override
    protected final Function<Duration, QueueService> getFactory() {
        return ReadWriteLockedQS::new;
    }
}