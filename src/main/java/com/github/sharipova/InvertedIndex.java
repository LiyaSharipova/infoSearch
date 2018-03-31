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

    private JSONObject getJsonFromFile(String fileName) throws IOException, ParseException {
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
        while(itr.hasNext()){
            String word = itr.next();
            term = new JSONObject();
            ArrayList<Integer> docs = new ArrayList<Integer>();
            for (int i = 0; i < articles.size(); i++) {
                JSONObject article = (JSONObject) articles.get(i);

                if (isWordInArticle("Title", word, normalizer, article) || isWordInArticle("Abstract", word, normalizer, article)) {
                    docs.add(i);
                }
            }
            term.put("Value", word);
            term.put("Documents", docs);
            term.put("Count", docs.size());
            terms.add(term);
        }
        index.put("Terms", terms);

        try (FileWriter file = new FileWriter(indexFileName)) {
            file.write(index.toJSONString());
        }
    }

    private void addWords(String key, String normalizer, JSONObject article) {
        JSONObject object = (JSONObject) article.get(key);
        String text = (String) object.get(normalizer);
        Arrays.stream(text.split(" ")).forEach(p -> words.add(p));
    }

    private boolean isWordInArticle(String key, String word, String normalizer, JSONObject article) {
        JSONObject object = (JSONObject) article.get(key);
        String text = (String) object.get(normalizer);
        if (text.contains(word)) {
            return true;
        }
        return false;
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