# NFC Emulator

A comprehensive Android application for reading, storing, and emulating NFC cards. This app allows you to capture NFC card data and then emulate those cards using Android's Host Card Emulation (HCE) service.

## Features

### 🔍 NFC Card Reading
- **Multi-technology Support**: Reads various NFC card types including:
  - Mifare Classic
  - Mifare Ultralight
  - NDEF (NFC Data Exchange Format)
  - ISO-DEP (ISO 14443-4)
  - NFC-A, NFC-B, NFC-F, NFC-V
- **Automatic Detection**: Automatically detects and reads NFC cards when placed near the device
- **Comprehensive Data Capture**: Stores complete card data including UID, technology information, and sector/page data

### 💾 Card Management
- **Save Cards**: Store read NFC cards with custom names for easy identification
- **Card Library**: View all saved cards with detailed information
- **Data Persistence**: Cards are saved locally using JSON format
- **Duplicate Prevention**: Automatically prevents saving duplicate cards

### 🎭 NFC Emulation
- **Host Card Emulation (HCE)**: Emulate saved NFC cards using Android's HCE service
- **Multiple AID Support**: Supports various Application Identifiers for broad compatibility
- **Real-time Emulation**: Start/stop emulation with a single tap
- **APDU Command Handling**: Responds to standard APDU commands including:
  - SELECT commands
  - READ BINARY commands
  - GET UID commands
  - AUTHENTICATE commands
  - UPDATE BINARY commands

### 📊 Logging & Debugging
- **Comprehensive Logging**: Detailed logs for all NFC operations
- **File-based Logging**: Logs are saved to external storage for analysis
- **Debug Tools**: Built-in debugging tools to inspect card data and service status
- **Log Management**: View, clear, and export logs

### 🎨 Modern UI
- **Material Design 3**: Beautiful, modern interface following Material Design guidelines
- **Jetpack Compose**: Built with the latest Android UI toolkit
- **Responsive Design**: Optimized for various screen sizes
- **Real-time Status**: Live updates of NFC status and emulation state

## Technical Details

### Architecture
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with StateFlow
- **Storage**: JSON-based local storage
- **NFC Service**: Android Host Card Emulation (HCE)

### Supported Android Versions
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36

### Dependencies
- **AndroidX Core KTX**: 1.17.0
- **Jetpack Compose BOM**: 2025.09.01
- **Lifecycle**: 2.9.4
- **Navigation Compose**: 2.9.5
- **Gson**: 2.13.2 (for JSON serialization)

## Installation

