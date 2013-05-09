import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.queryparser.classic.ParseException;

public class Search {
	public static void main(String[] args) throws IOException, ParseException, InvalidTokenOffsetsException  {
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_41);

		// 2. query
		if (args.length<2) {
			System.out.println("Utilize os parâmetros <palavras-chave> <indicepath> [basepath]");
			System.exit(1);
		}
		String querystr = args[0];
		String indexpath = args[1];
		String basepath = args.length > 2 ? args[2] : "";
		if (basepath.length()>1 && basepath.charAt(basepath.length()-1)!='/') {basepath+="/";}

		Query q = new QueryParser(Version.LUCENE_41, "conteudo", analyzer).parse(querystr);

		// 3. search
		SimpleFSDirectory index = new SimpleFSDirectory(new File(indexpath));
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TotalHitCountCollector counter = new TotalHitCountCollector();
		searcher.search(q, counter);
		TopScoreDocCollector collector = TopScoreDocCollector.create(counter.getTotalHits()+5, true);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		// 4. display results
		//Search.printAllFieldsN3(hits,searcher,querystr);
		Search.printCustomN3(hits,searcher, querystr,basepath);
		
		// reader can only be closed when there
		// is no need to access the documents any more.
		reader.close(); 
	}
	public static void printAllFieldsN3(ScoreDoc[] hits,IndexSearcher searcher,String querystr) throws IOException{
		System.out.println("@prefix p: <common.n3#> .");
		for(int i=0;i<hits.length;++i) {
		  int docId = hits[i].doc;
		  Document d = searcher.doc(docId);
		  System.out.println("[\tp:docId p:"+docId +" ;");
		  List<IndexableField> list=d.getFields();
		  for (IndexableField indexableField : list) {
			System.out.println("\tp:"+indexableField.name()+"\t\""+indexableField.stringValue()+"\" ;");
			System.out.println("\tp:hasTerm\t\""+querystr+"\" .");
		  }
		  System.out.println("] .");
		}
		System.out.println("#Found " + hits.length + " hits.");
	}
	public static void printCustomN3(ScoreDoc[] hits,IndexSearcher searcher,String querystr,String basepath) throws IOException{
		System.out.println("@prefix p: <common.n3#> .");
		
		for(int i=0;i<hits.length;++i) {
		  int docId = hits[i].doc;
		  Document d = searcher.doc(docId);
		  
		  System.out.print("\""+basepath+d.get("caminho")+"\" ");
		  String[] fields={};
		  for (int j = 0; j < fields.length; j++) {
			  System.out.println("\tp:"+fields[j]+"\t\""+d.get(fields[j])+"\" ;");
		  }
		  System.out.println("\tp:positiveSearch\t\""+StringEscapeUtils.escapeJava(querystr)+"\" .");
		}
		System.out.println("#Basepath:"+basepath);
		System.out.println("#Query:"+querystr);
		System.out.println("#Found " + hits.length + " hits.");
	}
}
