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

public class Parser {

    public final static String SITE = "http://www.mathnet.ru";
    public final static String HOME = "http://www.mathnet.ru/php/archive.phtml?jrnid=uzku&wshow=issue&bshow=contents&series=0&year=2016&volume=158%22%20\\%20%22&issue=1&option_lang=rus&bookID=1621";

    public static void main(String[] args) throws IOException {
        JSONObject obj = new JSONObject();
        JSONArray articles = new JSONArray();
        JSONObject article = new JSONObject();

        Document doc = Jsoup.connect(HOME).get();
        Elements links = doc.select("td[colspan] > a.SLink");

        for (Element link : links) {
            String url = SITE + link.attr("href");
            doc = (Document) Jsoup.connect(url).get();
            Pattern pattern = Pattern.compile("<b>Аннотация:<\\/b>(.|\\s)*?<br>");
            Matcher matcher = pattern.matcher(doc.body().toString());
            String annotation = "";
            if (matcher.find()) {
                annotation = matcher.group(0).replace("<b>Аннотация:</b>", "").replace("<br>", "");
            }


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
