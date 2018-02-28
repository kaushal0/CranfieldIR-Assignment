/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.demo;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.search.similarities.*;


//import lucene.CranfieldSplitter.Splitter;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {
	
  /*private static class Splitter extends Thread {
		String inputFile;
		String outputDirectory;

		Splitter(String inputFile, String outputDirectory) {
			super();
			System.out.println("inside Thread");
			this.inputFile = inputFile;
			this.outputDirectory = outputDirectory;
		}

		public void run() {
			processFile(inputFile, outputDirectory);
		}
  }*/
  
  private IndexFiles() {}

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    
    
    /*String inputDirectory = "/Users/kaushal/Desktop/Trinity/InformationRetrieval/cran/";
	Splitter documents_splitter = new Splitter(inputDirectory + "cran.all.1400", "/Users/kaushal/Desktop/Trinity/InformationRetrieval/CranfieldSplit/document/");
	documents_splitter.start();
	Splitter queries_splitter = new Splitter(inputDirectory + "cran.qry", "/Users/kaushal/Desktop/Trinity/InformationRetrieval/CranfieldSplit/queries/");
	queries_splitter.start();

	try {
		documents_splitter.join();
		queries_splitter.join();
	} catch (InterruptedException e) {
		e.printStackTrace();
	}
    */
    
    String indexPath = "/Users/kaushal/Desktop/Trinity/InformationRetrieval/CranfieldSplit/index";
    String docsPath = "/Users/kaushal/Desktop/Trinity/InformationRetrieval/CranfieldSplit/document";
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);
      
      
      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param path The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(final IndexWriter writer, Path path) throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          try {
            indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
            System.out.println("visitFile: " + file);
          } catch (IOException ignore) {
            // don't index files that can't be read.
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } else {
      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
    }
  }

  /** Indexes a single document */
  static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
    //try (InputStream stream = Files.newInputStream(file)) {
	  InputStream stream = Files.newInputStream(file);
	  InputStream stream1 = Files.newInputStream(file);
	  InputStream stream2 = Files.newInputStream(file);
	  InputStream stream3 = Files.newInputStream(file);
	  
	  
      //BufferedReader stream1 = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
      //String result = getStringFromInputStream(stream);
    	  BufferedReader br = null;
    	  BufferedReader br1 = null;
    	  BufferedReader br2 = null;
    	  
    	  boolean A = false;
    	  String line;
    	  String outputAuthor = "";
    	  String outputTitle = "";
    	  String outputContent = "";
    	  
    	  br = new BufferedReader(new InputStreamReader(stream1));
    	  br1 = new BufferedReader(new InputStreamReader(stream2));
    	  br2 = new BufferedReader(new InputStreamReader(stream3));
			
			while ((line = br.readLine()) != null) {
				if (A==true) {
					if(line.startsWith(".B")){
						A=false;
						break;
					}
					outputAuthor = outputAuthor + line + System.getProperty("line.separator");
				}
				if (line.startsWith(".A")) {
					A=true;
				}
			}
			A=false;
			while ((line = br1.readLine()) != null) {
				if (A==true) {
					if(line.startsWith(".A")){
						A=false;
						break;
					}
					outputTitle = outputTitle + line + System.getProperty("line.separator");
				}
				if (line.startsWith(".T")) {
					A=true;
				}
			}
			A=false;
			while ((line = br2.readLine()) != null) {
				if (A==true) {
					if(line.startsWith(".I")){
						A=false;
						break;
					}
					outputContent = outputContent + line + System.getProperty("line.separator");
				}
				if (line.startsWith(".W")) {
					A=true;
				}
			}

	  //System.out.println(outputAuthor);
	  //System.out.println(outputTitle);
	  System.out.println(outputContent);
	  InputStream streamAuthor = new ByteArrayInputStream(outputAuthor.getBytes(StandardCharsets.UTF_8));
	  InputStream streamTitle = new ByteArrayInputStream(outputTitle.getBytes(StandardCharsets.UTF_8));
	  InputStream streamContent = new ByteArrayInputStream(outputContent.getBytes(StandardCharsets.UTF_8));
	  
	  //System.out.println(output);
      
      //Reader resultReader = new Reader(result);
      //System.out.println(result);
      // make a new, empty document
      Document doc = new Document();
      
      // Add the path of the file as a field named "path".  Use a
      // field that is indexed (i.e. searchable), but don't tokenize 
      // the field into separate words and don't index term frequency
      // or positional information:
      Field pathField = new StringField("path", file.toString(), Field.Store.YES);
      doc.add(pathField);
      
      // Add the last modified date of the file a field named "modified".
      // Use a LongPoint that is indexed (i.e. efficiently filterable with
      // PointRangeQuery).  This indexes to milli-second resolution, which
      // is often too fine.  You could instead create a number based on
      // year/month/day/hour/minutes/seconds, down the resolution you require.
      // For example the long value 2011021714 would mean
      // February 17, 2011, 2-3 PM.
      doc.add(new LongPoint("modified", lastModified));
      
      // Add the contents of the file to a field named "contents".  Specify a Reader,
      // so that the text of the file is tokenized and indexed, but not stored.
      // Note that FileReader expects the file to be in UTF-8 encoding.
      // If that's not the case searching for special characters will fail.
      /*BufferedReader x = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
      System.out.println("BufferRead: " + x);
      String CurrLine;
      while ((CurrLine = x.readLine()) != null) {
			System.out.println(CurrLine);
      }*/
      //x = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8);
      
      
      //doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
      doc.add(new TextField("Author", new BufferedReader(new InputStreamReader(streamAuthor, StandardCharsets.UTF_8))));
      doc.add(new TextField("Title", new BufferedReader(new InputStreamReader(streamTitle, StandardCharsets.UTF_8))));
      doc.add(new TextField("Content", new BufferedReader(new InputStreamReader(streamContent, StandardCharsets.UTF_8))));
      //System.out.println(new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n")));
      
      //BufferedReader stream1 = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
      //String result = getStringFromInputStream(stream);
      //Reader resultReader = new Reader(result);
      //System.out.println(result);
      
      if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
        // New index, so we just add the document (no old document can be there):
        System.out.println("adding " + file);
        writer.addDocument(doc);
      } else {
        // Existing index (an old copy of this document may have been indexed) so 
        // we use updateDocument instead to replace the old one matching the exact 
        // path, if present:
        System.out.println("updating " + file);
        writer.updateDocument(new Term("path", file.toString()), doc);
      }
    //}
  }
  
  /*public static void processFile(String inputFile, String outputDirectory) {
		BufferedReader bufferedReader = null;
		String line;
		String output = "";
		Integer counter = 0;

		try {
			bufferedReader = new BufferedReader(new FileReader(new File(inputFile)));
			bufferedReader.readLine();

			while ((line = bufferedReader.readLine()) != null) {

				System.out.println(line);
				
				if (line.startsWith(".T")) {
					counter++;
				}
				else if (line.startsWith(".I")) {
					writeOutput(outputDirectory + counter.toString() + ".txt", output);
					output = "";
				}
				output = output + line + System.getProperty("line.separator");
				 
			}
			writeOutput(outputDirectory + counter.toString() + ".txt", output);
			bufferedReader.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (bufferedReader != null)
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public static void writeOutput(String outputFileName, String output) throws IOException {
		final File outputDirectory = new File(outputFileName);

		if (!outputDirectory.exists()) {
			outputDirectory.createNewFile();
		}

		Writer fileWriter = new FileWriter(outputDirectory);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		bufferedWriter.write(output);
		//bufferedWriter.write(analyze(output));
		bufferedWriter.close();
		fileWriter.close();
	}*/
  
  private static String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		//StringBuilder sb = new StringBuilder();
		boolean A = false;
		String line;
		String output = "";

		try {

			br = new BufferedReader(new InputStreamReader(is));
			
			while ((line = br.readLine()) != null) {
				if (A==true) {
					output = output + line + System.getProperty("line.separator");
					A=false;
				}
				if (line.startsWith(".A")) {
					A=true;
				}	
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return output;

	}
}
