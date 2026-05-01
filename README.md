# IntelliJ Developer Tools Plugin

<img src="src/main/resources/META-INF/pluginIcon.svg" alt="Plugin Logo" width="120px"/>

Developer Tools brings a broad collection of everyday development utilities directly into IntelliJ-based IDEs. Encode and decode data, transform text, validate JSON, generate identifiers, inspect archives, format code and SQL, and run other common tasks without leaving the IDE.

Main toolbar window:

<img src="screenshots/tool-window.png" alt="Main Tool Window" width="693px"/>

Editor menu:

<img src="screenshots/editor-menu.png" alt="Editor Menu" width="708px"/>

Plugin icon by [Gabriele Malaspina](https://www.svgrepo.com/svg/489187/toolbox).

## Key Features

- Encoding and decoding: JWT (JSON Web Tokens), Base32, Base64, URL Base64, MIME Base64, URL encoding, ASCII, and line breaks
- Regular Expression Matcher
- UUID, ULID, Nano ID, and password generators
- Text Sorting
- Text Case Transformation
- Text Diff Viewer
- Text Format Conversion
- Text escaping and unescaping: HTML entities, Java strings, JSON, CSV, XML, and escape sequences
- Text Filter
- JSON Path Parser
- JSON Schema Validator
- Hashing and HMAC
- HTTP Server (WireMock)
- Archive viewer and extractor for ZIP, TAR, JAR, 7z, and other formats
- Date and time tools for Unix timestamps, formatting, and parsing
- Unit converters for time, data size, and transfer rate
- Code Style Formatting
- SQL Formatting
- Color Picker
- Fetching, analyzing, and exporting server certificates
- QR Code/Barcode Generator
- Lorem Ipsum Generator
- ASCII Art

## Integration

The main tools are available in a standalone dialog and in a tool window. Some tools are also available from the editor menu or as code intentions. Editor actions may require selected text or a caret placed on a Java/Kotlin string or identifier.

Plugin settings are available in IntelliJ IDEA's settings/preferences under **Tools | Developer Tools**.

### Tool Window

The tool window is available through **View | Tool Windows | Developer Tools**. Inputs and tool configuration are stored per project.

### Dialog

The dialog is available from IntelliJ IDEA's main menu under **Tools | Developer Tools**.

To add the "Open Dialog" action to the main toolbar, enable it in IntelliJ IDEA's settings/preferences under **Tools | Developer Tools**, or add it manually via **Customize Toolbar... | Add Actions... | Developer Tools**.

Dialog inputs and tool configuration are stored at the application level.

## Development

This plugin is not treated as a library, so code changes do not necessarily follow semantic versioning rules.

If you want to contribute, please follow the code style defined in `.editorconfig` and sign your commits.
