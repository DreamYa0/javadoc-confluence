package com.g7.framework.javadoc.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.g7.framework.javadoc.confluence.ConfluencePageAncestor;
import com.g7.framework.javadoc.confluence.ConfluencePageBody;
import com.g7.framework.javadoc.confluence.ConfluencePageInfo;
import com.g7.framework.javadoc.confluence.ConfluencePageVersion;
import com.g7.framework.javadoc.confluence.ConfluenceSpace;
import com.g7.framework.javadoc.confluence.ConfluenceStorage;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ConfluenceUtils {
    private final static Logger logger = Logger.getLogger(ConfluenceUtils.class);

    // config
    private static String url = "";
    private static String spaceKey = "";
    private static String username = "";
    private static String password = "";
    private static Long parentId = 0l;
    private static boolean debug = false;

    public static ConfluencePageInfo getPageInfo(String title) {
        ConfluencePageInfo pageInfo = new ConfluencePageInfo();
        pageInfo.setTitle(title);
        String fullUrl = null;
        try {
            fullUrl = url + "/rest/api/content?title=" +
                    URLEncoder.encode(title, "utf-8") + "&spaceKey=" +
                    URLEncoder.encode(spaceKey, "utf-8") +
                    "&expand=history,version,ancestors,body.storage";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            if (debug)
                logger.info("url:" + fullUrl);
            String ret = HTTPUtils.get(fullUrl, username, password);
            if (debug)
                logger.info("ret:" + ret);
            JSONObject json = JSON.parseObject(ret);
            JSONArray o = (JSONArray) json.get("results");
            if (o != null && o.size() == 1) {
                JSONObject pageJson = (JSONObject) o.get(0);
                pageInfo = JSON.parseObject(pageJson.toJSONString(), ConfluencePageInfo.class);
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
        return pageInfo;
    }

    public static ConfluencePageInfo getPageInfoById(int id) {
        return getPageInfoById(id, null);
    }

    public static ConfluencePageInfo getPageInfoById(int id, List<String> expands) {
        ConfluencePageInfo pageInfo = new ConfluencePageInfo();
        String fullUrl = url + "/rest/api/content/" + id;
        if (expands != null && expands.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("&expand=");
            for (int i = 0; i < expands.size(); i++) {
                sb.append(expands.get(i));
                if (i != expands.size() - 1) {
                    sb.append(",");
                }
            }
            fullUrl += sb.toString();
        }

        try {
            if (debug)
                logger.info("url:" + fullUrl);
            String ret = HTTPUtils.get(fullUrl, username, password);
            if (debug)
                logger.info("ret:" + ret);

            JSONObject pageJson = JSON.parseObject(ret);
            pageInfo = JSON.parseObject(pageJson.toJSONString(), ConfluencePageInfo.class);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
        return pageInfo;
    }

    public static void updatePageContent(ConfluencePageInfo pageInfo, String title, String content) {
        if (pageInfo.getId() == null) {
            throw new RuntimeException("updatePageContent - pageInfo.getId() cannot be null!");
        }
        ConfluencePageInfo pageUpdt = new ConfluencePageInfo();
        pageUpdt.setId(pageInfo.getId());
        pageUpdt.setType("page");
        pageUpdt.setTitle(title);
        ConfluencePageAncestor ancestor = new ConfluencePageAncestor();
        long parentId = pageInfo.getAncestors().get(pageInfo.getAncestors().size() - 1).getId();

        ancestor.setId(parentId);
        List<ConfluencePageAncestor> ancestors = new ArrayList<ConfluencePageAncestor>();
        ancestors.add(ancestor);
        pageUpdt.setAncestors(ancestors);

        ConfluenceSpace space = new ConfluenceSpace();
        space.setKey(spaceKey);
        pageUpdt.setSpace(space);

        ConfluencePageBody body = new ConfluencePageBody();
        ConfluenceStorage storage = new ConfluenceStorage();
        storage.setRepresentation("storage");
        storage.setValue(content);
        body.setStorage(storage);
        pageUpdt.setBody(body);

        ConfluencePageVersion version = new ConfluencePageVersion();
        version.setNumber(pageInfo.getVersion().getNumber() + 1);
        pageUpdt.setVersion(version);

        if (debug)
            logger.info("update page:" + JSON.toJSONString(pageUpdt));
        String fullUrl = url + "/rest/api/content/" + pageInfo.getId();

        if (debug)
            logger.info("update url:" + fullUrl);
        try {
            HTTPUtils.put(fullUrl, JSON.toJSONString(pageUpdt), username, password);
        } catch (Exception e) {
            logger.error("Update page content failed message is " + e.getMessage());
        }
    }

    public static ConfluencePageInfo createPage(String title, long parentId, String content) {
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(content)) {
            throw new RuntimeException("createPage - title and content cannot be empty!");
        }

        ConfluencePageInfo pageCreate = new ConfluencePageInfo();

        List<ConfluencePageAncestor> ancestors = new ArrayList<ConfluencePageAncestor>();
        ConfluencePageAncestor ancestor = new ConfluencePageAncestor();
        ancestor.setId(parentId);
        ancestors.add(ancestor);
        pageCreate.setAncestors(ancestors);

        pageCreate.setType("page");
        pageCreate.setTitle(title);

        ConfluenceSpace space = new ConfluenceSpace();
        space.setKey(spaceKey);
        pageCreate.setSpace(space);

        ConfluencePageBody body = new ConfluencePageBody();
        ConfluenceStorage storage = new ConfluenceStorage();
        storage.setRepresentation("storage");
        storage.setValue(content);
        body.setStorage(storage);
        pageCreate.setBody(body);

        String fullUrl = url + "/rest/api/content/";

        if (debug) {
            logger.info("create page:" + JSON.toJSONString(pageCreate));
            logger.info("create url:" + fullUrl);
        }
        try {
            String ret = HTTPUtils.post(fullUrl, JSON.toJSONString(pageCreate), username, password);
            return JSON.parseObject(ret, ConfluencePageInfo.class);
        } catch (Exception e) {
            logger.error("Create page failed message is " + e.getMessage());
            throw new RuntimeException("Create page failed", e);
        }
    }

    public static void setUrl(String url) {
        ConfluenceUtils.url = url;
    }

    public static void setSpaceKey(String spaceKey) {
        ConfluenceUtils.spaceKey = spaceKey;
    }

    public static void setUsername(String username) {
        ConfluenceUtils.username = username;
    }

    public static void setPassword(String password) {
        ConfluenceUtils.password = password;
    }

    public static void setParentId(Long parentId) {
        ConfluenceUtils.parentId = parentId;
    }

    public static void setDebug(boolean debug) {
        ConfluenceUtils.debug = debug;
    }
}