### Prerequisites
- Android device with NFC capability
- Android 7.0 (API level 24) or higher
- NFC enabled on the device

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/NFCEmulator.git
   cd NFCEmulator
   ```

2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Build and run the app on your NFC-enabled Android device

### APK Installation
1. Download the latest APK from the releases section
2. Enable "Install from unknown sources" in your device settings
3. Install the APK file

## Usage

### Reading NFC Cards
1. **Enable NFC**: Ensure NFC is enabled in your device settings
2. **Launch App**: Open the NFC Emulator app
3. **Read Card**: Place an NFC card near your device
4. **Save Card**: Tap "Save Card" to store the card data with a custom name

### Emulating NFC Cards
1. **Select Card**: Choose a saved card from the list
2. **Start Emulation**: Tap "Start Emulation" to begin emulating the card
3. **Use Card**: The device will now respond as the emulated NFC card
4. **Stop Emulation**: Tap "Stop Emulation" to end the emulation

### Managing Cards
- **View Details**: Tap on any saved card to see detailed information
- **Delete Cards**: Use the delete button to remove unwanted cards
- **Refresh List**: Use the refresh button to update the card list

## Permissions

The app requires the following permissions:
- `android.permission.NFC` - Access to NFC functionality
- `android.permission.BIND_NFC_SERVICE` - Host Card Emulation service
- `android.permission.FOREGROUND_SERVICE` - Run emulation service in foreground
- `android.permission.WRITE_EXTERNAL_STORAGE` - Save logs and card data (Android 12 and below)
- `android.permission.READ_EXTERNAL_STORAGE` - Read saved data (Android 12 and below)

## Supported NFC Technologies

### Mifare Classic
- Reads sector data from Mifare Classic cards
- Supports authentication commands
- Emulates sector-based data structure

### Mifare Ultralight
- Reads page data from Mifare Ultralight cards
- Supports page-based read operations
- Emulates page-based data structure

### NDEF (NFC Data Exchange Format)
- Reads NDEF messages from compatible cards
- Supports text, URI, and other NDEF record types
- Emulates NDEF message responses

### ISO-DEP (ISO 14443-4)
- Supports ISO-DEP communication protocol
- Handles SELECT and other standard APDU commands
- Emulates ISO-DEP card responses

### Other Technologies
- NFC-A, NFC-B, NFC-F, NFC-V support
- Basic UID reading and emulation
- Technology-specific data handling

## Troubleshooting

### Common Issues

**NFC Not Working**
- Ensure NFC is enabled in device settings
- Check if the device supports NFC
- Verify the app has NFC permissions

**Card Reading Fails**
- Make sure the card is properly positioned
- Try different card types
- Check if the card is damaged or corrupted

**Emulation Not Working**
- Ensure the emulation service is running
- Check if the target device supports the card type
- Verify the AID configuration

**App Crashes**
- Check the logs using the built-in log viewer
- Clear app data and try again
- Ensure sufficient storage space

### Debug Information
The app includes comprehensive debugging tools:
- **Status Check**: View current emulation status
- **Log Viewer**: Read detailed operation logs
- **File Debug**: Inspect saved card data
- **Service Info**: Check NFC service status

## Development

### Project Structure
```
app/
├── src/main/java/com/sinii/nfcemulator/
│   ├── MainActivity.kt              # Main UI and navigation
│   ├── NFCEmulationService.kt       # HCE service implementation
│   ├── NFCManager.kt                # NFC operations and data management
│   ├── NFCReaderActivity.kt         # NFC card reading interface
│   ├── NFCData.kt                   # Data models and utilities
│   ├── LogManager.kt                # Logging functionality
│   └── ui/theme/                    # UI theme and styling
├── src/main/res/
│   ├── xml/
│   │   ├── apdu_service.xml         # HCE service configuration
│   │   └── nfc_tech_filter.xml      # NFC technology filters
│   └── values/strings.xml           # String resources
└── build.gradle.kts                 # Build configuration
```

### Key Components

**NFCEmulationService**
- Implements Android's HostApduService
- Handles APDU command processing
- Manages emulation state and responses

**NFCManager**
- Manages NFC adapter and operations
- Handles card reading and data storage
- Provides card management functionality

**MainActivity**
- Main UI using Jetpack Compose
- Manages app state and navigation
- Handles user interactions

### Adding New Card Types
1. Add technology support in `NFCManager.kt`
2. Update `NFCData.kt` if new data structures are needed
3. Add emulation logic in `NFCEmulationService.kt`
4. Update technology filters in `nfc_tech_filter.xml`

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Disclaimer

This application is for educational and testing purposes only. Users are responsible for complying with all applicable laws and regulations regarding NFC card emulation. The developers are not responsible for any misuse of this application.

## Support

If you encounter any issues or have questions:
1. Check the troubleshooting section above
2. Review the app logs using the built-in log viewer
3. Open an issue on GitHub with detailed information
4. Include device information, Android version, and steps to reproduce

## Changelog

### Version 1.0
- Initial release
- NFC card reading and emulation
- Card management and storage
- Comprehensive logging system
- Material Design 3 UI
- Support for multiple NFC technologies

---

**Note**: This app requires an NFC-enabled Android device to function properly. NFC emulation capabilities may vary depending on the device and Android version.