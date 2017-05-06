package com.martinandersson.mqb.impl.serialized;

import com.martinandersson.mqb.impl.AbstractQueueService;
import static com.martinandersson.mqb.impl.Configuration.message;
import static com.martinandersson.mqb.impl.Lockable.mutex;
import static com.martinandersson.mqb.impl.Lockable.noLock;
import com.martinandersson.mqb.impl.PojoMessage;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;

/**
 * A queue service that uses Java's {@code synchronized} keyword.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class SynchronizedQueueService extends AbstractQueueService<PojoMessage>
{
    public SynchronizedQueueService(Duration timeout) {
        super(message(PojoMessage::new).
              timeout(timeout).
              map(mutex(new HashMap<>())).
              queue(noLock(ArrayDeque::new)));
    }
}