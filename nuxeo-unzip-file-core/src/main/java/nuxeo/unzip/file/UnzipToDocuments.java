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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

/**
 * TODO: This code has room for optimization.
 *
 * @since 8.3
 */
public class UnzipToDocuments {

    protected static Log logger = LogFactory.getLog(UnzipToDocuments.class);

    /**
     *
     * @param parentDocument
     * @param zipBlob
     * @return the main document (Folder) containing the unzipped data
     * @since 8.3
     */
    public static DocumentModel run(DocumentModel parentDocument, Blob zipBlob) {

        String tmpDir = Environment.getDefault().getTemp().getPath();
        Path tmpDirPath = tmpDir != null ? Paths.get(tmpDir) : null;
        Path outDirPath = null;
        String dcTitle;
        File mainParentFolderOnDisk = null;
        DocumentModel mainUnzippedFolderDoc = null;
        boolean isMainUzippedFolderDoc = false;

        CoreSession session = parentDocument.getCoreSession();

        DocumentModel docFolder;
        try {
            outDirPath = tmpDirPath != null ? Files.createTempDirectory(
                    tmpDirPath, "NxUnzip")
                    : Framework.createTempDirectory(null);
            byte[] buffer = new byte[4096];
            int len = 0;

            // create output directory if it doesn't exist
            File folder = new File(outDirPath.toString());
            if (!folder.exists()) {
                folder.mkdir();
            }
            mainParentFolderOnDisk = folder;

            // get the zip file content
            ZipInputStream zis = new ZipInputStream(zipBlob.getStream());

            // get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                String fileName = ze.getName();
                if (fileName.startsWith("__MACOSX/")
                        || fileName.startsWith(".")
                        || fileName.endsWith(".DS_Store")) {
                    ze = zis.getNextEntry();
                    continue;
                }

                dcTitle = fileName.split("/")[fileName.split("/").length - 1];
                int idx = fileName.lastIndexOf("/");
                String path = idx == -1 ? "" : fileName.substring(0, idx);

                if (ze.isDirectory()) {

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

                    ze = zis.getNextEntry();
                    continue;
                }

                File newFile = new File(outDirPath.toString() + File.separator
                        + fileName);
                FileOutputStream fos = new FileOutputStream(newFile);
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();

                FileManager fileManager = Framework.getLocalService(FileManager.class);
                FileBlob blob = new FileBlob(newFile);
                fileManager.createDocumentFromBlob(session, blob,
                        parentDocument.getPathAsString() + "/" + path, true,
                        blob.getFilename());
            }

            zis.closeEntry();
            zis.close();

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
