package com.martinandersson.mqb.impl;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * API for read/write actions on a lockable thing.<p>
 * 
 * What is here referred to as an "action" is labeled as a "task" in the API.<p>
 * 
 * How - and even if - a lock is used is implementation details. This API unify
 * the void or return-based actions a client wish to execute on the entity.<p>
 * 
 * Actions are branded as <i>unsafe</i>, <i>read</i> or <i>write</i>.<p>
 * 
 * Unsafe actions must be routed directly to the lockable thing. Client calling
 * this method has yet made the lockable visible to other threads.<p>
 * 
 * Read actions does not mutate the lockable thing, write actions do.<p>
 * 
 * The implementation does not need to use locks. In particular, the
 * implementation does not need to separate between readers and writers. Any
 * method declared in this interface may proceed without blocking, or may block
 * with no specified timeout or interruption capability.<p>
 * 
 * Lock-based implementations must support reentrancy. Write locks must be
 * downgradable to read locks, but not necessarily the other way around.
 * 
 * @param <T>  type of the lockable thing
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public interface Lockable<T>
{
    /**
     * Invoke {@code task} with the lockable thing without passing through the
     * lock.
     * 
     * @param task  task to perform
     */
    default void unsafe(Consumer<T> task) {
        unsafeGet(t -> {
            task.accept(t);
            return null;
        });
    }
    
    /**
     * Invoke {@code task} with the lockable thing without passing through the
     * lock.
     * 
     * @param <V>   function return type
     * @param task  task to perform
     * 
     * @return function return value
     */
    <V> V unsafeGet(Function<T, V> task);
    
    /**
     * Invoke the read-only {@code task} with the lockable thing.
     * 
     * @implSpec
     * The default implementation use {@code readGet(Function)}.
     * 
     * @param task  task to perform
     */
    default void read(Consumer<T> task) {
        readGet(t -> {
            task.accept(t);
            return null;
        });
    }
    
    /**
     * Invoke the read-only {@code task} with the lockable thing.
     * 
     * @implSpec
     * The default implementation use {@code writeGet(Function)}.
     * 
     * @param <V>   function return type
     * @param task  task to perform
     * 
     * @return function return value
     */    
    default <V> V readGet(Function<T, V> task) {
        return writeGet(task);
    }
    
    /**
     * Invoke the {@code task} with possible side-effects on the lockable thing.
     * 
     * @implSpec
     * The default implementation use {@code writeGet(Function)}.
     * 
     * @param task  task to perform
     */
    default void write(Consumer<T> task) {
        writeGet(t -> {
            task.accept(t);
            return null;
        });
    }
    
    /**
     * Invoke the {@code task} with possible side-effects on the lockable thing.
     * 
     * @param <V>   function return type
     * @param task  task to perform
     * 
     * @return function return value
     */
    <V> V writeGet(Function<T, V> task);
    
    
    
    /*
     *  -----------
     * | FACTORIES |
     *  -----------
     */
    
    // TODO: Make all anonymous classes. We want same overhead cost! Or at least.. hope for it.
    
    /**
     * Constructs a {@link Lockable} that uses no locking.<p>
     * 
     * Obviously we assume that the thing itself is thread-safe and all tasks
     * provided to the returned lockable will be invoked immediately without
     * blocking.
     * 
     * @param <T>    type of the lockable thing
     * @param thing  thing we want to "lock down"
     * 
     * @return a {@code Lockable} with no locking
     */
    static <T> Lockable<T> noLock(T thing) {
        return new Lockable<T>(){
            @Override public <V> V unsafeGet(Function<T, V> task) {
                return task.apply(thing);
            }
            
            @Override public <V> V writeGet(Function<T, V> task) {
                return task.apply(thing);
            }
        };
    }
    
    /**
     * Constructs a supplier of {@link Lockable} things that uses no locking.<p>
     * 
     * Obviously we assume that the things are thread-safe and all tasks
     * provided to the supplied lockables will be invoked immediately without
     * blocking.<p>
     * 
     * The specified supplier of things is polled every time someone poll the
     * supplier that this method returns.
     * 
     * @param <T>    type of the lockable thing
     * @param thing  supplier of things we want to "lock down"
     * 
     * @return a supplier of {@code Lockable} that uses no locking
     */
    static <T> Supplier<Lockable<T>> noLock(Supplier<T> thing) {
        return () -> noLock(thing.get());
    }
    
    /**
     * Constructs a {@link Lockable} which will be locked using Java's {@code
     * synchronized} keyword.
     * 
     * @param <T>    type of the lockable thing
     * @param thing  thing we want to "lock down"
     * 
     * @return a {@code Lockable} using {@code synchronized}
     */
    static <T> Lockable<T> mutex(T thing) {
        return new Lockable<T>(){
            @Override public <V> V unsafeGet(Function<T, V> task) {
                return task.apply(thing);
            }
            
            @Override public <V> V writeGet(Function<T, V> task) {
                synchronized (thing) {
                    return task.apply(thing);
                }
            }
        };
    }
    
    /**
     * Constructs a supplier of {@link Lockable} things which will be locked
     * using Java's {@code synchronized} keyword.<p>
     * 
     * The specified supplier of things is polled every time someone poll the
     * supplier that this method returns.
     * 
     * @param <T>    type of the lockable thing
     * @param thing  supplier of things we want to "lock down"
     * 
     * @return a supplier of {@code Lockable} that use {@code synchronized}
     */
    static <T> Supplier<Lockable<T>> mutex(Supplier<T> thing) {
        return () -> mutex(thing.get());
    }
    
    /**
     * Constructs a {@link Lockable} which will be locked using the specified
     * {@code lock}.<p>
     * 
     * Please not that the returned lockable does not differentiate between
     * readers and writers.
     * 
     * @param <T>    type of the lockable thing
     * @param thing  thing we want to "lock down"
     * @param lock   lock that will be used
     * 
     * @return a {@code Lockable} using the specified {@code lock}
     */
    static <T> Lockable<T> lock(T thing, Lock lock) {
        return new Lockable<T>(){
            @Override public <V> V unsafeGet(Function<T, V> task) {
                return task.apply(thing);
            }
            
            @Override public <V> V writeGet(Function<T, V> task) {
                lock.lock();
                
                try {
                    return task.apply(thing);
                }
                finally {
                    lock.unlock();
                }
            }
        };
    }
    
    /**
     * Constructs a {@link Lockable} which will be locked using the specified
     * {@code lock}.
     * 
     * @param <T>    type of the lockable thing
     * @param thing  thing we want to "lock down"
     * @param lock   lock that will be used
     * 
     * @return a {@code Lockable} using the specified {@code lock}
     */
    static <T> Lockable<T> readWrite(T thing, ReadWriteLock lock) {
        return new Lockable<T>(){
            final Lock r = lock.readLock(),
                       w = lock.writeLock();
            
            @Override public <V> V unsafeGet(Function<T, V> task) {
                return task.apply(thing);
            }
            
            @Override public <V> V readGet(Function<T, V> task) {
                return get(r, task);
            }
            
            @Override public <V> V writeGet(Function<T, V> task) {
                return get(w, task);
            }
            
            private <V> V get(Lock lock, Function<T, V> task) {
                lock.lock();
                
                try {
                    return task.apply(thing);
                }
                finally {
                    lock.unlock();
                }
            }
        };
    }
    
    /**
     * Constructs a supplier of {@link Lockable} things which will be locked
     * using the specified {@code lock}.<p>
     * 
     * The specified suppliers are polled every time someone poll the supplier
     * that this method returns.
     * 
     * @param <T>    type of the lockable thing
     * @param thing  thing we want to "lock down"
     * @param lock   lock that will be used
     * 
     * @return a supplier of {@code Lockable} that use the specified {@code lock}
     */
    static <T> Supplier<Lockable<T>> readWrite(Supplier<T> thing, Supplier<ReadWriteLock> lock) {
        return () -> readWrite(thing.get(), lock.get());
    }
}