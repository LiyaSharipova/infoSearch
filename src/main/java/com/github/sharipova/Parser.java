package com.github.sharipova;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Parser {

    public final static String SITE = "http://www.mathnet.ru";

    public static void main(String[] args) throws IOException {
        JSONObject obj = new JSONObject();
        JSONArray articles = new JSONArray();
        JSONArray article = new JSONArray();
        JSONArray keyWords = new JSONArray();

        Document doc = Jsoup.connect("http://www.mathnet.ru/php/archive.phtml?jrnid=uzku&wshow=issue&bshow=contents&series=0&year=2016&volume=158%22%20\\%20%22&issue=1&option_lang=rus&bookID=1621").get();
        Elements links = doc.select("td[colspan] > a.SLink");

        for (Element link : links) {
            String url = SITE + link.attr("href");
            doc = (Document) Jsoup.connect(url).get();
            article.add("URL : " + url);
            article.add("Title : " + doc.select("span.red:has(font)").text());

//            /html/body/table[1]/tbody/tr/td/table[3]/tbody/tr/td[2]/text()[2]
            article.add("Abstract : " + doc.select("b:contains(Аннотации) ~ b:contains(Ключевые слова)"));
        }
    }
}
