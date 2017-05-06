package com.martinandersson.qsb.impl;

import com.martinandersson.qsb.api.Message;
import java.time.Duration;
import java.time.Instant;
import static java.util.Objects.requireNonNull;

/**
 * A {@link Message} implementation that provide state-management of a {@code
 * queue} and {@code content}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public abstract class AbstractMessage implements Message
{
    private final String
            queue,
            content;
    
    
    
    /**
     * Constructs a message.
     * 
     * @param queue    queue
     * @param content  message content
     * 
     * @throws NullPointerException if any argument is {@code null}
     */
    public AbstractMessage(String queue, String content) {
        this.queue   = requireNonNull(queue);
        this.content = requireNonNull(content);
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final String get() {
        return content;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final String queue() {
        return queue;
    }
    
    
    
    /**
     * Mark this message as completed.
     */
    protected abstract void complete();
    
    /**
     * Try grab this message.<p>
     * 
     * The message timestamp is only used (stored) if message was successfully
     * grabbed.
     * 
     * @param timeout  message timeout
     * 
     * @return grab response
     */
    final GrabResponse tryGrab(Duration timeout) {
        Instant then = Instant.now();
        return tryGrab(then, then.plus(timeout));
    }
    
    /**
     * Try grab this message.<p>
     * 
     * The arguments are only used (stored) if message was successfully grabbed.
     * 
     * @param then     grabbed timestamp
     * @param expires  expiration timestamp
     * 
     * @return grab response
     */
    protected abstract GrabResponse tryGrab(Instant then, Instant expires);
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object other) {
        return super.equals(other);
    }
}