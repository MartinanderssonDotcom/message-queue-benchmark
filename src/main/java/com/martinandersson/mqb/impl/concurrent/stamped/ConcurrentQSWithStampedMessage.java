package com.martinandersson.mqb.impl.concurrent.stamped;

import com.martinandersson.mqb.impl.AbstractQueueService;
import static com.martinandersson.mqb.impl.Lockable.noLock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import static com.martinandersson.mqb.impl.Configuration.message;

/**
 * Uses {@code StampedMessage}, {@code ConcurrentHashMap} and {@code
 * ConcurrentLinkedQueue}.<p>
 * 
 * This queue service has exactly-once delivery semantics (yet, lock-free).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ConcurrentQSWithStampedMessage extends AbstractQueueService<StampedMessage>
{
    public ConcurrentQSWithStampedMessage(Duration timeout) {
        super(message(StampedMessage::new).
              timeout(timeout).
              map(noLock(new ConcurrentHashMap<>())).
              queue(noLock(ConcurrentLinkedQueue::new)));
    }
}