package com.github.sharipova;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.text.Collator;
import java.util.*;

/**
 * Created by liya on 07.03.18.
 */
public class InvertedIndex {
    private Set<String> words = new TreeSet<String>();
    private JSONArray terms = new JSONArray();

    public static JSONObject getJsonFromFile(String fileName) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(new FileReader(
                fileName));
    }

    private void createInvertedIndexes(String fileName, String normalizer) throws IOException, ParseException {
        JSONObject index = new JSONObject();
        JSONObject term;
        JSONObject jsonObject = getJsonFromFile(fileName);
        JSONArray articles = (JSONArray) jsonObject.get("Articles");
        String indexFileName = normalizer + ".json";

        for (int i = 0; i < articles.size(); i++) {
            JSONObject article = (JSONObject) articles.get(i);
            addWords("Title", normalizer, article);
            addWords("Abstract", normalizer, article);
        }
        Iterator<String> itr = words.iterator();
        while (itr.hasNext()) {
            String word = itr.next();
            term = new JSONObject();
            JSONArray documentsTitle = new JSONArray();
            JSONArray documentsAbstract = new JSONArray();
            JSONObject documentTitle = null;
            JSONObject documentAbs = null;
            Set<Integer> docsTitle = new HashSet<>();
            Set<Integer> docsAbstract = new HashSet<>();
            for (int i = 1; i < articles.size() + 1; i++) {
                JSONObject article = (JSONObject) articles.get(i - 1);

                int occuranceTitle = isWordInArticle("Title", word, normalizer, article);
                addDocumentIfWordOccurs(documentsTitle, docsTitle, i, occuranceTitle, article, "Title", normalizer);
                int occuranceAbstract = isWordInArticle("Abstract", word, normalizer, article);
                addDocumentIfWordOccurs(documentsAbstract, docsAbstract, i, occuranceAbstract, article, "Abstract", normalizer);
            }

            term.put("Value", word);
            term.put("DocumentsAbstract", documentsAbstract);
            term.put("DocumentsTitle", documentsTitle);
//            union two sets
            docsAbstract.addAll(docsTitle);
            term.put("Count", docsAbstract.size());
            terms.add(term);
        }
        index.put("Terms", terms);

        try (FileWriter file = new FileWriter(indexFileName)) {
            file.write(index.toJSONString());
        }
    }

    private void addDocumentIfWordOccurs(JSONArray documents, Set<Integer> docsTitle, int id,
                                         int occurance, JSONObject article, String key, String normalizer) {
        if (occurance != 0) {
            JSONObject document = new JSONObject();
            document.put("Id", id);
            docsTitle.add(id);
            document.put("WordCount", occurance);
            document.put("TotalWordsCount", getTotalWordsCount(key, article, normalizer));
            documents.add(document);
        }
    }

    private void addWords(String key, String normalizer, JSONObject article) {
        JSONObject object = (JSONObject) article.get(key);
        String text = (String) object.get(normalizer);
        Arrays.stream(text.split(" ")).forEach(p -> words.add(p));
    }

    private int isWordInArticle(String key, String term, String normalizer, JSONObject article) {
        JSONObject object = (JSONObject) article.get(key);
        String text = (String) object.get(normalizer);
        String[] words = text.split(" ");
        int occurance = 0;
        for (String word : words) {
            if (word.equals(term))
                occurance++;
        }
        return occurance;
    }

    private int getTotalWordsCount(String key, JSONObject article, String normalizer) {
        JSONObject object = (JSONObject) article.get(key);
        String text = (String) object.get(normalizer);
        String[] words = text.split(" ");
        return words.length;
    }


    public static void main(String[] args) {
        InvertedIndex index = new InvertedIndex();
        try {
            index.createInvertedIndexes("result.json", "Porter");
            index.createInvertedIndexes("result.json", "MyStem");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
