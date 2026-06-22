# Jahia Augmented Search CLI Tools

This repository contains a Java-based project built with Maven, providing command-line tools for managing documents and search index operations within a Jahia-based content management system.

## Features

- **Add documents to index**: Index documents from both internal (JCR) and external content stores, supporting synchronous, asynchronous, and test modes for bulk event generation.
- **Remove documents from index**: Remove documents from both internal (JCR) and external content stores, supporting synchronous, asynchronous, and test modes for bulk event generation.
- **Start indexation**: Trigger full re-indexing of the platform, with options to force indexation.
- **Apache Karaf integration**: Commands are available as Karaf shell commands for easy CLI access.
- **Jahia integration**: Interacts with Jahia's JCR, external content APIs, and Augmented Search modules.

## Commands

- `as:add <path> [--test] [--async]`  
  Add a document to the index at the specified path.
    - `path`: **Required.** The JCR or external-provider path of the document to index.
    - `--test`: Generate 1000 events for load testing.
    - `--async`: Run indexing in asynchronous mode.

- `as:remove <path> [--test] [--async]`  
  Remove a document from the index at the specified path.
    - `path`: **Required.** The JCR or external-provider path of the document to remove.
    - `--test`: Generate 1000 events for load testing.
    - `--async`: Run removal in asynchronous mode.

- `as:index [--force]`  
  Start full indexation of the platform. Fails if no Augmented Search connection is configured.
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
