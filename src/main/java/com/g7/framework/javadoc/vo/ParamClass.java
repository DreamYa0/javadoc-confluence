package com.g7.framework.javadoc.vo;

import java.util.List;

public class ParamClass {

    private String qualified;
    private String simpleName;
    private List<FieldInfoVO> fields;

    public String getQualified() {
        return qualified;
    }

    public void setQualified(String qualified) {
        this.qualified = qualified;
    }

    public List<FieldInfoVO> getFields() {
        return fields;
    }

    public void setFields(List<FieldInfoVO> fields) {
        this.fields = fields;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }
}
