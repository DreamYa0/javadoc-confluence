package com.g7.framework.javadoc.confluence;

import java.util.Date;

public class ConfluencePageHistory {
    private Date createdDate;
    private ConfluenceUser createdBy;

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public ConfluenceUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(ConfluenceUser createdBy) {
        this.createdBy = createdBy;
    }
}
