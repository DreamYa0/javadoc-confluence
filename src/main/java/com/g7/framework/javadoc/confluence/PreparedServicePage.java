package com.g7.framework.javadoc.confluence;

import java.util.List;

public class PreparedServicePage {
    private String title;
    private String content;
    private String serviceName;
    private boolean pageAlreadyExist = false;// 表示该title不存在，需要插入，true表示存在需要更新
    private ConfluencePageInfo existedPageInfo;// 按title查找在Confluence里的页面数据
    private List<PreparedMethodPage> subPages;//
    private ConfluencePageInfo parentPage;// 其实只需要id

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isPageAlreadyExist() {
        return pageAlreadyExist;
    }

    public void setPageAlreadyExist(boolean pageAlreadyExist) {
        this.pageAlreadyExist = pageAlreadyExist;
    }

    public ConfluencePageInfo getExistedPageInfo() {
        return existedPageInfo;
    }

    public void setExistedPageInfo(ConfluencePageInfo existedPageInfo) {
        this.existedPageInfo = existedPageInfo;
    }

    public List<PreparedMethodPage> getSubPages() {
        return subPages;
    }

    public void setSubPages(List<PreparedMethodPage> subPages) {
        this.subPages = subPages;
    }

    public ConfluencePageInfo getParentPage() {
        return parentPage;
    }

    public void setParentPage(ConfluencePageInfo parentPage) {
        this.parentPage = parentPage;
    }
}
