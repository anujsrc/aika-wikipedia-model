package org.aika.wikipedia.importer;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;


public class WikipediaImporter extends CorpusImporter {

	private XMLStreamReader parser;

	public WikipediaImporter(String wikiFile) {
		try {
			InputStream in = new BZip2CompressorInputStream(new FileInputStream(new File(wikiFile)));
			XMLInputFactory factory = XMLInputFactory.newInstance();
			parser = factory.createXMLStreamReader(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		startImporterThread();
	}

	@Override
	protected void startImport() throws InterruptedException {
		String[] doc;
		do {
			doc = prepareNextDocument();
			if (doc != null) {
				getImportQueue().addDocument(doc);
			}
		} while (doc != null);
	}


	private String[] prepareNextDocument() {
		try {
	    	String title = null;
	    	String content = null;
			while (true) {
			    int event = parser.next();
			    if (event == XMLStreamConstants.END_DOCUMENT) {
			       parser.close();
			       return null;
			    }
			    if (event == XMLStreamConstants.START_ELEMENT) {
			    	if("page".equals(parser.getLocalName())) {
			        	title = null;
			        	content = null;
			        }if("title".equals(parser.getLocalName())) {
			        	title = parser.getElementText();
			        } else if("text".equals(parser.getLocalName())) {
			        	content = parser.getElementText();
			        }
			    }
			    if (event == XMLStreamConstants.END_ELEMENT) {
			    	if("page".equals(parser.getLocalName())) {
						if (!title.startsWith("Liste ") &&
							!title.startsWith("Liste_") &&
							!title.startsWith("Datei:") &&
							!title.startsWith("Vorlage:") &&
							!title.startsWith("Hilfe:") &&
							!title.startsWith("Portal:") &&
							!title.startsWith("MediaWiki:") &&
							!title.startsWith("Wikipedia:")) {

							if (content != null) {
								return new String[] {title, content};
							}
							return null;
						} else {
							System.err.println("Sorted out: " + title);
						}
			    	}
			    }
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public void close() {
		super.close();
	}
	
}
