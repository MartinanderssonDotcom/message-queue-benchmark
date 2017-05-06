package com.martinandersson.qsb.impl;

/**
 * Enumerated responses of an attempt to grab a message for external consumption.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public enum GrabResponse
{
    /**
     * Request to grab the message succeeded.<p>
     * 
     * 
     * <h3>Possible reasons</h3>
     * 
     * Message has not been grabbed before or was grabbed but reached its
     * expiration.
     */
    SUCCEEDED,
    
    /**
     * Request to grab the message is declined.<p>
     * 
     * 
     * <h3>Possible reasons</h3>
     * 
     * Message marked as completed.
     */
    COMPLETED,
    
    /**
     * Request to grab the message is declined.<p>
     * 
     * 
     * <h3>Possible reasons</h3>
     * 
     * Message already grabbed by someone else and has yet to expire.
     */
    ACTIVE
}