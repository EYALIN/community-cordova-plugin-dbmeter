import Foundation
import AVFoundation

/**
 * DBMeterPlugin - Cordova plugin to get decibel (dB) values from the microphone.
 *
 * This plugin provides the decibel level from the microphone for sound meter applications.
 */
@objc(DBMeterPlugin) class DBMeterPlugin: CDVPlugin {

    private let LOG_TAG = "DBMeterPlugin"
    private let SAMPLE_INTERVAL_MS = 100

    private var isListening: Bool = false
    private var audioRecorder: AVAudioRecorder!
    private var command: CDVInvokedUrlCommand!
    private var timer: DispatchSourceTimer!
    private var isTimerExists: Bool = false

    /**
     * Initialize with command delegate
     */
    init(commandDelegate: CDVCommandDelegate) {
        super.init()
        self.commandDelegate = commandDelegate
    }

    override init() {
        super.init()
    }

    /**
     * Permits to free the memory from the audioRecord instance
     */
    @objc(destroy:)
    func destroy(command: CDVInvokedUrlCommand) {
        if (self.isListening) {
            self.timer.suspend()
            self.isListening = false
        }

        self.command = nil

        if (self.audioRecorder != nil) {
            self.audioRecorder.stop()
            self.audioRecorder = nil

            let pluginResult = CDVPluginResult(status: .ok)
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
        } else {
            self.sendPluginError(callbackId: command.callbackId, errorCode: PluginError.DBMETER_NOT_INITIALIZED, errorMessage: "DBMeter is not initialized")
        }
    }

    /**
     * Initialize the audio recorder if not already initialized
     */
    private func initAudioRecorder() throws {
        if (self.audioRecorder == nil) {
            let url: URL = URL.init(fileURLWithPath: "/dev/null")

            let settings: [String: Any] = [
                AVFormatIDKey: Int(kAudioFormatAppleLossless),
                AVSampleRateKey: 44100.0,
                AVNumberOfChannelsKey: 1 as NSNumber,
                AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
            ]

            let audioSession: AVAudioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(AVAudioSession.Category.playAndRecord)
            try audioSession.setActive(true)

            self.audioRecorder = try AVAudioRecorder(url: url, settings: settings)
            self.audioRecorder.isMeteringEnabled = true
        }
    }

    /**
     * Calculate the dB level from the audio recorder
     */
    private func calculateDbLevel() -> Int32 {
        self.audioRecorder.updateMeters()
        let peakPowerForChannel = pow(10, (self.audioRecorder.averagePower(forChannel: 0) / 20))
        let db = Int32(round(20 * log10(peakPowerForChannel) + 90))
        return db
    }

    /**
     * Starts listening the audio signal and sends dB values as a
     * CDVPluginResult keeping the same callback alive.
     */
    @objc(start:)
    func start(command: CDVInvokedUrlCommand) {
        self.commandDelegate!.run(inBackground: {
            self.command = command

            if (!self.isTimerExists) {
                self.timer = DispatchSource.makeTimerSource(queue: DispatchQueue.main)
                self.timer.schedule(deadline: .now(), repeating: DispatchTimeInterval.milliseconds(self.SAMPLE_INTERVAL_MS))
                self.timer.setEventHandler(handler: self.timerCallBack)
                self.isTimerExists = true
            }

            do {
                try self.initAudioRecorder()
            } catch {
                self.sendPluginError(callbackId: command.callbackId, errorCode: PluginError.DBMETER_NOT_INITIALIZED, errorMessage: "Error while initializing DBMeter")
                return
            }

            if (!self.isListening) {
                self.isListening = true
                self.audioRecorder.record()
                self.timer.resume()
            }
        })
    }

    /**
     * Get a single dB level reading (Promise-based)
     */
    @objc(getDbLevel:)
    func getDbLevel(command: CDVInvokedUrlCommand) {
        self.commandDelegate!.run(inBackground: {
            do {
                try self.initAudioRecorder()
            } catch {
                self.sendPluginError(callbackId: command.callbackId, errorCode: PluginError.DBMETER_NOT_INITIALIZED, errorMessage: "Error while initializing DBMeter")
                return
            }

            let wasListening = self.isListening
            if (!self.isListening) {
                self.audioRecorder.record()
            }

            // Give it a moment to get a reading
            Thread.sleep(forTimeInterval: 0.1)

            let db = self.calculateDbLevel()

            // Stop if it wasn't listening before
            if (!wasListening) {
                self.audioRecorder.stop()
            }

            let pluginResult = CDVPluginResult(status: .ok, messageAs: db)
            self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
        })
    }

    /**
     * Stops listening the audio signal.
     * Even if stopped, the AVAudioRecorder instance still exists.
     * To destroy this instance, please use the destroy method.
     */
    @objc(stop:)
    public func stop(command: CDVInvokedUrlCommand) {
        self.commandDelegate!.run(inBackground: {
            if (self.isListening) {
                self.isListening = false

                if (self.audioRecorder != nil && self.audioRecorder.isRecording) {
                    self.timer.suspend()
                    self.audioRecorder.stop()
                }

                let pluginResult = CDVPluginResult(status: .ok)
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            } else {
                self.sendPluginError(callbackId: command.callbackId, errorCode: PluginError.DBMETER_NOT_LISTENING, errorMessage: "DBMeter is not listening")
            }
        })
    }

    /**
     * Returns whether the DBMeter is listening.
     */
    @objc(isListening:)
    func isListening(command: CDVInvokedUrlCommand?) -> Bool {
        if (command != nil) {
            let pluginResult = CDVPluginResult(status: .ok, messageAs: self.isListening)
            self.commandDelegate!.send(pluginResult, callbackId: command!.callbackId)
        }
        return self.isListening
    }

    /**
     * Timer callback for continuous monitoring
     */
    private func timerCallBack() {
        autoreleasepool {
            if (self.isListening && self.audioRecorder != nil) {
                let db = self.calculateDbLevel()

                let pluginResult = CDVPluginResult(status: .ok, messageAs: db)
                pluginResult.setKeepCallbackAs(true)
                self.commandDelegate!.send(pluginResult, callbackId: self.command.callbackId)
            }
        }
    }

    /**
     * Convenient method to send plugin errors.
     */
    private func sendPluginError(callbackId: String, errorCode: PluginError, errorMessage: String) {
        let pluginResult = CDVPluginResult(status: .error, messageAs: ["code": errorCode.hashValue, "message": errorMessage])
        self.commandDelegate!.send(pluginResult, callbackId: callbackId)
    }

    enum PluginError: String {
        case DBMETER_NOT_INITIALIZED = "0"
        case DBMETER_NOT_LISTENING = "1"
    }
}
