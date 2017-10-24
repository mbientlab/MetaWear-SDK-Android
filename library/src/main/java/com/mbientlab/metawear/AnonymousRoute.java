package com.mbientlab.metawear;

/**
 * Pared down variant of the {@link Route} interface that only has one {@link Subscriber}.  This
 * interface can be used to retrieve logged data from a board that was not programmed by the current
 * device.
 * @author Eric Tsai
 */
public interface AnonymousRoute {
    /**
     * String identifying the data producer chain the subscriber is receiving data from
     * @return String identifying the data chain
     */
    String identifier();
    /**
     * Subscribe to the data produced by this chain
     * @param subscriber    Subscriber implementation to handle the received data
     */
    void subscribe(Subscriber subscriber);
    /**
     * Sets the environment values  passed into the {@link Subscriber#apply(Data, Object...) apply} function
     * @param env   Environment values to use with the subscriber
     */
    void setEnvironment(Object ... env);
}
