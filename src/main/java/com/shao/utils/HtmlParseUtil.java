package com.shao.utils;

import com.shao.pojo.Content;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shaoruilin
 * @create 2021-02-25-21:53
 */
@Component
public class HtmlParseUtil {
//    public static void main(String[] args) throws Exception {
//        new HtmlParseUtil().parseJD("elasticsearch").forEach(System.out::println);
//    }

    public List<Content> parseJD(String keywords) throws Exception {
        // 获取请求 https://search.jd.com/Search?keyword=java
        // 前提，需要联网
        String url = "https://search.jd.com/Search?keyword=" + keywords;
        // 解析网页。（Jsoup返回Document就是浏览器Document对象）
        Document document = Jsoup.parse(new URL(url), 30000);
        // 所有在js中可以使用的方法，这里都能用
        Element element = document.getElementById("J_goodsList");
        //System.out.println(element.html());
        // 获取所有li元素
        List<Content> contentList = new ArrayList<>();
        Elements elements = element.getElementsByTag("li");
        for (Element e1 : elements) {
            String img = e1.getElementsByTag("img").eq(0).attr("data-lazy-img");
            String price = e1.getElementsByClass("p-price").eq(0).text();
            String title = e1.getElementsByClass("p-name").eq(0).text();
            contentList.add(new Content(img, price, title));
        }
        return contentList;
    }
}
