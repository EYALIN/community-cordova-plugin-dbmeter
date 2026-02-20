[![NPM version](https://img.shields.io/npm/v/community-cordova-plugin-dbmeter)](https://www.npmjs.com/package/community-cordova-plugin-dbmeter)
[![Downloads](https://img.shields.io/npm/dm/community-cordova-plugin-dbmeter)](https://www.npmjs.com/package/community-cordova-plugin-dbmeter)


# community-cordova-plugin-dbmeter

I dedicate a considerable amount of my free time to developing and maintaining many cordova plugins for the community ([See the list with all my maintained plugins][community_plugins]).
To help ensure this plugin is kept updated,
new features are added and bugfixes are implemented quickly,
please donate a couple of dollars (or a little more if you can stretch) as this will help me to afford to dedicate time to its maintenance.
Please consider donating if you're using this plugin in an app that makes you money,
or if you're asking for new features or priority bug fixes. Thank you!

[![](https://img.shields.io/static/v1?label=Sponsor%20Me&style=for-the-badge&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86)](https://github.com/sponsors/eyalin)


---

## Credits

This plugin is based on the original work by **Alexis Kofman** ([cordova-plugin-dbmeter](https://github.com/nicoschtein/cordova-plugin-dbmeter)).
Thank you for creating the foundation of this plugin!

---

# Community Cordova Plugin DBMeter

## Overview
This Cordova plugin provides access to the device's microphone to measure sound levels in decibels (dB). It supports both Android and iOS platforms, offering real-time audio level monitoring for applications like sound meters, noise monitors, and audio-reactive apps.

## Installation
To install the plugin in your Cordova project, use the following command:
```
cordova plugin add community-cordova-plugin-dbmeter
```

## Usage
To use the plugin, you can either use continuous monitoring with callbacks or Promise-based single readings.

### Continuous Monitoring (Callback-based)
```javascript
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
    // Start continuous monitoring
    DBMeterPlugin.start(function(dB) {
        console.log('Current dB level:', dB);
    }, function(error) {
        console.error('Error:', error.message);
    });

    // Stop after 10 seconds
    setTimeout(function() {
        DBMeterPlugin.stop().then(function() {
            console.log('Stopped monitoring');
        });
    }, 10000);
}
```

### Single Reading (Promise-based)
```javascript
document.addEventListener('deviceready', onDeviceReady, false);

async function onDeviceReady() {
    try {
        const dB = await DBMeterPlugin.getDbLevel();
        console.log('Current dB level:', dB);
    } catch (error) {
        console.error('Error:', error.message);
    }
}
```

## API

### `DBMeterPlugin.start(onReading, onError)`
Start continuous monitoring of sound levels.

**Parameters:**
- `onReading` (function): Callback called with each dB reading (approximately every 100ms)
- `onError` (function, optional): Callback called if an error occurs

**Example:**
```javascript
DBMeterPlugin.start(function(dB) {
    console.log('dB:', dB);
}, function(error) {
    console.log('Error code:', error.code, 'Message:', error.message);
});
```

### `DBMeterPlugin.getDbLevel()`
Get a single dB level reading.

**Returns:** `Promise<number>` - The current dB level

**Example:**
```javascript
DBMeterPlugin.getDbLevel().then(function(dB) {
    console.log('Current level:', dB, 'dB');
}).catch(function(error) {
    console.error('Error:', error.message);
});
```

### `DBMeterPlugin.stop()`
Stop listening to the microphone.

**Returns:** `Promise<void>`

**Example:**
```javascript
DBMeterPlugin.stop().then(function() {
    console.log('DBMeter stopped');
});
```

### `DBMeterPlugin.isListening()`
Check if the DBMeter is currently monitoring.

**Returns:** `Promise<boolean>`

**Example:**
```javascript
DBMeterPlugin.isListening().then(function(listening) {
    console.log('Is listening:', listening);
});
```

### `DBMeterPlugin.delete()`
Delete the DBMeter instance and free resources.

**Returns:** `Promise<void>`

**Example:**
```javascript
DBMeterPlugin.delete().then(function() {
    console.log('DBMeter instance destroyed');
});
```

## TypeScript Support

This plugin includes TypeScript definitions. Import and use like this:

```typescript
import { IDBMeterPlugin } from 'community-cordova-plugin-dbmeter';

declare var DBMeterPlugin: IDBMeterPlugin;

// Start monitoring
DBMeterPlugin.start((dB: number) => {
    console.log('dB level:', dB);
});

// Or use async/await
async function measureSound() {
    const dB = await DBMeterPlugin.getDbLevel();
    console.log('dB level:', dB);
}
```

## Supported Platforms

- iOS
- Android

## iOS Quirks

Since iOS 10 it's mandatory to provide a usage description in the info.plist if trying to access privacy-sensitive data. When the system prompts the user to allow access, this usage description string will be displayed as part of the permission dialog box.

This plugin requires the following usage description:

- `NSMicrophoneUsageDescription` describes the reason the app accesses the user's microphone.

To add this entry into the info.plist, you can use the `edit-config` tag in the platform section of your `config.xml`:

```xml
<edit-config target="NSMicrophoneUsageDescription" file="*-Info.plist" mode="merge">
    <string>This app needs microphone access to measure sound levels</string>
</edit-config>
```

## Error Codes

| Code | Name | Description |
|------|------|-------------|
| 0 | DBMETER_NOT_INITIALIZED | The DBMeter has not been initialized/started yet |
| 1 | DBMETER_NOT_LISTENING | The DBMeter is not currently listening |

## Contributing
Contributions to the plugin are welcome. Please ensure to follow the coding standards and submit your pull requests for review.

## License
This project is licensed under the MIT License.

---
[community_plugins]: https://github.com/nicoschtein?tab=repositories&q=community&type=&language=&sort=
