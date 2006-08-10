/*
 * (c) Copyright IBM Corp. 2004, 2005 All Rights Reserved.
 */
package org.dita.dost.reader;

import java.io.File;

import org.dita.dost.log.DITAOTJavaLogger;
import org.dita.dost.module.Content;
import org.dita.dost.module.ContentImpl;
import org.dita.dost.util.Constants;
import org.dita.dost.util.FileUtils;
import org.dita.dost.util.MergeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
/**
 * MergeTopicParser reads topic file and transform the references to other dita
 * files into internal references. The parse result of MergeTopicParser will be
 * returned to MergeMapParser and serves as part of intermediate merged result.
 * 
 * @author Zhang, Yuan Peng
 */
public class MergeTopicParser extends AbstractXMLReader {
	private static StringBuffer topicInfo = null;
	private ContentImpl content;
	private String dirPath = null;
	private String filePath = null;
	private boolean isFirstTopicId = false;
	private DITAOTJavaLogger logger = null;

	private XMLReader reader = null;
	private String retId = null;
	private MergeUtils util;
	
	/**
	 * Default Constructor
	 */
	public MergeTopicParser() {
		logger = new DITAOTJavaLogger();
		try{
			if (System.getProperty(Constants.SAX_DRIVER_PROPERTY) == null){
				//The default sax driver is set to xerces's sax driver
				System.setProperty(Constants.SAX_DRIVER_PROPERTY, Constants.SAX_DRIVER_DEFAULT_CLASS);
			}
			if(reader == null){
				reader = XMLReaderFactory.createXMLReader();
				reader.setContentHandler(this);
//				reader.setProperty(Constants.LEXICAL_HANDLER_PROPERTY,this);
				reader.setFeature(Constants.FEATURE_NAMESPACE_PREFIX, true);
//				reader.setFeature(Constants.FEATURE_VALIDATION, true); 
//				reader.setFeature(Constants.FEATURE_VALIDATION_SCHEMA, true);
			}
			if(topicInfo == null){
				topicInfo = new StringBuffer(Constants.INT_1024);
			}
			
			content = new ContentImpl();
			util = MergeUtils.getInstance();
		}catch (Exception e){
			logger.logException(e);
		}
	}



	/**
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length) throws SAXException {
		topicInfo.append(ch, start, length);
	}


	/**
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		
	}

	/**
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName) throws SAXException {
		topicInfo.append(Constants.LESS_THAN)
		.append(Constants.SLASH)
		.append(qName)
		.append(Constants.GREATER_THAN);
	}

	
	/**
	 * @see org.dita.dost.reader.AbstractReader#getContent()
	 */
	public Content getContent() {
		content.setValue(topicInfo);
		return content;
	}



	/**
	 * @param classValue
	 * @param attValue
	 * @return
	 */
	private String handleID(String classValue, String attValue) {
		String retAttValue = attValue;
		if(classValue != null 
				&& classValue.indexOf(Constants.ATTR_CLASS_VALUE_TOPIC)!=-1){
			// Process the topic element id
			String value = filePath+Constants.SHARP+attValue;
			if(util.findId(value)){
				topicInfo.append(Constants.STRING_BLANK)
				.append("oid").append(Constants.EQUAL).append(Constants.QUOTATION)
				.append(attValue).append(Constants.QUOTATION);
				retAttValue = util.getIdValue(value);
			}else{
				topicInfo.append(Constants.STRING_BLANK)
				.append("oid").append(Constants.EQUAL).append(Constants.QUOTATION)
				.append(attValue).append(Constants.QUOTATION);
				retAttValue = util.addId(value);           			
			}
			if(isFirstTopicId){
				isFirstTopicId = false;
				retId = retAttValue;
				util.addId(filePath,retAttValue);
			}
		}
		return retAttValue;
	}


