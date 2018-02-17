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

    public static void main(String[] args) throws IOException {
        JSONObject obj = new JSONObject();
        JSONArray articles = new JSONArray();
        JSONObject article = new JSONObject();

        Document doc = Jsoup.connect("http://www.mathnet.ru/php/archive.phtml?jrnid=uzku&wshow=issue&bshow=contents&series=0&year=2016&volume=158%22%20\\%20%22&issue=1&option_lang=rus&bookID=1621").get();
        Elements links = doc.select("td[colspan] > a.SLink");

        for (Element link : links) {
            String url = SITE + link.attr("href");
            doc = (Document) Jsoup.connect(url).get();
            article.put("URL", url);
            article.put("Title", doc.select("span.red:has(font)").text());
            Pattern pattern = Pattern.compile("<b>Аннотация:<\\/b>(.|\\s)*?<br>");
            Matcher matcher = pattern.matcher(doc.body().toString());
            String group = matcher.group(1);

            boolean matches = doc.body().toString().matches("<b>Аннотация:<\\/b>(.|\\s)*?<br>");

//            /html/body/table[1]/tbody/tr/td/table[3]/tbody/tr/td[2]/text()[2]
            Elements elements = doc.select("b:contains(Аннотация:)");
            String s = elements.first().nextSibling().toString();
            article.put("Abstract" , doc.select("body:matches(<b>Аннотация:<\\/b>(.|\\s)*?<br>)"));

            article.put("Keywords", doc.select("b + i").text());
            articles.add(article);
        }
//        try (FileWriter file = new FileWriter("result.txt")) {
//            file.write(obj.toJSONString());
//        }
    }
}
