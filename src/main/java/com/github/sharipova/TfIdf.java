package com.github.sharipova;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class TfIdf {
    private static final double K_TITLE = 0.6;
    private static final double K_ABSTRACT = 0.4;

    /**
     * Поиск документов, в которых встречаются все слова из запроса. Результат отсортирован по значению tf_idf(score)
     */
    public Map<Double, Integer> search(String query, String normalizer, String documentsFile) throws IOException, ParseException {
        String[] words = query.toLowerCase().split("\\s+");
        String indexFileName = normalizer + "WithScore" + ".json";
        IntersectionSearch intersectionSearch = new IntersectionSearch();
        Set<Integer> resultDocIds = intersectionSearch.search(query, indexFileName, normalizer, documentsFile);
        Map<Double, Integer> docsWithScore = new TreeMap<>(Collections.reverseOrder());
        JSONObject invertedIndex = InvertedIndex.getJsonFromFile(indexFileName);
        JSONArray terms = (JSONArray) invertedIndex.get("Terms");
        //считываем score для документов из результа поиска
        Arrays.stream(words).forEach(word -> {
            word = intersectionSearch.normalize(word, normalizer);
            for (int i = 0; i < terms.size(); i++) {
                Double tfIdf;
                JSONObject term = (JSONObject) terms.get(i);
                if (term.get("Value").equals(word)) {
                    JSONArray documentsTitle = (JSONArray) term.get("DocumentsTitle");
                    for (int j = 0; j < documentsTitle.size(); j++) {
                        JSONObject documentTitle = (JSONObject) documentsTitle.get(j);
                        int id = (int) (long) documentTitle.get("Id");
                        if (resultDocIds.contains(id)) {
                            tfIdf = Double.parseDouble((String) documentTitle.get("Score"));
                            docsWithScore.put(tfIdf, id);
                        }
                    }
                    //если слово находится и в заголовке, и в аннотации, то складываем score заголовка и аннотации
                    JSONArray documentsAbstract = (JSONArray) term.get("DocumentsAbstract");
                    for (int j = 0; j < documentsAbstract.size(); j++) {
                        JSONObject documentAbstract = (JSONObject) documentsAbstract.get(j);
                        int id = (int) (long) documentAbstract.get("Id");
                        if (resultDocIds.contains(id) && docsWithScore.values().contains(id)) {
                            Double tfIdfTitle = getKey(docsWithScore, id);
                            tfIdf = tfIdfTitle + Double.parseDouble((String) documentAbstract.get("Score"));
                            docsWithScore.remove(tfIdfTitle);
                            docsWithScore.put(tfIdf, id);
                        } else {
                            tfIdf = Double.parseDouble((String) documentAbstract.get("Score"));
                            docsWithScore.put(tfIdf, id);
                        }
                    }
                }
            }
        });

        return docsWithScore;

    }

    /**
     * расчитывает tf_idf и составляет новый индекс файл
     */
    public void calculateTfIdfAndPrint(String indexFileName, String documentsFile, String normalizer) throws IOException, ParseException {
        JSONObject invertedIndex = InvertedIndex.getJsonFromFile(indexFileName);
        JSONArray terms = (JSONArray) invertedIndex.get("Terms");
        for (int i = 0; i < terms.size(); i++) {
            JSONObject term = (JSONObject) terms.get(i);
            JSONArray documentsTitle = calculateTfIdfForWord(term,
                    "DocumentsTitle", documentsFile);
            JSONArray documentsAbstract = calculateTfIdfForWord(term,
                    "DocumentsAbstract", documentsFile);
            term.replace("DocumentsTitle", documentsTitle);
            term.replace("DocumentsAbstract", documentsAbstract);

        }
        JSONObject index = new JSONObject();
        index.put("Terms", terms);
        printToFile(normalizer, index);
    }

    /**
     * tf_idf = k * tf * idf
     *
     * @return документы с проставленным score
     */
    private JSONArray calculateTfIdfForWord(JSONObject term, String key, String documentsFile) throws IOException, ParseException {
        JSONArray documents = (JSONArray) term.get(key);
        int N = IntersectionSearch.getAllDocumentIds(documentsFile).size();
        if (documents != null) {

            for (int i = 0; i < documents.size(); i++) {
                JSONObject document = (JSONObject) documents.get(i);
                Double tf = calculateTf((long) document.get("WordCount"), (long) document.get("TotalWordsCount"));
                int df = documents.size();
                Double idf = calculateIdf(N, df);

                DecimalFormat df3 = new DecimalFormat("0.###");

                switch (key) {
                    case "DocumentsTitle":
                        document.put("Score", df3.format(K_TITLE * tf * idf));
                        break;
                    case "DocumentsAbstract":
                        document.put("Score", df3.format(K_ABSTRACT * tf * idf));
                        break;
                    default:
                        throw new IllegalArgumentException("No such key: " + key);
                }
            }
        }
        return documents;

    }

    /**
     * @return tf = сколько раз слово встречается в доке / общее кол-во слов в доке
     */
    public static Double calculateTf(long wordCount, long totalWordsCount) {
        return (double) wordCount / totalWordsCount;
    }

    /**
     * @return idf = log (N/df), общее кол-во документов / кол-во документов, в котором встречается слово
     */
    public static Double calculateIdf(int N, int df) {
        return Math.log((double) N / df) / Math.log(2.0);
    }

    public static Double getKey(Map<Double, Integer> map, Integer value) {
        double key = 0;
        for (Map.Entry<Double, Integer> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                key = entry.getKey();
            }
        }
        return key;
    }


    private void printToFile(String normalizer, JSONObject index) throws IOException {
        String indexFileName = normalizer + "WithScore" + ".json";
        try (FileWriter file = new FileWriter(indexFileName)) {
            file.write(index.toJSONString());
        }
    }

    private void printResult(String query, Map<Double, Integer> result) {
        System.out.println("Query: " + query);
        for (Map.Entry<Double, Integer> entry : result.entrySet()) {
            System.out.println("Document id = " + entry.getValue() + ", score = " + entry.getKey());
        }
    }

    public static void main(String[] args) {
        TfIdf tfIdf = new TfIdf();
        try {
            tfIdf.calculateTfIdfAndPrint("Porter.json", "result.json", "Porter");
            tfIdf.calculateTfIdfAndPrint("MyStem.json", "result.json", "MyStem");
            String query = "задача уравнение -струя";
            Map<Double, Integer> result = tfIdf.search(query, "MyStem", "result.json");
            tfIdf.printResult(query, result);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
