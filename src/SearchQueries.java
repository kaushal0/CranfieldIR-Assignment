package org.apache.lucene.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class SearchQueries {
	
	public static void main(String[] args) throws Exception {
	    String usage =
	      "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
	    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
	      System.out.println(usage);
	      System.exit(0);
	    }

	    String queryPath= "/Users/kaushal/Desktop/Trinity/InformationRetrieval/cran/cran.qry";
	    String index = "/Users/kaushal/Desktop/Trinity/InformationRetrieval/CranfieldSplit/index";
	    String field = "Content";
	    String sim = "bm25";
	    String queries = null;
	    int repeat = 0;
	    boolean raw = false;
	    String queryString = "experimental investigation of the aerodynamics of a\n" + 
	    		"wing in a slipstream .";
	    int hitsPerPage = 100;
	    
	    /*for(int i = 0;i < args.length;i++) {
	      if ("-index".equals(args[i])) {
	        index = args[i+1];
	        i++;
	      } else if ("-field".equals(args[i])) {
	        field = args[i+1];
	        i++;
	      } else if ("-queries".equals(args[i])) {
	        queries = args[i+1];
	        i++;
	      } else if ("-query".equals(args[i])) {
	        queryString = args[i+1];
	        i++;
	      } else if ("-repeat".equals(args[i])) {
	        repeat = Integer.parseInt(args[i+1]);
	        i++;
	      } else if ("-raw".equals(args[i])) {
	        raw = true;
	      } else if ("-paging".equals(args[i])) {
	        hitsPerPage = Integer.parseInt(args[i+1]);
	        if (hitsPerPage <= 0) {
	          System.err.println("There must be at least 1 hit per page.");
	          System.exit(1);
	        }
	        i++;
	      }
	    }*/
	    
	    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
	    IndexSearcher searcher = new IndexSearcher(reader);
	    Analyzer analyzer = new StandardAnalyzer();
	    
	    //Similarity simfn = new BM25Similarity();
	    //searcher.setSimilarity(searcher.getDefaultSimilarity());
	    //searcher.setSimilarity(new BM25Similarity(0.2F, 0.5F));
	    //Similarity si= new ClassicSimilarity();
	    //searcher.setSimilarity(si);
	    //sim = "TF-IDF";
	    //System.out.println(reader.getDocCount(field));

	    BufferedReader qBuffer = new BufferedReader(new FileReader(new File(queryPath)));
	    //BufferedReader in = null;
	    /*if (queries != null) {
	      in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
	    } else {
	      in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
	    }*/
	    
	    /**/
	        
	    QueryParser parser = new QueryParser(field, analyzer);
	    
	    
	    //while (true) {
	    		
			//String line = qBuffer.readLine();
	    		int counter=0;
	    		Query query;
	    		String line;
	    		String queryText="";
	    		String[] pair= {};
			boolean firstQuery=true;
			
			
			/*while ((line = qBuffer.readLine()) != null) {

				//System.out.println(line);
				
				if (line.startsWith(".W")) {
					queryText = "";
					continue;
				}
				else if (line.startsWith(".I")) {
					if (firstQuery==true) {
						firstQuery=false;
						continue;
					}
					pair = line.split(" ", 2);
					//System.out.println(pair[1]);
					//System.out.println(queryText);
					query= parser.parse(queryText);
					System.out.println(queryText);
					doPagingSearch(qBuffer, searcher, pair[1], query, simstring);
				}
				
				queryText = queryText + line;// + System.getProperty("line.separator");
			}*/
			
			while ((line = qBuffer.readLine()) != null) {

				//System.out.println(line);
				
				if (line.startsWith(".I")) {
					if (firstQuery==true) {
						//pair = line.split(" ", 2);
						counter++;
						firstQuery=false;
						continue;
					}
					//query= parser.parse(queryText);
					//System.out.println(counter);
					//System.out.println(queryText);
					doPagingSearch(qBuffer, searcher, String.valueOf(counter), queryText, sim);
					//pair = line.split(" ", 2);
					counter++;
					continue;
				}
				else if (line.startsWith(".W")) {
					queryText="";
					continue;
				}				
				queryText = queryText + line;// + System.getProperty("line.separator");
			}
			query= parser.parse(queryText);
			doPagingSearch(qBuffer, searcher, String.valueOf(counter), queryText, sim);
			
				
			/*	//System.out.println(line);
				//System.out.println(line);
				if(A==true) {
					if(line.startsWith(".I")) {
						//System.out.println(line);
						System.out.println(queryText);
						System.out.println("\n");
						//String[] pair = line.split(" ", 2);
						//System.out.println(pair[1]);
						//query= parser.parse(queryText);
						//doPagingSearch(qBuffer, searcher, pair[1], query, simstring);
						A=false;
						continue;
					}
					queryText = queryText + line + System.getProperty("line.separator");
				}
				else if (line.startsWith(".W")) {
					A=true;
				}
				else if (line == null || line.length() == -1) {
					break;
				}
				
			}
			//String[] pair = line.split(" ", 2);
			//Query query = parser.parse(pair[1]);

			//doPagingSearch(in, searcher, pair[0], query, simstring);
		//}*/
	    
	    qBuffer.close();
	  }
	
	public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, String qid, String queryText, String sim)	 
			throws IOException, ParseException {
		
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser1 = new QueryParser("Title", analyzer);
		QueryParser parser2 = new QueryParser("Author", analyzer);
		QueryParser parser3 = new QueryParser("Content", analyzer);
		    
		Query query1 = parser1.parse(queryText);
		Query query2 = parser2.parse(queryText);
		Query query3 = parser3.parse(queryText);
		
	    Query boostedTermQuery1 = new BoostQuery(query1, (float) 2);
	    Query boostedTermQuery2 = new BoostQuery(query2, (float) 2.5);
	    Query boostedTermQuery3 = new BoostQuery(query3, (float) 0.5);
	
	    BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
	    booleanQuery.add(boostedTermQuery1, Occur.SHOULD);
	    booleanQuery.add(boostedTermQuery2, Occur.SHOULD);
	    booleanQuery.add(boostedTermQuery3, Occur.SHOULD);
		
		//PrintStream out = new PrintStream(new FileOutputStream("/Users/kaushal/Desktop/Trinity/InformationRetrieval/cran/outputEval1.log", true));
		// Collect enough docs to show 5 pages 
		TopDocs results = searcher.search(booleanQuery.build(), 15);
		ScoreDoc[] hits = results.scoreDocs;
		//HashMap<String, String> DocOutput = new HashMap<String, String>(1000);
		int numTotalHits = Math.toIntExact(results.totalHits);
		
		int count;
		int startDoc = 0;
		int endDoc = Math.min(numTotalHits, 15);
		//System.out.println(endDoc);

		for (int i = startDoc; i < endDoc; i++) {
			Document doc = searcher.doc(hits[i].doc);
			//System.out.println(doc);
			String docno = doc.get("path");
			//docno.substring(15, 20);
			count=0;
			while(docno.charAt(count)!='.') {
					count++;
			}
			docno= docno.substring(76, count);
			
			qid= qid.replaceFirst("^0+(?!$)", "");
			//System.out.println(qid);
			// There are duplicate document numbers in the FR collection, so only output a given docno once.
			/*if (DocOutput.containsKey(docno)) {
				continue;
			}*/
			//DocOutput.put(docno, docno);
			System.out.println(qid+" Q0 "+docno+" "+i+" "+hits[i].score+" "+sim);
			//System.out.println(qid+" Q0 "+docno+" "+i+" "+hits[i].score+" "+sim);			
			//System.out.println(qid+" "+0+" "+docno+" "+Math.round((hits[i].score)));
			//System.setOut(out);
		}
	}	
	
}
