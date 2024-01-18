package com.g7.framework.javadoc.confluence;

public class PreparedMethodPage {
    private String title;
    private String content;
    private String serviceName;
    private String methodName;
    private boolean pageAlreadyExist = false;// 表示该title不存在，需要插入，true表示存在需要更新
    private ConfluencePageInfo existedPageInfo;// 按title查找在Confluence里的页面数据
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

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
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

    public ConfluencePageInfo getParentPage() {
        return parentPage;
    }

    public void setParentPage(ConfluencePageInfo parentPage) {
        this.parentPage = parentPage;
    }
}
