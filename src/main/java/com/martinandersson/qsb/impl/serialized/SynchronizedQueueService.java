package com.martinandersson.qsb.impl.serialized;

import com.martinandersson.qsb.impl.AbstractQS;
import static com.martinandersson.qsb.impl.Configuration.message;
import static com.martinandersson.qsb.impl.Lockable.mutex;
import static com.martinandersson.qsb.impl.Lockable.noLock;
import com.martinandersson.qsb.impl.PojoMessage;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;

/**
 * A queue service that uses Java's {@code synchronized} keyword.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class SynchronizedQueueService extends AbstractQS<PojoMessage>
{
    public SynchronizedQueueService(Duration timeout) {
        super(message(PojoMessage::new).
              timeout(timeout).
              map(mutex(new HashMap<>())).
              queue(noLock(ArrayDeque::new)));
    }
}