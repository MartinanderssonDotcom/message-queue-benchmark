package com.martinandersson.qsb.impl.concurrent;

import com.martinandersson.qsb.impl.AbstractQS;
import static com.martinandersson.qsb.impl.Lockable.noLock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import static com.martinandersson.qsb.impl.Configuration.message;
import com.martinandersson.qsb.impl.PojoMessage;

/**
 * Uses {@code PojoMessage}, {@code ConcurrentHashMap} and {@code
 * ConcurrentLinkedQueue}.<p>
 * 
 * This queue service has at-least-once delivery semantics.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class ConcurrentQSWithPojoMessage extends AbstractQS<PojoMessage>
{
    public ConcurrentQSWithPojoMessage(Duration timeout) {
        super(message(PojoMessage::new).
              timeout(timeout).
              map(noLock(new ConcurrentHashMap<>())).
              queue(noLock(ConcurrentLinkedQueue::new)));
    }
}