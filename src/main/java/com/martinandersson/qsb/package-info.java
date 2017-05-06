/**
 * Alternative queue service implementations:<p>
 * 
 * Put grabbed messages in another queue. Each consumer need only check the head
 * of this guy as opposed to now when all grabbed messages has to be iterated
 * through.<p>
 * 
 * Allegedly, Java 9 comes with a publish/subscribe framework:
 * <pre>
 *   http://openjdk.java.net/jeps/266
 * </pre>
 * 
 * Investigate!
 */
package com.martinandersson.qsb;
