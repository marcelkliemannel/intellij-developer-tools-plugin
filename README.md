# IntelliJ Developer Tools Plugin

<img src="src/main/resources/META-INF/pluginIcon.svg" alt="Plugin Logo" width="120px"/>

This plugin is a powerful and versatile set of tools designed to enhance the development experience for software engineers. With its extensive collection of features, developers can increase their productivity and simplify complex operations without leaving their coding environment.

<img src="screenshots/main-dialog.png" alt="Main Dialog" width="932px"/>

Plugin icon by [Gabriele Malaspina](https://www.svgrepo.com/svg/489187/toolbox).

## Key Features

- Encoding and Decoding:
  - JWT (JSON Web Tokens)
  - Base32
  - Base64
  - URL Base64
  - MIME Base64
  - URL Encoding
  - Line breaks
- Regular Expression Matcher
- UUID Generator
- Text Sorting
- Text Case Transformation
- Text Diff Viewer
- Text Escaping:  HTML entities, Java Strings, JSON, CSV, and XML
- JSON Path Parser
- JSON Schema Validator
- Hashing
- Code Style Formatting
- SQL Formatting
- Password Generator
- QR Code/Barcode Generator
- Lorem Ipsum Generator

## Integration

The main dialog with all tools is available through:
 - IntelliJ's main menu under **Tools | Developer Tools**;
 - in the toolbar of the old UI if enabled via **View | Toolbar**;
 - and in the new UI in the right main toolbar.


## Development

This plugin is not seen as a library. Therefore, code changes do not necessarily adhere to the semantics version rules.

If you want to contribute something, please follow the code style in the `.editorconfig` and sign your commits.

## License

Copyright (c) 2023 Marcel Kliemannel

Licensed under the **Apache License, Version 2.0** (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at <https://www.apache.org/licenses/LICENSE-2.0>.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the [LICENSE](./LICENSE) for the specific language governing permissions and limitations under the License.
