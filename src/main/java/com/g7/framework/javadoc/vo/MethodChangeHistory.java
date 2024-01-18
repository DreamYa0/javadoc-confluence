package com.g7.framework.javadoc.vo;

public class MethodChangeHistory {
    /**
     * 修改时间
     */
    private String time;
    /**
     * 修改内容
     */
    private String content;
    /**
     * 修改者
     */
    private String author;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
