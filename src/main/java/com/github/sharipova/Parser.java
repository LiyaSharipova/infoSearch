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

    public static void main(String[] args) throws IOException {
        JSONObject obj = new JSONObject();
        JSONArray articles = new JSONArray();
        JSONObject article;
        JSONArray annotations = new JSONArray();
        JSONObject annotation;

        Document doc = Jsoup.connect(HOME).get();
        Elements links = doc.select("td[colspan] > a.SLink");

        for (Element link : links) {
            article = new JSONObject();
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
            annotation.put("Porter", Stream.of(originalAnnotation.replaceAll("[^А-Яа-я\\s]", "")
                    .split("\\s+")).map(r -> Porter.stem(r)).collect(Collectors.joining(" ")));

            annotation.put("MyStem", Stream.of(originalAnnotation.replaceAll("[^А-Яа-я\\s]", "")
                    .split("\\s+")).map(r -> {
                try {
                    return MyStem.stem(r);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.joining(" ")));

            article.put("Keywords", doc.select("b + i").text());
            article.put("Abstract", annotation);
            article.put("URL", url);
            article.put("Title", doc.select("span.red:has(font)").text());
            articles.add(article);
        }
        obj.put("Articles", articles);
        try (FileWriter file = new FileWriter("result.json")) {
            file.write(obj.toJSONString());
        }
    }
}
