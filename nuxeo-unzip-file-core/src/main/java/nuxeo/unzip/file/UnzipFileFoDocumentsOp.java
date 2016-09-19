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
 *     Frederic Vadon
 *     Thibaud Arguillere
 */
package nuxeo.unzip.file;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * This operation extracts the zip file (either a Blob input or a Blob field in
 * a Document) and creates Documents according to their types and preserving the
 * hierarchy.
 *
 */
@Operation(id = UnzipFileFoDocumentsOp.ID, category = Constants.CAT_DOCUMENT, label = "Unzip and create documents", description = "Unzip file and create the same structure in the target (the current folder if target isn't provided). When using a blob as input, the target parameter is required. The operation does nothing if the input is null.")
public class UnzipFileFoDocumentsOp {

    public static final String ID = "Document.UnzipFileToDocuments";

    @Context
    protected CoreSession session;

    @Param(name = "target", required = false)
    protected DocumentModel target;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {

        if (input == null) {
            return input;
        }

        DocumentRef parent = input.getParentRef();
        DocumentModel parentDocument;

        if (target == null) {
            parentDocument = session.getDocument(parent);
        } else {
            parentDocument = target;
        }

        if (StringUtils.isBlank(xpath)) {
            xpath = "file:content";
        }
        Blob zipBlob = (Blob) input.getPropertyValue(xpath);

        /* ignore = */UnzipToDocuments.run(parentDocument, zipBlob);

        return input;
    }

    @OperationMethod
    public Blob run(Blob input) {

        if (input == null) {
            return input;
        }

        if (target == null) {
            throw new IllegalArgumentException(
                    "When receiving a Blob, the target parameter cannot be empty");
        }

        /* ignore = */UnzipToDocuments.run(target, input);

        return input;
    }

}
