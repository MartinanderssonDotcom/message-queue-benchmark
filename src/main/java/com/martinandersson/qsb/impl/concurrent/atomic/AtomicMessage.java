package com.martinandersson.qsb.impl.concurrent.atomic;

import com.martinandersson.qsb.api.Message;
import com.martinandersson.qsb.impl.AbstractMessage;
import com.martinandersson.qsb.impl.GrabResponse;
import static com.martinandersson.qsb.impl.GrabResponse.ACTIVE;
import static com.martinandersson.qsb.impl.GrabResponse.COMPLETED;
import static com.martinandersson.qsb.impl.GrabResponse.SUCCEEDED;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link Message} with atomic timestamps.<p>
 * 
 * TODO: Describe and rename to AtomicMessage.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
final class AtomicMessage extends AbstractMessage
{
    /**
     * Any thread that succeed setting this reference, has also succeeded in
     * grabbing the message. Current "processing state" of a grabbed/delivered
     * message may be analyzed using the Timestamps API. A re-delivery will set
     * this reference to a new Timestamps instance.
     */
    private final AtomicReference<Timestamps> timestamps;
    
    
    
    /**
     * Constructs an {@code AtomicMessage}.
     * 
     * @param queue    queue
     * @param content  message content
     */
    public AtomicMessage(String queue, String content) {
        super(queue, content);
        this.timestamps = new AtomicReference<>();
    }
    
    
    
    private Timestamps timestamps() {
        return timestamps.get();
    }
    
    @Override
    public void complete() {
        final Timestamps stamps = timestamps();
        
        if (stamps == null) {
            throw new IllegalStateException("Please grab the message first lol.");
        }
        
        if (stamps.hasCompleted()) {
            return;
        }
        
        // TODO: Rethink this a bit. Current design guarantee exactly-once only
        //       during contention of grabbing the message. There's still a race
        //       between complete() and tryGrab().
        timestamps.set(stamps.asCompleted());
    }
    
    @Override
    public GrabResponse tryGrab(Instant then, Instant expires) {
        final Timestamps oldStamps = timestamps();
        
        if (oldStamps != null) {
            if (oldStamps.hasCompleted()) {
                return COMPLETED;
            }
            
            if (oldStamps.hasExpired(then)) {
                // TODO: This is broken. We need to compareAndSet! Or something.
                //       Currently, this message will be continously redelivered.
                return SUCCEEDED;
            }
            
            return ACTIVE;
        }
        
        /*
        
        We only allow to set new timestamps if "expected old stamps" remain
        the same. If the object reference has changed since we first read it,
        then this can only be because of one of two things:
        
          1) Someone else grabbed/re-delivered the message or,
          2) message was marked as completed.
        
        In both cases, this method invocation will return false and the
        calling thread need not bother any more. He can move on with his
        life, trying to grab another message or have a cup of coffee.
        
        */
        
        boolean updated = timestamps.compareAndSet(
                    oldStamps, new Timestamps(then, expires));
        
        if (updated) {
            return SUCCEEDED;
        }
        
        // TODO: We could return COMPLETED_OR_ACTIVE, which we know client will
        // treat the same as ACTIVE anyways, meaning he will just continue and
        // try to grab the next message. Or we do a dangerous spin (will most
        // likely return immediately).
        // 
        // TODO: Use do-while instead. But sure, keep the same spin concept.
        return tryGrab(then, expires);
    }
}