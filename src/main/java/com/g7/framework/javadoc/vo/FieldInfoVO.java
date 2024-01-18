package com.g7.framework.javadoc.vo;

public class FieldInfoVO {
    /**
     * 字段名称
     */
    private String name;
    /**
     * 字段类型
     */
    private String type;
    /**
     * 可否为空
     */
    private String canBeEmpty;
    /**
     * 字段说明
     */
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCanBeEmpty() {
        return canBeEmpty;
    }

    public void setCanBeEmpty(String canBeEmpty) {
        this.canBeEmpty = canBeEmpty;
    }
}
