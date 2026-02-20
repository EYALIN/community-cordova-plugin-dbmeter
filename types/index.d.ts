/**
 * Callback type for continuous dB readings
 */
export type DbReadingCallback = (db: number) => void;

/**
 * Callback type for errors
 */
export type ErrorCallback = (error: IDBMeterError) => void;

/**
 * Error object returned by the plugin
 */
export interface IDBMeterError {
    /** Error code (0 = not initialized, 1 = not listening) */
    code: number;
    /** Human-readable error message */
    message: string;
}

/**
 * Error codes enumeration
 */
export enum DBMeterErrorCode {
    DBMETER_NOT_INITIALIZED = 0,
    DBMETER_NOT_LISTENING = 1
}

/**
 * Noise classification based on dB level
 */
export interface INoiseClassification {
    /** Noise level category */
    level: 'very_quiet' | 'quiet' | 'moderate' | 'loud' | 'very_loud' | 'harmful' | 'dangerous';
    /** Translation key for label */
    label: string;
    /** Translation key for description */
    description: string;
    /** Color code for UI display */
    color: string;
    /** Risk level for hearing */
    riskLevel: 'safe' | 'low' | 'moderate' | 'high';
}

/**
 * Session statistics for sound measurements
 */
export interface ISoundSessionStats {
    /** Current dB reading */
    currentDb: number;
    /** Peak (maximum) dB recorded */
    peakDb: number;
    /** Timestamp when peak was recorded */
    peakTimestamp: number;
    /** Minimum dB recorded */
    minDb: number;
    /** Timestamp when minimum was recorded */
    minTimestamp: number;
    /** Average dB over the session */
    avgDb: number;
    /** Total number of readings */
    readingCount: number;
    /** Session start time (ms since epoch) */
    sessionStartTime: number;
    /** Session duration in milliseconds */
    sessionDuration: number;
    /** Current noise classification */
    classification: INoiseClassification;
}

/**
 * DBMeterPlugin interface - main plugin API
 */
export interface IDBMeterPlugin {
    /**
     * Start listening to the microphone and get continuous dB values
     * @param onReading - Callback called with each dB reading (typically every 100ms)
     * @param onError - Callback called on error
     */
    start(onReading: DbReadingCallback, onError?: ErrorCallback): void;

    /**
     * Get a single dB level reading (Promise-based)
     * @returns Promise resolving to the current dB value
     */
    getDbLevel(): Promise<number>;

    /**
     * Stop listening to the microphone
     * @returns Promise resolving when stopped
     */
    stop(): Promise<void>;

    /**
     * Check if the DBMeter is currently listening
     * @returns Promise resolving to boolean indicating listening state
     */
    isListening(): Promise<boolean>;

    /**
     * Delete/destroy the DBMeter instance and free resources
     * @returns Promise resolving when destroyed
     */
    delete(): Promise<void>;

    /**
     * Error codes reference
     */
    ERROR_CODES: {
        '0': 'DBMETER_NOT_INITIALIZED';
        '1': 'DBMETER_NOT_LISTENING';
    };
}

/**
 * DBMeterManager class for TypeScript usage
 */
declare class DBMeterManager implements IDBMeterPlugin {
    start(onReading: DbReadingCallback, onError?: ErrorCallback): void;
    getDbLevel(): Promise<number>;
    stop(): Promise<void>;
    isListening(): Promise<boolean>;
    delete(): Promise<void>;
    ERROR_CODES: {
        '0': 'DBMETER_NOT_INITIALIZED';
        '1': 'DBMETER_NOT_LISTENING';
    };
}

export default DBMeterManager;

/**
 * Global declaration for window.DBMeterPlugin
 */
declare global {
    interface Window {
        DBMeterPlugin: IDBMeterPlugin;
    }
    var DBMeterPlugin: IDBMeterPlugin;
}
