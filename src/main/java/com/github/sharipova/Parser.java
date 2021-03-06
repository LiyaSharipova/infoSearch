package com.github.sharipova;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {

    public final static String SITE = "http://www.mathnet.ru";
    public final static String HOME = "http://www.mathnet.ru/php/archive.phtml?jrnid=uzku&wshow=issue&bshow=contents&series=0&year=2016&volume=158%22%20\\%20%22&issue=1&option_lang=rus&bookID=1621";

    public static String normalizeWithPorter(String original) {
        return Stream.of(original.replaceAll("[^А-Яа-я\\s]", "")
                .split("\\s+")).map(r -> Porter.stem(r)).collect(Collectors.joining(" "));
    }

    public static String normalizeWithMyStem(String original) {
        return Stream.of(original.replaceAll("[^А-Яа-я\\s]", "")
                .split("\\s+")).map(r -> {
            try {
                return MyStem.stem(r);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.joining(" "));
    }

    public static void createInvertedIndex(JSONArray articles) {

    }


    public static void main(String[] args) throws IOException {
        JSONObject obj = new JSONObject();
        JSONArray articles = new JSONArray();
        JSONObject article;
        JSONObject annotation;
        JSONObject title;

        Document doc = Jsoup.connect(HOME).get();
        Elements links = doc.select("td[colspan] > a.SLink");

        int i = 0;
        for (Element link : links) {
            article = new JSONObject();
            title = new JSONObject();
            String url = SITE + link.attr("href");
            doc = (Document) Jsoup.connect(url).get();
            Pattern pattern = Pattern.compile("<b>Аннотация:<\\/b>((.|\\s)*?)<br>");
            Matcher matcher = pattern.matcher(doc.body().toString());
            String originalAnnotation = "";
            if (matcher.find()) {
                originalAnnotation = Jsoup.parse(matcher.group(1)).text();
            }
            annotation = new JSONObject();
            annotation.put("Original", originalAnnotation);
            annotation.put("Porter", normalizeWithPorter(originalAnnotation));

            annotation.put("MyStem", normalizeWithMyStem(originalAnnotation));
            String originalTitle = doc.select("span.red:has(font)").text();
            title.put("Original", originalTitle);
            title.put("Porter", normalizeWithPorter(originalTitle));
            title.put("MyStem", normalizeWithMyStem(originalTitle));

            article.put("Id", i);
            article.put("Title", title);
            article.put("URL", url);
            article.put("Abstract", annotation);
            article.put("Keywords", doc.select("b + i").text());
            article.put("WordsCount", originalAnnotation.replaceAll("[^А-Яа-я\\s]", "")
                    .split("\\s+").length + originalTitle.replaceAll("[^А-Яа-я\\s]", "")
                    .split("\\s+").length);
            articles.add(article);
            i++;
        }
        obj.put("Articles", articles);
        try (FileWriter file = new FileWriter("result1.json")) {
            file.write(obj.toJSONString());
        }
    }
}
