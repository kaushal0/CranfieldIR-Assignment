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
//package org.apache.lucene.demo;


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
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
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

public class IndexFiles {
	  
  private IndexFiles() {}

  /** Index all text files under a directory. */
  	  
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    
        
    String indexPath = "/Users/kaushal/Desktop/Trinity/InformationRetrieval/CranfieldSplit/index";
    String docsPath = "/Users/kaushal/Desktop/Trinity/InformationRetrieval/CranfieldSplit/document";
    
    boolean create = true;
    /*for(int i=0;i<args.length;i++) {
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
    }*/

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
      
      Field pathField = new StringField("path", file.toString(), Field.Store.YES);
      doc.add(pathField);
      
      doc.add(new LongPoint("modified", lastModified));
      
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
        System.out.println("updating " + file);
        writer.updateDocument(new Term("path", file.toString()), doc);
      }
    //}
  }
}
