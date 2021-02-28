package com.shao.service;

import com.alibaba.fastjson.JSON;
import com.shao.pojo.Content;
import com.shao.utils.HtmlParseUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author shaoruilin
 * @create 2021-02-25-22:34
 */
@Service
public class ContentService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private HtmlParseUtil htmlParseUtil;

    // 解析数据放入es索引中
    public Boolean parseContent(String keyword) throws Exception {
        List<Content> contents = htmlParseUtil.parseJD(keyword);
        // 把查询到的数据放入es中,批量请求
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("2m");

        for (Content content : contents) {
            bulkRequest.add(
                    new IndexRequest("jd_goods")
                            .source(JSON.toJSONString(content), XContentType.JSON));
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return !bulk.hasFailures();
    }

//    public List<Map<String, Object>> searchPage(String keyword, int pageNo, int pageSize) throws IOException {
//        if (pageNo < 1) {
//            pageNo = 1;
//        }
//
//        // 条件搜索
//        SearchRequest searchRequest = new SearchRequest("jd_goods");
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//
//        // 分页
//        searchSourceBuilder.from(pageNo);
//        searchSourceBuilder.size(pageSize);
//
//        // 精准匹配
//        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", keyword);
//        searchSourceBuilder.query(termQueryBuilder);
//        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
//
//        // 执行搜索
//        searchRequest.source(searchSourceBuilder);
//        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
//
//        // 解析结果
//        ArrayList<Map<String, Object>> list = new ArrayList<>();
//        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
//            list.add(documentFields.getSourceAsMap());
//        }
//        return list;
//    }

    //3. 获取这些数据实现搜索高亮功能
    public List<Map<String, Object>> searchPageHighlightBuilder(String keyword, int pageNo, int pageSize) throws IOException {
        if (pageNo < 1) {
            pageNo = 1;
        }

        // 条件搜索
        SearchRequest searchRequest = new SearchRequest("jd_goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 分页
        searchSourceBuilder.from(pageNo);
        searchSourceBuilder.size(pageSize);

        // 精准匹配
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", keyword);
        searchSourceBuilder.query(termQueryBuilder);
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        // 高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.requireFieldMatch(false); // 多个高亮显示：关闭
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);


        // 执行搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // 解析结果
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField title = highlightFields.get("title");
            Map<String, Object> sourceAsMap = hit.getSourceAsMap(); // 原来的结果
            if (title != null) {
                Text[] fragments = title.fragments();
                String newTitle = "";
                for (Text text : fragments) {
                    newTitle += text;
                }
                sourceAsMap.put("title", newTitle);
            }
            list.add(sourceAsMap);
        }
        return list;
    }
}
