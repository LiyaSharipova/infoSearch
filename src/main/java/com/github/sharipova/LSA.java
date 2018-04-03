package com.github.sharipova;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.sharipova.IntersectionSearch.normalize;
import static com.github.sharipova.TfIdf.calculateIdf;
import static com.github.sharipova.TfIdf.calculateTf;
import static com.github.sharipova.TfIdf.getKey;

/**
 * Латентно-семантический анализ
 */
public class LSA {

    /**
     *Поиск запроса с использованием LSA
     */
    public void searchWithLSA(String query, String indexFileName, String normalizer, String documentsFile) throws IOException, ParseException {
        JSONObject invertedIndex = InvertedIndex.getJsonFromFile(indexFileName);
        JSONArray terms = (JSONArray) invertedIndex.get("Terms");
        int documentsCount = IntersectionSearch.getAllDocumentIds(documentsFile).size();
        //матрица tf_idf
        Matrix tfIdfMatrix = Matrix.constructWithCopy(getTfIdfMatrix(terms, documentsCount));
        printMatrix(tfIdfMatrix, "Матрица A");
        //матрица запроса
        Matrix queryMatrix = Matrix.constructWithCopy(getTfIdfQueryVector(terms, query, normalizer, documentsCount));
        printMatrix(queryMatrix, "Query");
        //сингулярное разложение
        SingularValueDecomposition s = tfIdfMatrix.svd();

        Matrix U = s.getU();
        Matrix S = s.getS();
        Matrix V = s.getV();
        printMatrix(U, "Матрица U");
        printMatrix(S, "Матрица S");
        printMatrix(V, "Матрица V");

        //приближение матриц с рангом = 2
        Integer rank = 2;
        Matrix Sk = S.getMatrix(0, rank - 1, 0, rank - 1);
        Matrix Vk = V.getMatrix(0, documentsCount - 1, 0, rank - 1);
        Matrix Uk = U.getMatrix(0, terms.size() - 1, 0, rank - 1);
        printMatrix(Sk, "Матрица Sk, k = " + rank);
        printMatrix(Vk, "Матрица Vk, k =  " + rank);
        printMatrix(Uk, "Матрица Uk, k =  " + rank);

        //обратная матрица Sk
        Matrix inversedS = Sk.inverse();

        //вектор запроса: q = q^t * Uk * Sk^-1
        Matrix queryVector = queryMatrix.times(Uk).times(inversedS);
        printMatrix(queryVector, "Vector q");
        double[][] q = queryVector.getArray();
        double[][] docs = Vk.getArray();

        //расчитываем косинусную меру
        for (int i = 0; i < docs.length; i++) {
            System.out.println("Sim (q, d" + i + ") = " + String.format("%.3f", sim(q, docs[i])));
        }

    }

    /**
     *sim(q, d) = скалярное произведение / модуль длины
     */
    public static Double sim(double[][] query, double[] doc) {

        double queryVectorLength = 0.0;
        double docVectorLength = 0.0;

        double scalar = 0.0;

        for (int i = 0; i < doc.length; i++) {
            scalar += query[0][i] * doc[i];
            queryVectorLength += Math.pow(query[0][i], 2);
            docVectorLength += Math.pow(doc[i], 2);
        }
        if (docVectorLength != 0) {
            return scalar / ((Math.sqrt(queryVectorLength)) * (Math.sqrt(docVectorLength)));
        }
        return 0.0;


    }

    /**
     *Матрица tf_idf для запроса
     */
    private double[][] getTfIdfQueryVector(JSONArray terms, String query, String normalizer, int documentsCount) {

        String[] words = query.toLowerCase().split("\\s+");
        double[][] queryMatrix = new double[1][terms.size()];
        AtomicInteger i = new AtomicInteger(0);
        Arrays.stream(words).forEach(word -> {
            word = normalize(word, normalizer);
            Double tf = calculateTf(occuranceInQuery(word, words, normalizer), words.length);
            int df = 0;
            int id = 0;
            for (int j = 0; j < terms.size(); j++) {
                JSONObject term = (JSONObject) terms.get(j);
                if (term.get("Value").equals(word)) {
                    df = (int) (long) term.get("Count");
                    id = (int) (long) term.get("Id");
                }
            }
            Double idf = 0.0;
            if (df != 0) {
                idf = calculateIdf(documentsCount, df);
            }
            Double tfIdf = tf * idf;
            queryMatrix[0][id] = tfIdf;
            i.getAndIncrement();
        });
        return queryMatrix;
    }

    /**
     *Матрица tf_idf для документов
     */
    private double[][] getTfIdfMatrix(JSONArray terms, int documentsCount) throws IOException, ParseException {
        Map<Double, Integer> docsWithScore = new TreeMap<>(Collections.reverseOrder());
        double[][] tfIdfMatrix = new double[terms.size()][documentsCount];
        for (int i = 0; i < terms.size(); i++) {
            JSONObject term = (JSONObject) terms.get(i);
            double tfIdf;
            JSONArray documentsTitle = (JSONArray) term.get("DocumentsTitle");
            JSONArray documentsAbstract = (JSONArray) term.get("DocumentsAbstract");
            for (int j = 0; j < documentsTitle.size(); j++) {
                JSONObject documentTitle = (JSONObject) documentsTitle.get(j);
                int id = (int) (long) documentTitle.get("Id");
                tfIdf = Double.parseDouble((String) documentTitle.get("Score"));
                docsWithScore.put(tfIdf, id);
            }
            for (int j = 0; j < documentsAbstract.size(); j++) {
                JSONObject documentAbstract = (JSONObject) documentsAbstract.get(j);
                int id = (int) (long) documentAbstract.get("Id");
                if (docsWithScore.values().contains(id)) {
                    Double tfIdfTitle = getKey(docsWithScore, id);
                    tfIdf = tfIdfTitle + Double.parseDouble((String) documentAbstract.get("Score"));
                    docsWithScore.remove(tfIdfTitle);
                    docsWithScore.put(tfIdf, id);
                } else {
                    tfIdf = Double.parseDouble((String) documentAbstract.get("Score"));
                    docsWithScore.put(tfIdf, id);
                }
            }
            double[] tfIdsForTerm = new double[documentsCount];
            for (int j = 0; j < documentsCount; j++) {
                Double tfIdfForJ = getKey(docsWithScore, j);
                if (tfIdfForJ != 0) {
                    tfIdsForTerm[j] = tfIdfForJ;
                }
            }
            tfIdfMatrix[i] = tfIdsForTerm;
        }
        return tfIdfMatrix;

    }

    /**
     *Вывод матрицы
     */
    public static void printMatrix(Matrix m, String matrixName) {

        double[][] array = m.getArray();
        System.out.println(matrixName);
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                System.out.print(String.format("%.3f", array[i][j]) + " ");
            }
            System.out.println();
        }
    }

    /**
     *Количество вхождения слова  в запрос
     */
    private int occuranceInQuery(String word, String[] words, String normalizer) {
        int occurance = 0;
        for (String s : words) {
            s = normalize(s, normalizer);
            if (s.equals(word)) {
                occurance++;
            }
        }
        return occurance;
    }


    public static void main(String[] args) throws IOException, ParseException {
        LSA lsa = new LSA();
        lsa.searchWithLSA("задача уравнение", "PorterWithScore.json", "Porter", "result.json");
    }


}
