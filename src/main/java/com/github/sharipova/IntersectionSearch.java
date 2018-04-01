package com.github.sharipova;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация метода пересечения
 */
public class IntersectionSearch {

    public void search(String query, String IndexFileName, String normalizer, String documentsFile) throws IOException, ParseException {
        Set<Integer> allDocIds = getAllDocumentIds(documentsFile);
//      read the invertedIndex file
        JSONArray terms = getJsonFromFile(IndexFileName, "Terms");
//      get words from query
        String[] words = query.toLowerCase().split("\\s+");
        Map<String, Set<Integer>> wordWithDocsMap = new TreeMap<>();

        Arrays.stream(words).forEach(word -> {
            String searchWord = normalize(word, normalizer);
//          ids of documents with a searchWord
            Set<Integer> wordDocIds = new TreeSet<>();

            for (int i = 0; i < terms.size(); i++) {
                JSONObject term = (JSONObject) terms.get(i);
                if (term.get("Value").equals(searchWord)) {
                    JSONObject documents = (JSONObject) term.get("Documents");
                    Set<Integer> docsTitle = jsonArrayToSet((JSONArray) documents.get("Title"));
                    Set<Integer> docsAbstract = jsonArrayToSet((JSONArray) documents.get("Abstract"));
//                    Set<Integer> docsAbstract = (Set<Integer>) documents.get("Abstract");
                    wordDocIds.addAll(union(docsTitle, docsAbstract));
                }
            }
            if (word.startsWith("-"))
                wordWithDocsMap.put(word, getInversion(wordDocIds, allDocIds));
            else
                wordWithDocsMap.put(word, wordDocIds);


        });
        Set<Integer> result = new TreeSet<>();
        List<String> wordsList = new ArrayList<>(wordWithDocsMap.keySet());
        result = wordWithDocsMap.get(wordsList.get(0));
        for (int i = 0; i < wordsList.size() - 1; i++) {
            if (!(result.isEmpty() && i != 0))
                result = getIntersection(result,
                        new TreeSet<>(wordWithDocsMap.get(wordsList.get(i))));
        }

        printResult(query, result);

    }

    private void printResult(String query, Set<Integer> result) {
        System.out.println("Query: " + query);
        System.out.println("Result documents ids:");
        result.forEach(integer -> System.out.print(integer + " "));
        System.out.println("");
        System.out.println("Сount: " + result.size());
    }

    private JSONArray getJsonFromFile(String fileName, String key) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(fileName));

        return (JSONArray) jsonObject.get(key);
    }

    private Set<Integer> getAllDocumentIds(String fileName) throws IOException, ParseException {
        Set<Integer> allDocIds = new HashSet<>();
        JSONArray documents = getJsonFromFile(fileName, "Articles");
        for (int i = 1; i < documents.size() + 1; i++) {
            JSONObject document = (JSONObject) documents.get(i - 1);
            allDocIds.add(Integer.parseInt((String) document.get("Id")));
        }
        return allDocIds;
    }

    private String normalize(String word, String normalizer) {
        String searchWord = "";
        if (word.startsWith("-")) {
            word.replace("-", "");
        }
        switch (normalizer) {
            case "MyStem":
                try {
                    searchWord = MyStem.stem(word);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "Porter":
                searchWord = Porter.stem(word);
                break;
            default:
                return null;
        }
        return searchWord;

    }

    private Set<Integer> jsonArrayToSet(JSONArray jsonArray) {
        Set<Integer> set = new TreeSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            set.add((int) (long) jsonArray.get(i));
        }
        return set;
    }

    /**
     * Метод возвращающий id документов, в которых нет искомого слова
     *
     * @param wordDocs документы, в которых слово есть
     * @param allDocs  все документы
     * @return
     */
    private Set<Integer> getInversion(Set<Integer> wordDocs, Set<Integer> allDocs) {
        Set<Integer> res = new TreeSet<>();
        for (Integer in : allDocs) {
            if (!wordDocs.contains(in))
                res.add(in);
        }
        return res;
    }

    /**
     * Пересечение двух множеств
     *
     * @return пересечение
     */
    private Set<Integer> getIntersection(Set<Integer> firstSet, Set<Integer> secondSet) {
        if (firstSet == null || secondSet == null)
            return null;

        firstSet.retainAll(secondSet);
        return firstSet;
    }

    private Set<Integer> union(Set<Integer> firstSet, Set<Integer> secondSet) {
        firstSet.addAll(secondSet);
        return firstSet;
    }


    public static void main(String[] args) throws IOException, ParseException {
        IntersectionSearch intersectionSearch = new IntersectionSearch();
        intersectionSearch.search("задача уравнение -струя", "MyStem.json",
                "MyStem", "result.json");
    }

}
