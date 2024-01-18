package com.g7.framework.javadoc.vo;

import java.util.ArrayList;
import java.util.List;

public class MethodPageVO {

    private String tables;
    private String special;
    private String signature;
    private String signatureAbbrev;
    private String author;
    private String description;
    private String serviceName;
    private String since;
    private List<MethodChangeHistory> changeList = new ArrayList<>();
    private List<ParamClass> paramClassList = new ArrayList<>();
    private List<ParamClass> returnClassList = new ArrayList<>();
    private String sample;
    private String paramTag;
    /**
     * 如：Request<String> waybillId 的 waybillId
     */
    private String paramName;
    /**
     * 如：Request<ShuntingReq>
     */
    private String paramType;
    private String returnTag;
    /**
     * 如：Result<String>
     */
    private String returnType;
    /**
     * 前端请求地址 如：waybill-app.WaybillDetailService.detail
     */
    private String uri;

    /**
     * 接口是否废弃 true已废弃 false未废弃
     */
    private String deprecated;

    public String getParamTag() {
        return paramTag;
    }

    public void setParamTag(String paramTag) {
        this.paramTag = paramTag;
    }

    public String getReturnTag() {
        return returnTag;
    }

    public void setReturnTag(String returnTag) {
        this.returnTag = returnTag;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignatureAbbrev() {
        return signatureAbbrev;
    }

    public void setSignatureAbbrev(String signatureAbbrev) {
        this.signatureAbbrev = signatureAbbrev;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public List<MethodChangeHistory> getChangeList() {
        return changeList;
    }

    public void setChangeList(List<MethodChangeHistory> changeList) {
        this.changeList = changeList;
    }

    public List<ParamClass> getParamClassList() {
        return paramClassList;
    }

    public void setParamClassList(List<ParamClass> paramClassList) {
        this.paramClassList = paramClassList;
    }

    public List<ParamClass> getReturnClassList() {
        return returnClassList;
    }

    public void setReturnClassList(List<ParamClass> returnClassList) {
        this.returnClassList = returnClassList;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public String getTables() {
        return tables;
    }

    public void setTables(String tables) {
        this.tables = tables;
    }

    public String getSpecial() {
        return special;
    }

    public void setSpecial(String special) {
        this.special = special;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(String deprecated) {
        this.deprecated = deprecated;
    }
}
