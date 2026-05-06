# Batch SVG Converter (IntelliJ / Android Studio Plugin)

A lightweight and efficient Android Studio plugin designed to streamline the process of importing multiple SVG files into your Android project as VectorDrawables.

## Features

- **Batch Conversion**: Select multiple SVG files at once and convert them all in a single action.
- **Project View Integration**: Simply right-click on any directory (like `res/drawable`) or a group of files to start the conversion.
- **Smart Renaming**: Prompts for filenames for each asset, defaulting to a valid Android resource name (lowercase, no spaces).
- **Fast and Secure**: Uses the official Android `Svg2Vector` library for accurate conversions.

## Installation

1. Open Android Studio.
2. Go to `Settings` (or `Preferences` on macOS) > `Plugins`.
3. Select the `Marketplace` tab.
4. Search for "Batch SVG Converter".
5. Click `Install` and restart the IDE if prompted.

*(Or install from disk using the ZIP file from the [releases](https://github.com/pankajgaur/batch-svg-converter/releases) page)*

## Usage

1. In the **Project View**, navigate to your `res/drawable` folder (or any folder where you want to save the assets).
2. **Right-click** on the folder.
3. Select **"Batch Vector Asset"**.
4. Choose the SVG files you wish to convert from your computer.
5. Review/Edit the filenames in the dialog boxes.
6. Done! Your VectorDrawables are now ready to use in your project.

## Development

To build the plugin from source:

```bash
./gradlew buildPlugin
```

The distribution ZIP will be located in `build/distributions/`.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
