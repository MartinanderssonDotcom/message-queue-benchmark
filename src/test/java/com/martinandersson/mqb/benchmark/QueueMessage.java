package com.martinandersson.mqb.benchmark;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@State(Scope.Thread)
public class QueueMessage {
    final String msg = 'T' + Long.toString(Thread.currentThread().getId());
}