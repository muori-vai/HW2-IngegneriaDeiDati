package lucenex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;

public class FileHandler {

	public static List<Document> getDocuments(String folderPath) {
		List<Document> documents = new ArrayList<>();

		File folder = new File(folderPath);

		if (folder.exists() && folder.isDirectory()) {
			File[] files = folder.listFiles();

			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						String fileName = file.getName();
						if (fileName.endsWith(".txt")) {
							fileName = fileName.substring(0, fileName.length() - 4);
							String fileContent = readFileContent(file);

							Document doc = new Document();
							doc.add(new TextField("title", fileName, Field.Store.YES));
							doc.add(new TextField("content", fileContent, Field.Store.YES));
							documents.add(doc);
						}
					}
				}
			}
		} else {
			System.out.println("Folder or path does not exist!\n");
		}
		return documents;
	}

	private static String readFileContent(File file) {
		StringBuilder content = new StringBuilder();
		try (Scanner scanner = new Scanner(file)) {
			while (scanner.hasNextLine()) {
				content.append(scanner.nextLine());
				content.append(System.lineSeparator());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content.toString();
	}
}
