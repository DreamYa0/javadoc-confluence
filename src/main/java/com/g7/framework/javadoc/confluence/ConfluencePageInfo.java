package com.g7.framework.javadoc.confluence;

import java.util.List;

public class ConfluencePageInfo {
    private Long id;
    private String type;
    private String title;
    private ConfluencePageHistory history;
    private ConfluencePageVersion version;
    private List<ConfluencePageAncestor> ancestors;
    private ConfluenceSpace space;
    private ConfluencePageBody body;
    private ConfluenceContainer container;

    public ConfluencePageAncestor getParentPageAncestor() {
        if (ancestors == null || ancestors.size() == 0) {
            return null;
        }
        return ancestors.get(ancestors.size() - 1);
    }

    public ConfluenceSpace getSpace() {
        return space;
    }

    public void setSpace(ConfluenceSpace space) {
        this.space = space;
    }

    public ConfluencePageBody getBody() {
        return body;
    }

    public void setBody(ConfluencePageBody body) {
        this.body = body;
    }

    public ConfluenceContainer getContainer() {
        return container;
    }

    public void setContainer(ConfluenceContainer container) {
        this.container = container;
    }

    public List<ConfluencePageAncestor> getAncestors() {
        return ancestors;
    }

    public void setAncestors(List<ConfluencePageAncestor> ancestors) {
        this.ancestors = ancestors;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ConfluencePageHistory getHistory() {
        return history;
    }

    public void setHistory(ConfluencePageHistory history) {
        this.history = history;
    }

    public ConfluencePageVersion getVersion() {
        return version;
    }

    public void setVersion(ConfluencePageVersion version) {
        this.version = version;
    }
}
