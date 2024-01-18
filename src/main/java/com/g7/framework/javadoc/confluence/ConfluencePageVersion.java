package com.g7.framework.javadoc.confluence;

import java.util.Date;

public class ConfluencePageVersion {
    private ConfluenceUser by;
    private Date when;
    private Integer number;
    private Boolean minorEdit;

    public ConfluenceUser getBy() {
        return by;
    }

    public void setBy(ConfluenceUser by) {
        this.by = by;
    }

    public Date getWhen() {
        return when;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Boolean isMinorEdit() {
        return minorEdit;
    }

    public void setMinorEdit(Boolean minorEdit) {
        this.minorEdit = minorEdit;
    }
}
