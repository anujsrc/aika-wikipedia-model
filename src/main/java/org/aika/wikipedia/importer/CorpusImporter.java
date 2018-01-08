package org.aika.wikipedia.importer;


import java.util.Iterator;


/**
 * This class is the superclass of all classes that import corpora. This class provides an iterable interface to the outside,
 * so that documents can be imported one by one, without having to import the whole corpus into memory. To that the internal
 * importer code has to be written in form of an iterator, an separate thread is started in which the internal import loop is
 * running. The class {@link DocumentImportQueue} is used to synchronize this inner loop with the outer loop of the surrounding program.
 *
 * @author Lukas Molzberger
 */
public abstract class CorpusImporter {

	private final DocumentImportQueue queue = new DocumentImportQueue();

	private Thread importerThread;

	/**
	 * Start the thread for the internal importing loop.
	 */
	public void startImporterThread() {
		this.importerThread = new Thread(
			null,
			new Runnable() {
				public void run() {
					try {
						startImport();
					} catch (InterruptedException e) {
						System.out.println(getImporterName() + "Thread has been terminated!");
					}
					CorpusImporter.this.queue.close();
				}
			},
			getImporterName() + "Thread"
		);
		this.importerThread.start();

		this.queue.init();
	}

	public final String getImporterName() {
		return this.getClass().getSimpleName();
	}


	public void close() {
		this.importerThread.interrupt();
	}

	/**
	 * Returns an iterable over the documents to be imported.
	 * @return an iterable over the documents to be imported.
	 */
	public Iterable<String[]> getDocuments() {
		return new DefaultIterable<>(this.queue);
	}


	/**
	 * Start the inner import loop.
	 * @throws InterruptedException
	 */
	protected abstract void startImport() throws InterruptedException;


	/**
	 * Returns the import queue which is used to forward the current document from the inner import loop to the outer loop.
	 * @return
	 */
	protected DocumentImportQueue getImportQueue() {
		return this.queue;
	}

	/**
	 * This class provides an synchronization for the inner and the outer import loop. Each loop
	 * stalls until the other loop has finished processing the current document.
	 *
	 * @author Lukas Molzberger
	 */
	public static class DocumentImportQueue implements Iterator<String[]> {
		private boolean getNextDocument = true;
		private String[] currentDocument;


		public void init() {
			getNextDocument();
		}

		private synchronized void getNextDocument() {
			try {
				this.getNextDocument = true;
				notify();
				while(this.getNextDocument) {
					wait();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean hasNext() {
			return this.currentDocument != null;
		}

		public String[] next() {
			String[] doc = this.currentDocument;
			getNextDocument();
			return doc;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Forward a document to the outer loop.
		 * @param doc
		 * @throws InterruptedException
		 */
		public synchronized void addDocument(String[] doc) throws InterruptedException {
			if(doc == null) {
				return;
			}
			this.currentDocument = doc;
			this.getNextDocument = false;
			notify();
			while(!this.getNextDocument) {
				wait();
			}
		}

		/**
		 * Close the import queue.
		 */
		private synchronized void close() {
			this.currentDocument = null;
			this.getNextDocument = false;
			notify();
		}
	}

}
