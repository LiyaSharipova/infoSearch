package com.github.sharipova;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Реализация метода пересечения
 */
public class IntersectionSearch {

    public Set<Integer> search(String query, String indexFileName, String normalizer, String documentsFile) throws IOException, ParseException {
        Set<Integer> allDocIds = getAllDocumentIds(documentsFile);
//      read the invertedIndex file
        JSONArray terms = getJsonFromFile(indexFileName, "Terms");
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
                    JSONArray documentsTitle = (JSONArray) term.get("DocumentsTitle");
                    JSONArray documentsAbstract = (JSONArray) term.get("DocumentsAbstract");
                    Set<Integer> docsTitle = jsonArrayToSet((JSONArray) documentsTitle);
                    Set<Integer> docsAbstract = jsonArrayToSet((JSONArray) documentsAbstract);
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
        for (int i = 1; i < wordsList.size() - 1; i++) {
            if (!(result.isEmpty()))
                result = getIntersection(result,
                        new TreeSet<>(wordWithDocsMap.get(wordsList.get(i))));
        }
        return result;

    }

    private void printResult(String query, Set<Integer> result) {
        System.out.println("Query: " + query);
        System.out.println("Result documents ids:");
        result.forEach(integer -> System.out.print(integer + " "));
        System.out.println("");
        System.out.println("Сount: " + result.size());
    }

    public static JSONArray getJsonFromFile(String fileName, String key) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(fileName));

        return (JSONArray) jsonObject.get(key);
    }

    public static Set<Integer> getAllDocumentIds(String fileName) throws IOException, ParseException {
        Set<Integer> allDocIds = new HashSet<>();
        JSONArray documents = getJsonFromFile(fileName, "Articles");
        for (int i = 1; i < documents.size() + 1; i++) {
            JSONObject document = (JSONObject) documents.get(i - 1);
            allDocIds.add(Integer.parseInt((String) document.get("Id")));
        }
        return allDocIds;
    }

    public static String normalize(String word, String normalizer) {
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
                throw new IllegalArgumentException("No such normalizer: " + normalizer);

        }
        return searchWord;

    }

    private Set<Integer> jsonArrayToSet(JSONArray jsonArray) {
        Set<Integer> set = new TreeSet<>();
        if (jsonArray != null){
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject document = (JSONObject) jsonArray.get(i);
                set.add((int) (long) document.get("Id"));
            }
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
        String query = "задача уравнение -струя";
        Set<Integer> result = intersectionSearch.search(query, "MyStem.json",
                "MyStem", "result.json");
        intersectionSearch.printResult(query, result);
    }

}
