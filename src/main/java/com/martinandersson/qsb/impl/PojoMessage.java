package com.martinandersson.qsb.impl;

import static com.martinandersson.qsb.impl.GrabResponse.ACTIVE;
import static com.martinandersson.qsb.impl.GrabResponse.COMPLETED;
import static com.martinandersson.qsb.impl.GrabResponse.SUCCEEDED;
import java.time.Instant;
import static java.util.Objects.requireNonNull;

/**
 * A non thread-safe message implementation.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class PojoMessage extends AbstractMessage
{
    private Instant expires;
    
    private boolean completed;
    
    
    /**
     * Constructs a {@code PojoMessage}.
     * 
     * @param queue    queue
     * @param content  content
     * 
     * @throws NullPointerException if any argument is {@code null}
     */
    public PojoMessage(String queue, String content) {
        super(queue, content);
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void complete() {
        if (expires == null) {
            throw new IllegalStateException("Please grab the message first lol.");
        }
        
        completed = true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected GrabResponse tryGrab(Instant then, Instant expires) {
        if (this.expires == null) {
            this.expires = requireNonNull(expires);
            return SUCCEEDED;
        }
        
        if (completed) {
            return COMPLETED;
        }
        
        if (hasExpired(then)) {
            this.expires = requireNonNull(expires);
            return SUCCEEDED;
        }
        
        return ACTIVE;
    }
    
    private boolean hasExpired(Instant then) {
        return then.equals(expires) || then.isAfter(expires);
    }
}