	/**
	 * @param sharpIndex
	 * @param attValue
	 * @return
	 */
	private String handleLocalDita(int sharpIndex, String attValue) {
		String fileId;
		String topicId;
		String pathFromMap;
		int slashIndex;
		int index;
		String retAttValue = attValue;
		if (sharpIndex != -1){ // href value refer to an id in a topic
			pathFromMap = FileUtils.resolveTopic(new File(filePath).getParent(),attValue.substring(0,sharpIndex));
			topicInfo.append(Constants.STRING_BLANK)
			.append("ohref").append(Constants.EQUAL).append(Constants.QUOTATION)
			.append(pathFromMap)
			.append(attValue.substring(sharpIndex))
			.append(Constants.QUOTATION);
			
			topicId = attValue.substring(sharpIndex);
			slashIndex = topicId.indexOf(Constants.SLASH);
			index = attValue.indexOf(Constants.SLASH, sharpIndex);
			topicId = (slashIndex != -1) 
					? pathFromMap + topicId.substring(0, slashIndex)
					: pathFromMap + topicId;

			
			if(util.findId(topicId)){// topicId found 
				String prefix = Constants.SHARP + util.getIdValue(topicId);
				retAttValue = (index!=-1)? prefix + attValue.substring(index) : prefix;
			}else{//topicId not found
				String prefix = Constants.SHARP + util.addId(topicId);
				retAttValue = (index!=-1)? prefix + attValue.substring(index) : prefix;
			}

		}else{ // href value refer to a topic
			pathFromMap = FileUtils.resolveTopic(new File(filePath).getParent(),attValue);
			    					
			topicInfo.append(Constants.STRING_BLANK)
			.append("ohref").append(Constants.EQUAL).append(Constants.QUOTATION)
			.append(pathFromMap)
			.append(Constants.QUOTATION);
			
			if(util.findId(pathFromMap)){
				retAttValue = Constants.SHARP + util.getIdValue(pathFromMap);
			}else{
				fileId = util.getFirstTopicId(pathFromMap, dirPath);
				if (util.findId(pathFromMap + Constants.SHARP + fileId)){
					util.addId(pathFromMap,util.getIdValue(pathFromMap + Constants.SHARP + fileId));
					retAttValue = Constants.SHARP + util.getIdValue(pathFromMap + Constants.SHARP + fileId);
				}else{
					retAttValue = Constants.SHARP + util.addId(pathFromMap);
					util.addId(pathFromMap + Constants.SHARP + fileId, util.getIdValue(pathFromMap));
				}
				
			}
		}
		return retAttValue;
	}

    /**
     * Parse the file
	 * @param filename
	 * @param dir
	 * @return
	 */
	public String parse(String filename,String dir){
		int index = filename.indexOf(Constants.SHARP);
		dirPath = dir;
		try{
			filePath = (index != -1) ? filename.substring(0,index):filename;
			reader.parse(dir + File.separator + filePath);
			return retId;
		}catch (Exception e){
			logger.logException(e);
			return null;
		}
	}

	/**
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		isFirstTopicId = true;
	}

	/**
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		//write the start element of topic parsing logic;
		String classValue = null;
		String scopeValue = null;
		String formatValue = null;
		int attsLen = atts.getLength();
		int sharpIndex;
		
		topicInfo.append(Constants.LESS_THAN).append(qName);
		classValue = atts.getValue(Constants.ATTRIBUTE_NAME_CLASS);		
		
		for (int i = 0; i < attsLen; i++) {
            String attQName = atts.getQName(i);
            String attValue = atts.getValue(i);
            
            if(Constants.ATTRIBUTE_NAME_ID.equals(attQName)){           	
            	attValue = handleID(classValue, attValue);
            }
            
            if(classValue != null
            		&&(classValue.indexOf(Constants.ATTR_CLASS_VALUE_XREF) != -1
            		|| classValue.indexOf(Constants.ATTR_CLASS_VALUE_LINK) != -1)
            		&& Constants.ATTRIBUTE_NAME_HREF.equals(attQName) 
            		&& attValue != null
            		&& !Constants.STRING_EMPTY.equalsIgnoreCase(attValue)){
            	scopeValue = atts.getValue(Constants.ATTRIBUTE_NAME_SCOPE);
        		formatValue = atts.getValue(Constants.ATTRIBUTE_NAME_FORMAT);
        		sharpIndex = attValue.indexOf(Constants.SHARP);
        		
//        		if (attValue.indexOf(Constants.SHARP) != -1){
//        			attValue = attValue.substring(0, attValue.indexOf(Constants.SHARP));
//        		}
        		
        		if((scopeValue == null 
    					|| Constants.ATTR_SCOPE_VALUE_LOCAL.equalsIgnoreCase(scopeValue))
    					&& (formatValue == null 
    							|| Constants.ATTR_FORMAT_VALUE_DITA.equalsIgnoreCase(formatValue))){
        			attValue = handleLocalDita(sharpIndex, attValue);
    			}
        		
            }
            
            //output all attributes
            topicInfo.append(Constants.STRING_BLANK)
            		.append(attQName).append(Constants.EQUAL).append(Constants.QUOTATION)
            		.append(attValue).append(Constants.QUOTATION);
        }
		topicInfo.append(Constants.GREATER_THAN);
	}
	

}