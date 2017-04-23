package com.martinandersson.mqb.api;

import java.util.Arrays;

/**
 * A queue service provide a "FIFO" structure for message storage and
 * retrieval.<p>
 * 
 * All implementations support "duplicates". The following example store
 * <i>two</i> messages in "my-queue":<p>
 * 
 * <pre>{@code
 *   QueueService service = ...
 *   service.push("my-queue", "{hello: \"world\"}");
 *   service.push("my-queue", "{hello: \"world\"}");
 * }</pre>
 * 
 * Order (FIFO) is honored on a best-effort basis and messages are delivered
 * at-least-once. An implementation may support stronger semantics, i.e.,
 * guarantee ordering and provide exactly-once delivery. These are details
 * specified by such implementation.<p>
 * 
 * A queue service must support messages that time out. A message that timed out
 * will be re-delivered to next consumer. All consumers must therefore invoke
 * {@link #complete(Message)} to mark the completion of message processing.<p>
 * 
 * TODO: Define contract better. For example, it is legit for a message to be
 * "completed" twice.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public interface QueueService
{
    /**
     * Push specified {@code message} into specified {@code queue}.
     * 
     * @param queue    queue [name] (must not be {@code null})
     * @param message  message (must not be {@code null})
     */
    void push(String queue, String message);
    
    /**
     * Push specified {@code message} and optionally {@code more} messages,
     * given a specified {@code queue}.
     * 
     * @implSpec
     * The default implementation uses {@link #push(String, String)}.
     * 
     * @param queue    queue [name]
     * @param message  message
     * @param more     more messages (optional)
     * 
     * @throws NullPointerException if any argument is {@code null}
     */
    default void push(String queue, String message, String... more) {
        push(queue, message);
        Arrays.stream(more).forEach(m -> push(queue, m));
    }
    
    /**
     * Push specified {@code messages} into specified {@code queue}.
     * 
     * @implSpec
     * The default implementation uses {@link #push(String, String)}.
     * 
     * @param queue     queue [name] (must not be {@code null})
     * @param messages  messages (must not be {@code null})
     */
    default void push(String queue, Iterable<String> messages) {
        messages.forEach(m -> push(queue, m));
    }
    
    /**
     * Pull head of specified {@code queue}.
     * 
     * @param queue  queue [name]
     * 
     * @return a message, or {@code null} if the queue is empty
     */
    Message pull(String queue);
    
    /**
     * Complete specified {@code message}.<p>
     * 
     * Clients must call this method upon [successfully] completing a
     * message.<p>
     * 
     * Failure to do so within an undefined time period will cause the message
     * to be re-inserted as head of the queue.
     * 
     * @param message  message to remove (must not be {@code null})
     */
    void complete(Message message);
}