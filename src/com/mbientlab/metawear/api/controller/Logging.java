package com.mbientlab.metawear.api.controller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.MetaWearController.ModuleController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.util.LoggingTrigger;

import static com.mbientlab.metawear.api.Module.LOGGING;

/**
 * Controller for the Logging module.  The logging module requires firmware v0.6.0 
 * or higher.
 * @author Eric Tsai
 */
public interface Logging extends ModuleController {
    /**
     * Enumeration of registers under the Logging module
     * @author Eric Tsai
     */
    public enum Register implements com.mbientlab.metawear.api.Register {
        /** Start and stops the logging */
        ENABLE {
            @Override public byte opcode() { return 0x1; }
        },
        /** Adds a trigger to log */
        ADD_TRIGGER {
            @Override public byte opcode() { return 0x2; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    final byte[] data) {
                if ((data[1] & 0x80) == 0x80) {
                    final byte[] triggerBytes= new byte[] {data[2], data[3], data[4], 
                            (byte) (data[5] & 0x1f), (byte) ((data[5] >> 5) & 0x7)};
                    Trigger triggerObj= LoggingTrigger.lookupTrigger(triggerBytes);
                    
                    if (triggerObj == null) {
                        triggerObj= new Trigger() {
                            @Override
                            public com.mbientlab.metawear.api.Register register() {
                                return Module.lookupModule(triggerBytes[0]).lookupRegister(triggerBytes[1]);
                            }
    
                            @Override
                            public byte index() {
                                return triggerBytes[2];
                            }
    
                            @Override
                            public byte offset() {
                                return triggerBytes[3];
                            }
    
                            @Override
                            public byte length() {
                                return triggerBytes[4];
                            }
                        };
                    }
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedTriggerObject(triggerObj);
                    }
                } else {
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedTriggerId(data[2]);
                    }
                }
            }
        },
        /** Removes a previously logged trigger */
        REMOVE_TRIGGER {
            @Override public byte opcode() { return 0x3; }
        },
        /** Internal tick counter in the MetaWear logging module */
        TIME {
            @Override public byte opcode() { return 0x4; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                final long counter= ByteBuffer.wrap(data, 2, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff;
                final Calendar now= Calendar.getInstance();
                
                ReferenceTick reference= new ReferenceTick() {
                    @Override
                    public long tickCount() {
                        return counter;
                    }

                    @Override
                    public Calendar timestamp() {
                        return now;
                    }
                };
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedReferenceTick(reference);
                }
            }
        },
        /** How many log entries are available */
        LENGTH {
            @Override public byte opcode() { return 0x5; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                int nEntries= ByteBuffer.wrap(data, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedTotalEntryCount(nEntries);
                }
            }
        },
        /** Starts a log download */
        READOUT {
            @Override public byte opcode() { return 0x6; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                int nEntries= ByteBuffer.wrap(data, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
                int notifyIncrement= ByteBuffer.wrap(data, 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedDownloadConfig(nEntries, notifyIncrement);
                }
            }
        },
        /** Receives log entries from the board */
        READOUT_NOTIFY {
            @Override public byte opcode() { return 0x7; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    final byte[] data) {
                LogEntry entry= new LogEntry() {
                    final byte[] dataBytes= Arrays.copyOfRange(data, 7, 11);
                    final long tick= ByteBuffer.wrap(data, 3, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff;
                    
                    @Override
                    public byte triggerId() {
                        return (byte) (data[2] & 0xf);
                    }

                    @Override
                    public long tick() {
                        return tick;
                    }

                    @Override
                    public byte[] data() {
                        return dataBytes;
                    }
                };
                
                for(ModuleCallbacks it: callbacks) {
                    ((Callbacks) it).receivedLogEntry(entry);
                }
                
                if (data.length == 20) {
                    LogEntry secondEntry= new LogEntry() {
                        private final byte[] dataBytes= Arrays.copyOfRange(data, 16, 20);
                        private final long tick= ByteBuffer.wrap(data, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff;
                        
                        @Override
                        public byte triggerId() {
                            return (byte) (data[11] & 0xf);
                        }

                        @Override
                        public long tick() {
                            return tick; 
                        }

                        @Override
                        public byte[] data() {
                            return dataBytes;
                        }
                    };
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedLogEntry(secondEntry);
                    }
                }
            }
        },
        /** Indicates download progress */
        READOUT_PROGRESS {
            @Override public byte opcode() { return 0x8; }
            @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                    byte[] data) {
                int nEntriesLeft= ByteBuffer.wrap(data, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff;
                
                if (nEntriesLeft == 0) {
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).downloadCompleted();
                    }
                } else {
                    for(ModuleCallbacks it: callbacks) {
                        ((Callbacks) it).receivedDownloadProgress(nEntriesLeft);
                    }
                }
            }
        };

        @Override public Module module() { return Module.LOGGING; }
        @Override public void notifyCallbacks(Collection<ModuleCallbacks> callbacks,
                byte[] data) { }

    }

    /**
     * Callbacks for the logging module
     * @author Eric Tsai
     */
    public static abstract class Callbacks implements ModuleCallbacks {
        public final Module getModule() { return LOGGING; }

        /**
         * Called when a trigger ID has been resolved into a trigger object.  If the 
         * trigger data matches one of the triggers in the LoggingTrigger enum, 
         * the corresponding enum entry will be returned.  Otherwise, a Trigger implementation 
         * will be used to wrap the attributes.
         * @param triggerObj Trigger object that corresponds to the id
         * @see com.mbientlab.metawear.api.util.LoggingTrigger
         */
        public void receivedTriggerObject(Trigger triggerObj) { }
        /**
         * Called when a trigger ID has been received from the board.  
         * @param triggerId Unique numerical ID corresponding a set of trigger attributes
         */
        public void receivedTriggerId(byte triggerId) { }
        /**
         * Called when a tick reference has been received.  The reference parameter can be used
         * by the timestamp function to convert LogEntry tick counts into a timestamp
         * @param reference A reference tick to use for converting tick counts to timestamps
         * @see Logging.LogEntry#timestamp(Logging.ReferenceTick) 
         */
        public void receivedReferenceTick(ReferenceTick reference) { }
        /**
         * Called when the total number of log entries has been received 
         * @param totalEntries Total number of log entries to download
         */
        public void receivedTotalEntryCount(int totalEntries) { }
        /**
         * Called when the download configuration has been received
         * @param nEntries Number of entries to download
         * @param notifyIncrement How often to send a notification for a progress update
         */
        public void receivedDownloadConfig(int nEntries, int notifyIncrement) { }
        /**
         * Called when a log entry has been received
         * @param entry Log entry received from the board
         */
        public void receivedLogEntry(LogEntry entry) { }
        /**
         * Called every X log entries received from the board where X corresponds to the 
         * notifyIncrement parameter in the downloadLog function 
         * @param nEntriesLeft Number of entries left to download
         */
        public void receivedDownloadProgress(int nEntriesLeft) { }
        /**
         * Called when the log download is completed
         */
        public void downloadCompleted() { }
    }

    /**
     * Wrapper class pairing a tick count to a timestamp
     * @author Eric Tsai
     */
    public interface ReferenceTick {
        /**
         * Get the tick count used as a reference
         */
        public long tickCount();
        /**
         * Get the timestamp the tick corresponds to
         */
        public Calendar timestamp();
    }
    /**
     * Wrapper class encapsulating trigger attributes
     * @author Eric Tsai
     */
    public interface Trigger {
        /**
         * Metawear register the trigger listens for
         * @return Register that is being logged
         */
        public com.mbientlab.metawear.api.Register register();
        /**
         * Controls which module index to log i.e. a specific GPIO pin or NeoPixel strand ID.  
         * @return Module index being logged, or 0xff is unused
         */
        public byte index();
        /**
         * Position in the register byte array to start logging from
         * @return Data offset
         */
        public byte offset();
        /**
         * Number of bytes required by the trigger data.  Can be between [1, 4] bytes
         * @return Data length
         */
        public byte length();
    }
    
    /**
     * Wrapper class encapsulating the bytes of a log entry
     * @author Eric Tsai
     */
    public abstract class LogEntry {
        /**
         * ID of the trigger the entry is for
         */
        public abstract byte triggerId();
        /**
         * Time tick of when the event was recorded
         */
        public abstract long tick();
        /**
         * Get the timestamp of when the event was recorded.  A ReferenceTick object is provided 
         * by the {@link Callbacks#receivedReferenceTick(Logging.ReferenceTick)} callback function
         * @param reference Reference point used to convert the tick count into a timestamp
         * @see Logging#readReferenceTick()
         */
        public Calendar timestamp(ReferenceTick reference) {
            final double TICK_TIME_STEP= (48 / 32768.0) * 1000;
            Calendar copy= (Calendar) reference.timestamp().clone();
            
            copy.add(Calendar.MILLISECOND, (int) ((reference.tickCount() - tick()) * TICK_TIME_STEP)); 
            return copy;
        }
        /**
         * Recorded data from the register
         */
        public abstract byte[] data();
    }

    /**
     * Start logging trigger events
     */
    public void startLogging();
    /**
     * Stop logging trigger events
     */
    public void stopLogging();

    /**
     * Add a trigger to the MetaWear logging module.  When the MetaWear board has processed the trigger, 
     * a unique id representing the trigger passed back to the user via the receivedTriggerId function. 
     * @param triggerObj Trigger to log
     * @see Callbacks#receivedTriggerId(byte)
     */
    public void addTrigger(Trigger triggerObj);
    /**
     * Converts a trigger id to its corresponding trigger attributes.  When the attribute is 
     * received, the receivedTriggerObject callback function will be called
     * @param triggerId Trigger id to lookup
     * @see Callbacks#receivedTriggerObject(Logging.Trigger)
     */
    public void triggerIdToObject(byte triggerId);
    /**
     * Removes the trigger from the logging module
     * @param triggerId Unique id of the trigger to remove
     */
    public void removeTrigger(byte triggerId);

    /**
     * Retrieve a tick reference from the MetaWear board.  When the data is received, the readReferenceTick 
     * callback function will be called
     * @see Callbacks#receivedReferenceTick(Logging.ReferenceTick)
     */
    public void readReferenceTick();
    /**
     * Retrieve the total number of log entries available.  When the data is received, 
     * the receivedTotalEntryCount callback function will be called
     * @see Callbacks#receivedTotalEntryCount(int)
     */
    public void readTotalEntryCount();
    /**
     * Download the entries from the logging module.  When each entry is received, the 
     * receivedLogEntry callback function will be called.  For every X entries received, 
     * where X is set by the notifyIncrement parameter, the receivedDownloadProgress 
     * callback function will be called.
     * @param nEntries Number of entries to download
     * @param notifyIncrement How often to send a progress update
     * @see Callbacks#receivedLogEntry(Logging.LogEntry)
     * @see Callbacks#receivedDownloadProgress(int)
     */
    public void downloadLog(int nEntries, int notifyIncrement);

}
