package lucenex;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Main {

	public static String indexPath = "src/main/index";
	public static String documentsPath = "src/main/documents";

	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();

		Path index = Paths.get(indexPath);
		Directory indexDirectory = FSDirectory.open(index);
		List<Document> documents = FileHandler.getDocuments(documentsPath);
		indexDocs(indexDirectory, null, documents);

		long endTime = System.currentTimeMillis();
		long indexingTime = endTime - startTime;
		System.out.println("Indexing time: " + indexingTime + " milliseconds");

		IndexReader reader = DirectoryReader.open(indexDirectory);
		IndexSearcher searcher = new IndexSearcher(reader);
		Scanner scanner = new Scanner(System.in);
		String input;

		while (true) {
			System.out.println(
					"\nUsage:\n"
					+ "Insert \"title: \" or \"content: \" followed by a term or a phrase to search for.\n"
							+ "Insert \"all\" to get all the documents.\n"
							+ "Insert \"exit\" to close the program.\n");
			System.out.print("Enter query: ");
			input = scanner.nextLine();

			Query query = null;
			QueryParser parser = null;

			if (input.startsWith("title:")) {
				String title = input.substring(7);
				parser = new QueryParser("title", new SimpleAnalyzer());
				try {
					query = parser.parse(title);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if (input.startsWith("content:")) {
				String content = input.substring(9);
				parser = new QueryParser("content", new ItalianAnalyzer());
				try {
					query = parser.parse(content);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if (input.equals("exit")) {
				System.out.println("Goodbye.\n");
				break;
			} else if (input.equals("all")) {
				query = new MatchAllDocsQuery();
			} else {
				System.out.println("Invalid input.\n");
			}

			if (query != null) {
				runQuery(searcher, query);
			}
		}

		scanner.close();
	}

	public static void indexDocs(Directory directory, Codec codec, List<Document> documents) throws IOException {
		Analyzer defaultAnalyzer = new StandardAnalyzer();
//		CharArraySet stopWords = new CharArraySet(Arrays.asList(
//				"a, ad, al, alla, alle, agli, all', alla, anzi, anche, avanti, c, ci, col, coi, con, contro, da, dai, dal, dall', dalla, dappertutto, del, dell', della, dentro, di, dove, e, ecco, fra, gli, il, in, infatti, insomma, invece, l', lo, lui, ma, me, meno, molto, ne, negli, nell', nella, nemmeno, no, noi, non, per, però, piuttosto, più, più o meno, quando, quasi, quanto, sarebbe, sarebbe stato, se, sebbene, soltanto, sopra, sotto, sulla, sui, sul, sulle, tra, tu, tuttavia, verso, vi, voi"),
//				true);
		Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
		// perFieldAnalyzers.put("title", new WhitespaceAnalyzer());
		perFieldAnalyzers.put("title", new SimpleAnalyzer());
		// perFieldAnalyzers.put("content", new StandardAnalyzer(stopWords));
		perFieldAnalyzers.put("content", new ItalianAnalyzer());

		Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers);
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		if (codec != null) {
			config.setCodec(codec);
		}
		IndexWriter writer = new IndexWriter(directory, config);
		writer.deleteAll();

		for (Document doc : documents) {
			writer.addDocument(doc);
		}

		writer.commit();
		writer.close();
	}

	private static void runQuery(IndexSearcher searcher, Query query) throws IOException {
		runQuery(searcher, query, false);
	}

	private static void runQuery(IndexSearcher searcher, Query query, boolean explain) throws IOException {
		TopDocs hits = searcher.search(query, 10);

		int numberOfHits = hits.scoreDocs.length;
		if (numberOfHits == 0) {
			System.out.println("No document found. Please try again with other terms or phrases.\n");
		}

		for (int i = 0; i < numberOfHits; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = searcher.doc(scoreDoc.doc);
			System.out.println("doc" + scoreDoc.doc + ":" + doc.get("title") + " (" + scoreDoc.score + ")");
			if (explain) {
				Explanation explanation = searcher.explain(query, scoreDoc.doc);
				System.out.println(explanation);
			}
		}
	}

}