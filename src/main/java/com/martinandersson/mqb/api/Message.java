package com.martinandersson.mqb.api;

import java.util.function.Supplier;

/**
 * A message.<p>
 * 
 * Use {@link #get()} to get the message content.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public interface Message extends Supplier<String>
{
    /**
     * Returns the message Id.
     * 
     * The returned Id may be {@code -1}, in which case all messages from the
     * same queue service return {@code -1}. It is assumed that the queue
     * service implementation has no use for a value-based message identity.<p>
     * 
     * The id, if used, is not guaranteed to be unique across different queues.
     * 
     * @implSpec
     * The default implementation return {@code -1}.
     * 
     * @return the message Id
     */
    default long id() {
        return -1;
    }
    
    /**
     * Returns the queue [name] this message belongs to.
     * 
     * @return the queue [name] (never {@code null})
     */
    String queue();
    
    /**
     * Returns the message content.
     * 
     * @return the message content (never {@code null})
     */
    @Override
    public String get();
}