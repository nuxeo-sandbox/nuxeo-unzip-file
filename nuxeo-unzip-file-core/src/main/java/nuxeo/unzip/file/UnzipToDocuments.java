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
 *     Michael Gena
 *     Thibaud Arguillere
 */
package nuxeo.unzip.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Creates Documents, in a hierarchical way, copying the tree-structure stored
 * in the zip file
 *
 * TODO: This code has room for optimization, use of try-with-resource etc.
 *
 * @since 8.3
 */
public class UnzipToDocuments {

    protected static Log logger = LogFactory.getLog(UnzipToDocuments.class);

    public static String DEFAULT_FOLDERISH_TYPE = "Folder";

    public static int DEFAULT_COMMIT_MODULO = 100;

    /**
     * Unzip the file and creates the Documents
     *
     * @param parentDocument
     * @param zipBlob
     * @return the main document (Folder) containing the unzipped data
     * @since 8.3
     */
    public static DocumentModel run(DocumentModel parentDocument, Blob zipBlob, String folderishType, int commitModulo)
            throws NuxeoException {

        String tmpDir = Environment.getDefault().getTemp().getPath();
        Path tmpDirPath = tmpDir != null ? Paths.get(tmpDir) : null;
        Path outDirPath = null;
        String dcTitle;
        File mainParentFolderOnDisk = null;
        DocumentModel mainUnzippedFolderDoc = null;
        boolean isMainUzippedFolderDoc = false;

        CoreSession session = parentDocument.getCoreSession();
        FileManager fileManager = Framework.getService(FileManager.class);

        // Realign parameters
        if (StringUtils.isBlank(folderishType)) {
            folderishType = DEFAULT_FOLDERISH_TYPE;
        }
        if(commitModulo <= 0) {
            commitModulo = DEFAULT_COMMIT_MODULO;
        }

        DocumentModel docFolder;
        try {
            outDirPath = tmpDirPath != null ? Files.createTempDirectory(
                    tmpDirPath, "NxUnzip")
                    : Framework.createTempDirectory(null);
            byte[] buffer = new byte[4096];
            int len = 0;
            int count = 0;

            // create output directory if it doesn't exist
            File folder = new File(outDirPath.toString());
            if (!folder.exists()) {
                folder.mkdir();
            }
            mainParentFolderOnDisk = folder;

            // We must order the zip by names (full path names), so we make sure we cill create
            // the Container before trying to create their content. For example,, as the entries
            // are ordered by hash, we may receive "folder1/file.txt" before receiving "folder1/"
            // Using a TreeMap to order by name
            File zipBlobFile = zipBlob.getFile();
            ZipFile zipFile = new ZipFile(zipBlobFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Map<String, ZipEntry> entriesByName = new TreeMap<String, ZipEntry>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                entriesByName.put(entry.getName(), entry);
            }

            // Now, we can walk this tree
            Iterator<ZipEntry> sortedEntries = entriesByName.values().iterator();
            while(sortedEntries.hasNext()) {
                ZipEntry entry = sortedEntries.next();
                //logger.warn(entry.getName());

                String fileName = entry.getName();
                if (fileName.startsWith("__MACOSX/")
                        || fileName.startsWith(".")
                        || fileName.contentEquals("../") //Avoid hacks trying to access a directory outside the current one
                        || fileName.endsWith(".DS_Store")) {
                    continue;
                }

                dcTitle = fileName.split("/")[fileName.split("/").length - 1];
                int idx = fileName.lastIndexOf("/");
                String path = idx == -1 ? "" : fileName.substring(0, idx);

                // Create the container (default is Folder)
                if (entry.isDirectory()) {

                    if (path.indexOf("/") == -1) {
                        isMainUzippedFolderDoc = true;
                        path = "";
                    } else {
                        path = path.substring(0, path.lastIndexOf("/"));
                    }

                    File newFile = new File(outDirPath.toString()
                            + File.separator + fileName);
                    newFile.mkdirs();

                    docFolder = session.createDocumentModel(
                            parentDocument.getPathAsString() + "/" + path,
                            dcTitle, "Folder");
                    docFolder.setProperty("dublincore", "title", dcTitle);
                    docFolder = session.createDocument(docFolder);
                    session.saveDocument(docFolder);

                    if (isMainUzippedFolderDoc && mainUnzippedFolderDoc == null) {
                        mainUnzippedFolderDoc = docFolder;
                        isMainUzippedFolderDoc = false;
                    }

                    continue;
                }

                // If not a directory, create the file on disk then import it
                // (and so, let Nuxeo and its configuration decide the type of doc. to create)
                File newFile = new File(outDirPath.toString() + File.separator
                        + fileName);
                FileOutputStream fos = new FileOutputStream(newFile);
                InputStream zipEntryStream = zipFile.getInputStream(entry);
                while ((len = zipEntryStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();

                // Import
                FileBlob blob = new FileBlob(newFile);
                fileManager.createDocumentFromBlob(session, blob,
                        parentDocument.getPathAsString() + "/" + path, true,
                        blob.getFilename());

                count += 1;
                if((count % commitModulo) == 0) {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                }

            } // while(sortedEntries.hasNext())

            zipFile.close();

        } catch (IOException e) {
            throw new NuxeoException(
                    "Error while unzipping and creating Documents", e);
        } finally {
            if (mainParentFolderOnDisk != null) {
                org.apache.commons.io.FileUtils.deleteQuietly(mainParentFolderOnDisk);
            }
        }

        return mainUnzippedFolderDoc;
    }

}
