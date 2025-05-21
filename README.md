# Jahia Augmented Search CLI Tools

This repository contains a Java-based project built with Maven, providing command-line tools for managing documents and search index operations within a Jahia-based content management system.

## Features

- **Remove documents from index**: Remove documents from both internal (JCR) and external content stores, supporting synchronous, asynchronous, and test modes for bulk event generation.
- **Start indexation**: Trigger full re-indexing of the platform, with options to force indexation.
- **Apache Karaf integration**: Commands are available as Karaf shell commands for easy CLI access.
- **Jahia integration**: Interacts with Jahia's JCR, external content APIs, and Augmented Search modules.

## Commands

- `as:remove <path> [--test] [--async]`  
  Remove a document from the index at the specified path.
    - `--test`: Generate 1000 events for testing.
    - `--async`: Run removal in asynchronous mode.

- `as:index [--force]`  
  Start full indexation of the platform.
    - `--force`: Force re-indexation.

## Build

Requires Java and Maven.

```sh
mvn clean install
# to deploy on a Docker Jahia instance
mvn clean install jahia:deploy -Djahia.deploy.targetContainerName="jahia"
```

## Usage
Deploy the built bundle to your Jahia instance with Karaf. Use the provided commands in the Karaf shell.


## Development
* Java
* Maven
* Apache Karaf
* Jahia CMS

## License
See [LICENSE](LICENSE) file for details.
