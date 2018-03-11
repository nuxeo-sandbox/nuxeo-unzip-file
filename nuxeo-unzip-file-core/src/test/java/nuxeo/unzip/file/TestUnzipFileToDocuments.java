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
 *     Michael Vachette
 */package nuxeo.unzip.file;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.*;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.video.api", "org.nuxeo.ecm.platform.video.core", "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core", "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.platform.tag", "nuxeo.unzip.file.nuxeo-unzip-file-core" })
@LocalDeploy({ "nuxeo.unzip.file.nuxeo-unzip-file-core:OSGI-INF/disable-listeners-contrib.xml" })
public class TestUnzipFileToDocuments {

    public static final String ZIPFILE = "nuxeo-unzip-test.zip";

    protected File zipFile;

    protected FileBlob zipFileBlob;

    protected DocumentModel testDocsFolder;

    protected DocumentModel documentWithZip;

    protected static Map<String, String> PATHS_AND_DOCTYPES;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected FileManager fileManager;

    @Before
    public void setup() {

        assertNotNull(automationService);
        assertNotNull(fileManager);

        PATHS_AND_DOCTYPES = new HashMap<String, String>();
        PATHS_AND_DOCTYPES.put("/nuxeo-unzip-test/File.pdf", "File");
        PATHS_AND_DOCTYPES.put("/nuxeo-unzip-test/f1", "Folder");
        PATHS_AND_DOCTYPES.put("/nuxeo-unzip-test/f1/f1-f1", "Folder");
        PATHS_AND_DOCTYPES.put("/nuxeo-unzip-test/f1/f1-f1/Video.mp4", "Video");
        PATHS_AND_DOCTYPES.put("/nuxeo-unzip-test/f2", "Folder");
        PATHS_AND_DOCTYPES.put("/nuxeo-unzip-test/f2/Picture.jpg", "Picture");

        zipFile = FileUtils.getResourceFileFromContext(ZIPFILE);
        zipFileBlob = new FileBlob(zipFile);

        testDocsFolder = coreSession.createDocumentModel("/", "test-unzip", "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);

        documentWithZip = coreSession.createDocumentModel(testDocsFolder.getPathAsString(), zipFile.getName(), "File");
        documentWithZip.setPropertyValue("dc:title", zipFile.getName());
        documentWithZip.setPropertyValue("file:content", zipFileBlob);
        documentWithZip = coreSession.createDocument(documentWithZip);
        documentWithZip = coreSession.saveDocument(documentWithZip);

        coreSession.save();
    }

    @After
    public void cleanup() {

        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    protected void checkZippedFolderContent() {

        String mainParentPath = testDocsFolder.getPathAsString();
        String testPath;
        DocumentModel testDoc;

        for (Entry<String, String> entry : PATHS_AND_DOCTYPES.entrySet()) {
            String subPath = entry.getKey();
            String expectedType = entry.getValue();

            testPath = mainParentPath + subPath;
            PathRef pathRef = new PathRef(testPath);
            try {
                testDoc = coreSession.getDocument(pathRef);
                assertEquals(expectedType, testDoc.getType());
            } catch (DocumentNotFoundException e) {
                assertTrue("Document " + subPath + " was not created", false);
            }
        }
    }

    @Test
    public void testUnzipToDocuments() throws Exception {

        DocumentModel mainUnzippedFolderDoc;

        mainUnzippedFolderDoc = UnzipToDocuments.run(testDocsFolder, zipFileBlob);
        assertNotNull(mainUnzippedFolderDoc);
        assertEquals("nuxeo-unzip-test", mainUnzippedFolderDoc.getTitle());

        checkZippedFolderContent();
    }

    @Test
    public void testOperation() throws OperationException {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(documentWithZip);
        chain = new OperationChain("testChain");
        chain.add(UnzipFileFoDocumentsOp.ID);
        DocumentModel result = (DocumentModel) automationService.run(ctx, chain);
        assertNotNull(result);

        checkZippedFolderContent();

    }

    @Test
    public void testOperationShouldFailWithBlobAndNoTarget() throws OperationException {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(zipFileBlob);
        chain = new OperationChain("testChain");
        chain.add(UnzipFileFoDocumentsOp.ID);
        try {
            @SuppressWarnings("unused")
            DocumentModel ignore = (DocumentModel) automationService.run(ctx, chain);
            assertTrue("Should have thrown an exception", false);
        } catch (Exception e) {
            // Automation does not just forward the IllegalArgumentException, it
            // is embedded in another exception (OPeraitonException, so far)
            // Can change with evolution of the platform, let's be cool with assertions
            String cause, message;
            cause = e.getCause().toString();
            message = e.getMessage();
            assertTrue(
                    message.indexOf("IllegalArgumentException") > -1 || cause.indexOf("IllegalArgumentException") > -1);
        }

    }

    @Test
    public void tesArchiveCheck() {

        assertTrue(UnzipFileToDocumentsImporter.looksLikeValidZip(zipFile));

        assertFalse(UnzipFileToDocumentsImporter.looksLikeValidZip(null));

        File notValidZip = FileUtils.getResourceFileFromContext("not-a-valid-zip.zip");
        assertFalse(UnzipFileToDocumentsImporter.looksLikeValidZip(notValidZip));
    }

    @Test
    public void testImportViaFileManager() throws Exception {

        DocumentModel doc = fileManager.createDocumentFromBlob(coreSession, zipFileBlob,
                testDocsFolder.getPathAsString(), true, "TheArchive");
        assertTrue(doc != null);
        assertEquals("nuxeo-unzip-test", doc.getTitle());

        checkZippedFolderContent();

    }
}
