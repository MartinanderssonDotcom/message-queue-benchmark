package com.martinandersson.mqb.impl;

import com.martinandersson.mqb.api.QueueService;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Configures {@link AbstractQueueService}.<p>
 * 
 * Configurations can be overridden many times, until someone {@linkplain
 * #read() reads} the configuration.
 * 
 * @param <M>  concrete message implementation type
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class Configuration<M>
{
    /**
     * Constructs a new {@code Configuration}.
     * 
     * @param <M>      concrete message type
     * @param factory  message factory
     * 
     * @return a new {@code Configuration}
     */
    public static <M> Configuration<M> message(BiFunction<String, String, M> factory) {
        return new Configuration<>(factory);
    }
    
    private Configuration(BiFunction<String, String, M> messageFactory) {
        this.messageFactory = messageFactory;
    }
    
    
    
    private final BiFunction<String, String, M> messageFactory;
    
    private Duration timeout;
    
    private Lockable<Map<String, Lockable<Queue<M>>>> map;
    
    private Supplier<Lockable<Queue<M>>> queueFactory;
    
    
    
    /**
     * Set message timeout.
     * 
     * @param timeout  message timeout
     * 
     * @return this, for chaining
     * 
     * @throws IllegalArgumentException  if {@code timeout} is negative
     * 
     * @see QueueService
     */
    public Configuration<M> timeout(Duration timeout) {
        requireNotBuilt();
        
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Negative: " + timeout);
        }
        
        this.timeout = timeout;
        return this;
    }
    
    /**
     * Set map (that holds all queues).
     * 
     * @param map  map
     * 
     * @return this, for chaining
     * 
     * @see AbstractQueueService
     */
    public Configuration<M> map(Lockable<Map<String, Lockable<Queue<M>>>> map) {
        requireNotBuilt();
        this.map = map;
        return this;
    }
    
    /**
     * 
     * @param factory  queue factory
     * 
     * @return this, for chaining
     * 
     * @see AbstractQueueService
     */
    public Configuration<M> queue(Supplier<Lockable<Queue<M>>> factory) {
        requireNotBuilt();
        this.queueFactory = factory;
        return this;
    }
    
    
    
    private void requireNotBuilt() {
        if (built) {
            throw new IllegalStateException();
        }
    }
    
    private boolean built;
    
    /**
     * Complete the configuration object.<p>
     * 
     * This configuration object can not be mutated after this method has been
     * called. Attempts to call a set method after this point will blow up in an
     * {@code IllegalStateException}.
     * 
     * @return a read interface of this configuration object
     */
    Read read() {
        built = true;
        return new Read();
    }
    
    
    
    class Read {
        /** @see QueueService  */
        Duration timeout() {
            return timeout;
        }
        
        /** @see AbstractQueueService  */
        Lockable<Map<String, Lockable<Queue<M>>>> map() {
            return map;
        }
        
        BiFunction<String, String, M> messageFactory() {
            return messageFactory;
        }
        
        /** @see AbstractQueueService  */
        Supplier<Lockable<Queue<M>>> queueFactory() {
            return queueFactory;
        }
    }
}