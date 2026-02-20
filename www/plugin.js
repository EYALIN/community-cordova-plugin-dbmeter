var PLUGIN_NAME = 'DBMeterPlugin';

var DBMeterPlugin = {
    /**
     * Start listening to the microphone and get dB values
     * @param {function} onReading - Callback called with each dB reading
     * @param {function} onError - Callback called on error
     * @returns {void}
     */
    start: function(onReading, onError) {
        cordova.exec(onReading, onError, PLUGIN_NAME, 'start', []);
    },

    /**
     * Start listening with Promise (single reading then stop)
     * @returns {Promise<number>} Current dB value
     */
    getDbLevel: function() {
        return new Promise(function(resolve, reject) {
            cordova.exec(resolve, reject, PLUGIN_NAME, 'getDbLevel', []);
        });
    },

    /**
     * Stop listening to the microphone
     * @returns {Promise<void>}
     */
    stop: function() {
        return new Promise(function(resolve, reject) {
            cordova.exec(resolve, reject, PLUGIN_NAME, 'stop', []);
        });
    },

    /**
     * Check if the DBMeter is currently listening
     * @returns {Promise<boolean>}
     */
    isListening: function() {
        return new Promise(function(resolve, reject) {
            cordova.exec(resolve, reject, PLUGIN_NAME, 'isListening', []);
        });
    },

    /**
     * Delete/destroy the DBMeter instance and free resources
     * @returns {Promise<void>}
     */
    delete: function() {
        return new Promise(function(resolve, reject) {
            cordova.exec(resolve, reject, PLUGIN_NAME, 'destroy', []);
        });
    }
};

// Error codes for reference
DBMeterPlugin.ERROR_CODES = {
    '0': 'DBMETER_NOT_INITIALIZED',
    '1': 'DBMETER_NOT_LISTENING'
};

module.exports = DBMeterPlugin;
