# nuxeo-unzip-file
====

This add-on for Nuxeo platform extracts a .zip file and create the corresponding documents, building the same tree structure.

## Usage
The plug-in provides an operation, `Document.UnzipFile` that:

* Expects a `Document` as input
* Extracts its blob stored in the `xpath` parameter (default value: `file:content`)
* And creates the same structure in the target (the current folder if the `target` parameter isn't provided. `target` can be a Document UUID or it's path.



## Build

Assuming [Apache Maven](https://maven.apache.org) version 3.2.3 minimum is installed on your computer:

```
cd /PATH/TO/YOUR/MAIN/FOLDER
git clone https://github.com/nuxeo-sandbox/nuxeo-unzip-file.git
cd nuxeo-unzip
mvn clean install
```

#### WARNING

* As of "today" (Sept. 2016), unit tests are OK when ran from Eclipse, not from Maven. You should build with the `-DskipTests=true` switch.
* The Nuxeo Packaged generated contains way too much libraries, this also has to be sorted out. In the meantime, you can just build the core and drop the jar in the "bundles" directory of your Nuxeo server.


## Support

**These features are sand-boxed and not yet part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).
