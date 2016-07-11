package nuxeo.unzip.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.Tika;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.runtime.api.Framework;
/**
 *
 */
@Operation(id=UnzipFile.ID, category=Constants.CAT_DOCUMENT, label="Unzip", description="Unzip file and create the same structure in the current folder.")
public class UnzipFile {

    public static final String ID = "Document.UnzipFile";
    private Log logger = LogFactory.getLog(UnzipFile.class);
   
    @Context
    protected CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel input){
    	String tmpDir = Environment.getDefault().getTemp().getPath();
    	Path tmpDirPath = tmpDir != null ? Paths.get(tmpDir) : null;
    	Path outDirPath;

    	DocumentModel docFolder;
		try {
			outDirPath = tmpDirPath != null ? Files.createTempDirectory(tmpDirPath, "html5") : Framework.createTempDirectory(null);			
		   	byte[] buffer = new byte[1024];
	    	int len = 0;
	    	
	       	//create output directory if it doesn't exist
	    	File folder = new File(outDirPath.toString());
	    	
	    	if(!folder.exists()){
	       		folder.mkdir();
	       	}
	       	
	       	DocumentRef parent = input.getParentRef();
	       	DocumentModel parentDocument = session.getDocument(parent);
	      
	       	//copy the input file on temp folder
	       	BinaryBlob zipBlob = (BinaryBlob) input.getPropertyValue("file:content");
	       	
	       	//get the zip file content
	       	ZipInputStream zis = new ZipInputStream(zipBlob.getStream());
	       	
	       	//get the zipped file list entry
	       	ZipEntry ze = zis.getNextEntry();
	       	
	       	
	       	
	       	while(ze!=null){
	       		
	       		String fileName = ze.getName();
	       		if(fileName.startsWith("__MACOSX/") || fileName.startsWith(".") || fileName.endsWith(".DS_Store")){
	       			ze = zis.getNextEntry();
	       			continue;
	       		}	       		
	       		
	       		String path = fileName.lastIndexOf("/") == -1 ? "" : fileName.substring(0, fileName.lastIndexOf("/"));
	       			       		
	       		if(ze.isDirectory()){
	       			
	       			path = path.indexOf("/") ==-1 ? "" : path;
	       			
	       			File newFile = new File(outDirPath.toString() + File.separator + fileName);
	       			newFile.mkdirs();
	       			
	       			logger.error("Parent path for Folder: "+ parentDocument.getPathAsString()+"/"+path);
	       			docFolder = session.createDocumentModel(parentDocument.getPathAsString()+"/"+path, fileName.split("/")[fileName.split("/").length-1], "Folder");
	       			docFolder.setProperty("dublincore", "title", fileName.split("/")[fileName.split("/").length-1]);
	       			docFolder = session.createDocument(docFolder);
		            session.saveDocument(docFolder);
		            //parent = docFolder.getRef();
		            //parentDocument = session.getDocument(parent);
	       			ze = zis.getNextEntry();
	       			continue;
	       		}	       		
	       		File newFile = new File(outDirPath.toString() + File.separator + fileName);	       			                                     
	            FileOutputStream fos = new FileOutputStream(newFile);             
	            while ((len = zis.read(buffer)) > 0) {
	            	fos.write(buffer, 0, len);
	            }
	           		
	            fos.close();   
	            ze = zis.getNextEntry();
	            Tika tika = new Tika();
	            
	            String mimeType= (tika.detect(newFile.getPath()).equals("application/javascript")) ? "text/javascript" : tika.detect(newFile.getPath());
	            logger.error("MimeType: "+mimeType);
	            
	            FileBlob blob = new FileBlob(newFile, mimeType);
	            logger.error("Parent path for File: "+ parentDocument.getPathAsString());
	            DocumentModel doc = session.createDocumentModel(parentDocument.getPathAsString()+"/"+path, fileName.split("/")[fileName.split("/").length-1], "File");
	            doc.setProperty("dublincore", "title", fileName.split("/")[fileName.split("/").length-1]);
	            doc = session.createDocument(doc);	              
	            doc.setPropertyValue("file:content", (Serializable) blob);	          
	            doc.setPropertyValue("file:filename",fileName.split("/")[fileName.split("/").length-1]);
	            session.saveDocument(doc);	     
	       	}
	       	
	        zis.closeEntry();
	       	zis.close();
	       		       			    		
       }catch(IOException ex){
          ex.printStackTrace(); 
       }
		return input;
    }
    
}
