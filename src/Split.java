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


public class Split {
	private static class Splitter extends Thread {
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
	}
	
	public static void main(String[] args) {
		Splitter documents_splitter = new Splitter("/Users/kaushal/Desktop/Trinity/InformationRetrieval/cran/cran.all.1400", "/Users/kaushal/Desktop/Trinity/InformationRetrieval/CranfieldSplit/document/");
		documents_splitter.start();
		
		try {
			documents_splitter.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void processFile(String inputFile, String outputDirectory) {
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
		bufferedWriter.close();
		fileWriter.close();
	}
}
