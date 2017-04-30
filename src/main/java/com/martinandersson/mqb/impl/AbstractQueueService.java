package com.martinandersson.mqb.impl;

import com.martinandersson.mqb.api.Message;
import com.martinandersson.mqb.api.QueueService;
import java.util.Iterator;
import static java.util.Objects.requireNonNull;
import java.util.Queue;

/**
 * Skeleton queue service implementation built on top of a {@code Map} that map
 * queue names to {@code Queue}s of messages.<p>
 * 
 * Access to these building blocks is routed through a {@link Lockable} which
 * determines if explicit locking is required and if so, how.<p>
 * 
 * The lockables are specified using a {@link Configuration} object.
 * 
 * @param <M>  concrete message implementation type
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public abstract class AbstractQueueService<M extends AbstractMessage> implements QueueService
{
    private static final boolean LAZY_EVICTION = true;
    
    private final Configuration<M>.Read c;
    
    
    
    /**
     * Constructs a {@code AbstractQueueService}.
     * 
     * @param config  configuration object
     */
    public AbstractQueueService(Configuration<M> config) {
        c = config.read();
    }
    
    
    
    private final ThreadLocal<Boolean> pushed
            = ThreadLocal.withInitial(() -> false);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final void push(String queue, String message) {
        push0(c.messageFactory().apply(queue, message));
    }
    
    private void push0(M message) {
        c.map().write(map -> {
            try {
                map.compute(message.queue(), (key, old) -> {
                    // ThreadLocal, local AtomicBoolean, local boolean[], which hack is least ugly?
                    if (pushed.get()) {
                        return old;
                    }
                    
                    Lockable<Queue<M>> lq = old != null ? old :
                            c.queueFactory().get();
                    
                    // TOOD: .compute() might be broken, plus we need no lock at
                    // all if we just created the queue. Maybe save ref to queue
                    // we added to? In identity based Set. But then that would
                    // be a totally unnecessary operation for non-concurrent
                    // maps.
                    
                    lq.write(q -> {
                        q.add(message);
                        pushed.set(true);
                    });
                    
                    return lq;
                });
            }
            finally {
                pushed.set(false);
            }
        });
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final Message pull(String queue) {
        boolean[] empty = {true};
        
        final Message v = c.map().readGet(m -> {
            Lockable<Queue<M>> lq = m.get(queue);

            if (lq == null) {
                // ..don't try a delete, no need!
                empty[0] = false;
                return null;
            }
            
            // Must write-lock since we remove completed messages.
            return lq.writeGet(q -> {
                Iterator<M> it = q.iterator();
                
                M msg = null;
                
                iteration: while (it.hasNext()) {
                    empty[0] = false;
                    
                    final M impl = it.next();
                    
                    switch (impl.tryGrab(c.timeout())) {
                        case COMPLETED:
                            it.remove();
                        case ACTIVE:
                            // Try next message..
                            break;
                        case SUCCEEDED:
                            msg = impl;
                            break iteration;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
                
                return msg;
            });
        });
        
        if (empty[0]) {
            tryDelete(queue);
        }
        
        return v;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final void complete(Message message) {
        if (!(requireNonNull(message) instanceof AbstractMessage)) {
            throw new IllegalArgumentException(
                    "Where the hell did you get this thing from?");
        }
        
        AbstractMessage impl = (AbstractMessage) message;
        
        impl.complete();
        
        if (!LAZY_EVICTION) {
            // Can not lock for reading first. Although we require re-entrancy,
            // there is no gaurantee a read lock can be upgraded to a write lock.
            // In fact, ReentrantReadWriteLock can not.
            
            c.map().write(m -> {
                Lockable<Queue<M>> lq = m.get(impl.queue());
                
                if (lq != null && lq.writeGet(q -> q.remove(impl) && q.isEmpty())) {
                    tryDelete(impl.queue());
                }
            });
        }
    }
    
    private void tryDelete(String queue) {
        // computeIfPresent() will remove the entry if the queue is empty.
        c.map().writeGet(m -> m.computeIfPresent(queue, (key, old) ->
                old.readGet(q -> q.isEmpty() ? null : old)));
    }
}