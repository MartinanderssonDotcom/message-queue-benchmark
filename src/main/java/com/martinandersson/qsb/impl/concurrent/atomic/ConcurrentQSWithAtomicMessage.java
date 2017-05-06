package com.martinandersson.qsb.impl.concurrent.atomic;

import com.martinandersson.qsb.impl.AbstractQS;
import static com.martinandersson.qsb.impl.Lockable.noLock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import static com.martinandersson.qsb.impl.Configuration.message;

/**
 * Uses {@code AtomicMessage}, {@code ConcurrentHashMap} and {@code
 * ConcurrentLinkedQueue}.<p>
 * 
 * This queue service has exactly-once delivery semantics (yet, lock-free).
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ConcurrentQSWithAtomicMessage extends AbstractQS<AtomicMessage>
{
    public ConcurrentQSWithAtomicMessage(Duration timeout) {
        super(message(AtomicMessage::new).
              timeout(timeout).
              map(noLock(new ConcurrentHashMap<>())).
              queue(noLock(ConcurrentLinkedQueue::new)));
    }
}