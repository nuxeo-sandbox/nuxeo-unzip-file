/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package nuxeo.unzip.file;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.types.TypeManager;

/**
 * Unzips a file that contains a tree structure of files and creates documents
 * accordingly ("Folder" and File/Picture/Video/...)
 *
 * It is called by the FileManager service, when, for example, a user drops a
 * .zip file.
 *
 * There is an xml contribution to the Filemanager "plugins" extension. This
 * contribution makes sure that this UnzipFileToDocumentsImporter class is used
 * <i>after</i> other specialized importers (CSV importer, ArchivedTree
 * importer, ...)
 *
 * @since 8.3
 */
public class UnzipFileToDocumentsImporter extends AbstractFileImporter {

    private static final long serialVersionUID = -1018545014286064763L;

    protected static Log log = LogFactory.getLog(UnzipFileToDocumentsImporter.class);

    /**
     * Checks if the file may be a zip archive
     *
     * @param file
     * @return true is the file is not null and looks valid (just checking
     *         headers)
     * @since 8.3
     */
    public static boolean looksLikeValidZip(File file) {

        if(file == null) {
            return false;
        }

        ZipFile zip = null;
        boolean looksValid = false;
        try {
            zip = new ZipFile(file);
            looksValid = true;
        } catch (ZipException e) {
            // Ignore
        } catch (IOException e) {
            // Ignore
        } finally {
            IOUtils.closeQuietly(zip);
        }

        return looksValid;

    }

    @Override
    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String filename,
            TypeManager typeService) throws IOException {

        DocumentModel mainFolderDoc = null;

        try (CloseableFile source = content.getCloseableFile()) {
            if (looksLikeValidZip(source.getFile())) {
                DocumentModel parent = documentManager.getDocument(new PathRef(
                        path));
                mainFolderDoc = UnzipToDocuments.run(parent, content, null, 0);
            }
        }

        return mainFolderDoc;
    }

}
