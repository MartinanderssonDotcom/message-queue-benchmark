package com.martinandersson.qsb.impl.concurrent.stamped;

import static java.text.MessageFormat.format;
import java.time.Instant;
import java.util.Objects;
import static java.util.Objects.hash;
import java.util.StringJoiner;

/**
 * Immutable value-based class of timestamps:
 * 
 * <ul>
 *   <li>Grabbed: When was the message grabbed/delivered.</li>
 *   <li>Expires: When does the message expire.</li>
 *   <li>Completed: When was the message completed.</li>
 * </ul>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class Timestamps
{
    private final Instant grabbed,
                          expires,
                          completed;
    
    
    
    public Timestamps(Instant grabbed, Instant expires) {
        if (expires.isBefore(grabbed)) {
            throw new IllegalArgumentException(format(
                    "Expiration date can not be historic. Grabbed: {0}. Expires: {1}.",
                    grabbed, expires));
        }
        
        this.grabbed   = grabbed;
        this.expires   = expires;
        this.completed = null;
    }
    
    private Timestamps(Instant grabbed, Instant expires, Instant completed) {
        this.grabbed   = grabbed;
        this.expires   = expires;
        this.completed = completed;
    }
    
    
    
    public Instant grabbed() {
        return grabbed;
    }
    
    public Instant expires() {
        return expires;
    }
    
    public boolean hasCompleted() {
        return completed != null;
    }
    
    public boolean hasExpired(Instant then) {
        return then.equals(expires) || then.isAfter(expires);
    }
    
    public Timestamps asCompleted() {
        if (hasCompleted()) {
            // Currently, this is dead code.
            throw new IllegalStateException("Already completed.");
        }
        
        return new Timestamps(grabbed, expires, Instant.now());
    }
    
    
    
    @Override
    public int hashCode() {
        return hash(grabbed, expires, completed);
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (other == null) {
            return false;
        }
        
        // Timestamps is non-final and we are contractually bound to keep symmetry.
        if (other.getClass() != Timestamps.class) {
            return false;
        }
        
        Timestamps that = (Timestamps) other;
        
        return this.grabbed.equals(that.grabbed) &&
               this.expires.equals(that.expires) &&
               Objects.equals(this.completed, that.completed);
    }
    
    @Override
    public String toString() {
        return new StringJoiner(" || ")
                .add(grabbed.toString())
                .add(expires.toString())
                .add(String.valueOf(completed))
                .toString();
    }
}