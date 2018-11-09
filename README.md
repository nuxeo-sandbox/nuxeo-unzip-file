# nuxeo-unzip-file

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-unzip-file-master)](https://qa.nuxeo.org/jenkins/job/Sandbox/job/sandbox_nuxeo-unzip-file-master/)

This add-on for [Nuxeo platform](http://www.nuxeo.com) extracts a .zip file and creates the corresponding Documents, building the same tree structure.

The `FileManager` service is used to create the Documents based on the type of the files (so for example, it creates a `File` for a .pdf, a `Picture` for a .jpg, ...).

<div style="margin-left: 50px; font-style: italic;">
Notice that Nuxeo being an extensible platform, you may have overriden the default behavior, so for example you create a custom `MyCustomDesign` Document for an image file.
</div>

## Usage
The plug-in provides:

* Automatic extraction when a .zip file is uploaded to Nuxeo (whatever the way: End user in the UI, REST API, server-side importation, ...). There is an XML contribution to the `FileManager`.
* And an operation, `Document.UnzipFileToDocuments` that lets you trigger the extraction.

#### Automatic Extraction

The plugin contains an XML contribution to the `FileManager` that makes it possible to automatically extract the files of the zip file and create the documents and their structure.

The contribution makes sure the extraction will be run _after_ some specific other contributions that handle .zip file: The `CSVImporter` (the .zip file contains a .csv file) and the `ExportedZipImporter` (a Nuxeo tree structure was exported and is now imported).

If you want to store your .zip files as regular `File` documents, and later maybe use the `Document.UnzipFileToDocuments` operation to extract the files, you can deactivate the contribution. In Nuxeo Studio > Advanced Settings > XML Extensions, create a new contribution ("DisableZipAutoExtract" for example) and paste the following:

```
<extension
    target="org.nuxeo.ecm.platform.filemanager.service.FileManagerService"
    point="plugins">
  <plugin name="UnzipToDocuments" enabled="false"></plugin>
</extension>
``` 

#### The `Document.UnzipFileToDocuments` operation:

* Expects a `Document` or a `Blob` as input and creates the same structure in the target document.
* When the input is a `Document`:
  * Parameter `target` is optional. If not used, the parent of the input Document is used
  * The zip blob is read in the `xpath` parameter (default value: `file:content`) of the input Document
* When the input is a `Blob`:
  * The `target` parameter is required
  * `xpath` is ignored
* Other parameters:
  * `folderishType`: The document type to used when creating a folderish (default: `Folder`)
  * `commitModulo` (default value: 100): When the zip archive contains thousands of documents, the transaction will be commited/started every  `commitModulo` created. This will avoid long running transaction that any database hates, most often generating a "transaction timeout" error cacelling the whole process.



## Build

Assuming [Apache Maven](https://maven.apache.org) version 3.2.3 minimum is installed on your computer:

```
cd /PATH/TO/YOUR/MAIN/FOLDER
git clone https://github.com/nuxeo-sandbox/nuxeo-unzip-file.git
cd nuxeo-unzip
mvn clean install
```

The Nuxeo Package to install on your server is in target/

## Support

**These features are sand-boxed and not yet part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).
