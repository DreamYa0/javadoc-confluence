package com.g7.framework.javadoc;

import com.g7.framework.javadoc.confluence.ConfluencePageInfo;
import com.g7.framework.javadoc.confluence.PreparedMethodPage;
import com.g7.framework.javadoc.confluence.PreparedServicePage;
import com.g7.framework.javadoc.util.ConfluenceUtils;
import com.g7.framework.javadoc.util.HtmlUtils;
import com.g7.framework.javadoc.vo.FieldInfoVO;
import com.g7.framework.javadoc.vo.MethodPageVO;
import com.g7.framework.javadoc.vo.ParamClass;
import com.github.markusbernhardt.xmldoclet.xjc.AnnotationArgument;
import com.github.markusbernhardt.xmldoclet.xjc.AnnotationInstance;
import com.github.markusbernhardt.xmldoclet.xjc.Class;
import com.github.markusbernhardt.xmldoclet.xjc.Enum;
import com.github.markusbernhardt.xmldoclet.xjc.Field;
import com.github.markusbernhardt.xmldoclet.xjc.Interface;
import com.github.markusbernhardt.xmldoclet.xjc.Method;
import com.github.markusbernhardt.xmldoclet.xjc.MethodParameter;
import com.github.markusbernhardt.xmldoclet.xjc.Package;
import com.github.markusbernhardt.xmldoclet.xjc.Root;
import com.github.markusbernhardt.xmldoclet.xjc.TagInfo;
import com.github.markusbernhardt.xmldoclet.xjc.TypeInfo;
import com.google.common.base.Joiner;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.HTMLWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfluenceExecutor {

    private final static Logger logger = Logger.getLogger(ConfluenceExecutor.class);
    private final static String VM_FILENAME_SERVICE = "servicePage.vm";
    private final static String VM_FILENAME_METHOD = "methodPage.vm";
    private final static int MAX_TITLE_LEN = 100;
    private static List<String> checkErrPages = new ArrayList<>();
    /**
     * service页面的父页面
     */
    private ConfluencePageInfo parentPageInfo;
    private String parentPageTitleInPom;
    private String rootPath;
    private Template servicePageTem;
    private Template methodPageTem;
    private boolean debug = false;
    /**
     * 仅需检查的Service 逗号分隔
     */
    private String services;
    /**
     * 仅需检查的方法名 逗号分隔
     */
    private String methods;
    private List<String> createdPages = new ArrayList<>();
    private List<String> updatedPages = new ArrayList<>();

    public ConfluenceExecutor(String rootPath) {
        this.rootPath = rootPath;
    }

    public static void main(String[] args) {

        VelocityEngine ve = new VelocityEngine();
        //可选值："class"--从classpath中读取，"file"--从文件系统中读取
        ve.setProperty("resource.loader", "class");
        //如果从文件系统中读取模板，那么属性值为org.apache.velocity.runtime.resource.loader.FileResourceLoader
        ve.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        ve.init();


        ve.getTemplate(VM_FILENAME_SERVICE, "utf-8");
        ve.getTemplate(VM_FILENAME_METHOD, "utf-8");
    }

    public void execute() {

        long start = System.currentTimeMillis();
        File rootFile = new File(rootPath);
        if (!rootFile.isDirectory()) {
            throw new RuntimeException("API工程路径错误：" + rootPath);
        }

        // 读取 pom 拿到 parentPage的title
        readPom();

        // 初始化 Velocity
        initVelocity();

        // 得到 parentId
        long parentPageId = getParentPageId();

        // 读取 javadoc.xml 解析
        List<PreparedServicePage> servicePages = parseJavadoc();
        if (CollectionUtils.isEmpty(servicePages)) {
            logger.info("此工程没有接口，跳过接口文档生成！");
            return;
        }

        // 过滤不需要生成接口文档的Method
        filterMethod(servicePages);

        // 检查在Confluence里是否已存在page，拼装数据 判断已有页面的情况
        checkExitedConfluencePage(servicePages);

        // 插入或更新页面
        updatePages(servicePages);

        // 打印统计
        logger.info("本次创建页面" + createdPages.size() + "个：" + createdPages);
        logger.info("本次更新页面" + updatedPages.size() + "个：" + updatedPages);
        if (checkErrPages.size() > 0) {
            logger.warn("*** 以下页面在比对内容时发生异常，导致这些页面被强制更新。通常是因为 a)手工修改过页面 b)页面中包含乱码. "
                    + " 如果是由于b) 请检查你的注释中的乱码，并改掉，否则 即使内容没有修改也会强制更新页面！！！！"
                    + "\n    " + checkErrPages);
        }

        logger.info(String.format("更新页面完成 , 耗时: %d 秒", (System.currentTimeMillis() - start) / 1000));
    }

    private void filterService(List<PreparedServicePage> servicePages) {

        if (!services.trim().equalsIgnoreCase("all")) {

            String[] sstmp = services.trim().split(",");

            Iterator<PreparedServicePage> iterator = servicePages.iterator();
            while (iterator.hasNext()) {
                PreparedServicePage next = iterator.next();
                for (String name : sstmp) {
                    if (next.getServiceName().contains(name.trim())) {
                        // 过滤掉不需要生成接口文档的Service
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    private void filterMethod(List<PreparedServicePage> servicePages) {

        if (!methods.trim().equalsIgnoreCase("all")) {

            String[] sstmp = methods.trim().split(",");

            Iterator<PreparedServicePage> iterator = servicePages.iterator();
            while (iterator.hasNext()) {
                PreparedServicePage next = iterator.next();
                Iterator<PreparedMethodPage> subPageIterator = next.getSubPages().iterator();
                while (subPageIterator.hasNext()) {
                    PreparedMethodPage subPage = subPageIterator.next();
                    for (String name : sstmp) {
                        if (subPage.getMethodName().equalsIgnoreCase(name.trim())) {
                            // 过滤掉不需要生成接口文档的Method
                            subPageIterator.remove();
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 遍历这个2层树：
     * 若存在已有页面，且内容不相同，更新内容
     * 若不存在，插入页面（先插入service页面，method页面是其子页面）
     */
    private void updatePages(List<PreparedServicePage> servicePages) {
        long start = System.currentTimeMillis();
        for (PreparedServicePage servicePage : servicePages) {
            try {
                long servicePageId;
                if (servicePage.isPageAlreadyExist()) {
                    servicePageId = servicePage.getExistedPageInfo().getId();
                    updatePage(servicePage.getTitle(), servicePage.getExistedPageInfo(), servicePage.getContent());
                } else {
                    // create
                    ConfluencePageInfo pageInfo = ConfluenceUtils.createPage(servicePage.getTitle(), parentPageInfo.getId(), servicePage.getContent());
                    createdPages.add(servicePage.getTitle());
                    servicePageId = pageInfo.getId();
                }
                for (PreparedMethodPage methodPage : servicePage.getSubPages()) {
                    try {
                        if (methodPage.isPageAlreadyExist()) {
                            updatePage(methodPage.getTitle(), methodPage.getExistedPageInfo(), methodPage.getContent());
                        } else {
                            //create
                            ConfluenceUtils.createPage(methodPage.getTitle(), servicePageId, methodPage.getContent());
                            createdPages.add(methodPage.getTitle());
                        }
                    } catch (Exception e) {
                        logger.info(servicePage.getTitle() + "创建或更新失败 " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.info(servicePage.getTitle() + "创建或更新失败 " + e.getMessage());
            }
        }

        long cost = System.currentTimeMillis() - start;
        logger.info("插入或更新页面 耗时ms:" + cost);
    }

    private void updatePage(String title, ConfluencePageInfo existedPageInfo, String content) {
        // update
        if (!compareHtml(title, existedPageInfo.getBody().getStorage().getValue(), content)) {
            ConfluenceUtils.updatePageContent(existedPageInfo, title, content);
            updatedPages.add(title);
        }
    }

    /**
     * 遍历这个2层树：
     * 检查在Confluence里是否已存在page，拼装数据
     * <p>
     * service 若存在已有页面（pageAlreadyExist=true） 那么 parentPage id 必须与配置的confluenceParentId 相同 否则抛错
     * <p>
     * method ：1,父service页面存在时， method 的页面若存在，parentPage id 必须父service的id相同 否则抛错
     * 2,父service页面不存在时，method 若页面存在，抛错
     */
    private void checkExitedConfluencePage(List<PreparedServicePage> servicePages) {
        long start = System.currentTimeMillis();
        for (PreparedServicePage servicePage : servicePages) {
            ConfluencePageInfo pageInfo = ConfluenceUtils.getPageInfo(servicePage.getTitle());

            Long servicePageId = null;
            if (pageInfo.getId() != null) {
                // 对应服务根页面下已存在相同的Service页面
                servicePageId = pageInfo.getId();
                servicePage.setParentPage(parentPageInfo);
                servicePage.setExistedPageInfo(pageInfo);
                servicePage.setPageAlreadyExist(true);
                if (!pageInfo.getParentPageAncestor().getId().equals(this.parentPageInfo.getId())) {
                    String error = "已存在同名的 '%s' Service页面，请求修改Service注释 @title 后重新生成接口文档！";
                    throw new RuntimeException(String.format(error, servicePage.getTitle()));
                }
            }

            for (PreparedMethodPage methodPage : servicePage.getSubPages()) {
                ConfluencePageInfo mPageInfo = ConfluenceUtils.getPageInfo(methodPage.getTitle());
                if (mPageInfo.getId() != null) {
                    // method页面存在，service页面也存在
                    if (servicePageId != null
                            && !mPageInfo.getParentPageAncestor().getId().equals(servicePageId)) {
                        String error = "已存在同名的 '%s' Service页面，'%s' Method页面，请求修改Service、Method注释 @title 后重新生成接口文档！";
                        throw new RuntimeException(String.format(error, pageInfo.getTitle(), mPageInfo.getTitle()));
                    }
                    if (servicePageId == null) {
                        // method页面存在,然而 service页面不存在
                        String error = "已存在同名的 '%s' Method页面，请修改Method注释 @title 后重新生成接口文档！";
                        throw new RuntimeException(String.format(error, mPageInfo.getTitle()));
                    }
                    methodPage.setPageAlreadyExist(true);
                    methodPage.setParentPage(pageInfo);
                    methodPage.setExistedPageInfo(mPageInfo);
                }
            }
        }

        long cost = System.currentTimeMillis() - start;
        logger.info("检查页面结构关系 以及 页面是否已存在 耗时ms:" + cost);
    }

    /**
     * 解析 Javadoc， 组装好 List<PreparedServicePage> servicePages
     */
    private List<PreparedServicePage> parseJavadoc() {
        long start = System.currentTimeMillis();
        List<PreparedServicePage> ret = new ArrayList<PreparedServicePage>();

        Root root;
        // String xmlPath = rootPath + "/target/site/apidocs/" + "javadoc.xml";
        String xmlPath = "/Users/dreamyao/Documents/javadoc-confluence/src/main/resources/javadoc.xml";
        File xmlFile = new File(xmlPath);
        try {
            String xmlStr = FileUtils.readFileToString(xmlFile, "UTF-8");
            JAXBContext context = JAXBContext.newInstance(Root.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            root = (Root) unmarshaller.unmarshal(new StringReader(xmlStr));

            List<Interface> allInterfaces = getAllInterfaces(root);
            // 过滤掉不需要生成接口文档的Service
            filterInterface(allInterfaces);
            if (allInterfaces.size() == 0) {
                return new ArrayList<>();
            }

            Map<String, Class> allClasses = getAllClassesMap(root);
            if (allClasses.size() == 0) {
                throw new RuntimeException("此工程没有类文件！");
            }

            Map<String, Enum> allEnums = getAllEnumsMap(root);

            for (Interface interf : allInterfaces) {
                PreparedServicePage preparedServicePage = generateServicePage(interf, allClasses, allEnums);
                ret.add(preparedServicePage);
            }
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }

        long cost = System.currentTimeMillis() - start;
        logger.info("解析javadoc 生成页面树 耗时ms:" + cost);
        return ret;
    }

    private void filterInterface(List<Interface> allInterfaces) {
        if (!services.trim().equalsIgnoreCase("all")) {
            String[] sstmp = services.trim().split(",");
            Iterator<Interface> iterator = allInterfaces.iterator();
            while (iterator.hasNext()) {
                Interface next = iterator.next();
                for (String name : sstmp) {
                    if (next.getName().contains(name.trim())) {
                        // 过滤掉不需要生成接口文档的Service
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 拼装 PreparedServicePage
     */
    private PreparedServicePage generateServicePage(Interface interf, Map<String, Class> allClasses, Map<String, Enum> allEnums) {
        PreparedServicePage preparedServicePage = new PreparedServicePage();
        preparedServicePage.setParentPage(parentPageInfo);
        preparedServicePage.setServiceName(interf.getQualified());
        preparedServicePage.setTitle(getServicePageTitle(interf));
        preparedServicePage.setContent(getServicePageContent(interf));

        //检查数据
        if (preparedServicePage.getTitle().length() > MAX_TITLE_LEN) {
            throw new RuntimeException("页面标题不能超过" + MAX_TITLE_LEN + "字符 :" + interf.getQualified());
        }

        List<PreparedMethodPage> methodPages = new ArrayList<PreparedMethodPage>();
        for (Method method : interf.getMethod()) {
            methodPages.add(generateMethodPage(interf, method, allClasses, allEnums));
        }
        preparedServicePage.setSubPages(methodPages);

        return preparedServicePage;
    }

    /**
     * 拼装 PreparedMethodPage
     */
    private PreparedMethodPage generateMethodPage(Interface interf, Method method, Map<String, Class> allClasses, Map<String, Enum> allEnums) {
        PreparedMethodPage preparedMethodPage = new PreparedMethodPage();
        preparedMethodPage.setParentPage(null);
        preparedMethodPage.setServiceName(interf.getQualified());
        preparedMethodPage.setMethodName(method.getName());
        preparedMethodPage.setTitle(getMethodPageTitle(method));
        preparedMethodPage.setContent(getMethodPageContent(interf, method, allClasses, allEnums));

        if (preparedMethodPage.getTitle().length() > MAX_TITLE_LEN) {
            throw new RuntimeException("页面标题不能超过" + MAX_TITLE_LEN + "字符 :" + method.getQualified());
        }
        return preparedMethodPage;
    }

    private String getPom() {

        String pom = "&lt;dependency&gt;\n\t&lt;groupId&gt;%s&lt;/groupId&gt;\n\t&lt;artifactId&gt;%s&lt;/artifactId&gt;\n\t&lt;version&gt;%s&lt;/version&gt;\n&lt;/dependency&gt;";

        String groupId;
        String artifactId;
        String version;

        SAXReader reader = new SAXReader();
        // 读取并解析XML文档
        try {
            Document doc = reader.read(new File(rootPath + "/pom.xml"));
            // 取得Root节点
            Element root = doc.getRootElement();
            Element propGroupId = root.element("groupId");
            groupId = propGroupId.getText();

            Element propArtifactId = root.element("artifactId");
            artifactId = propArtifactId.getText();

            Element propVersion = root.element("version");
            version = propVersion.getText();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }

        return String.format(pom, groupId, artifactId, version);
    }

    /**
     * 拼装 Service 页面的内容
     */
    private String getServicePageContent(Interface interf) {

        VelocityContext ctx = new VelocityContext();

        String pom = getPom();
        ctx.put("pom", pom);
        if (pom.contains("SNAPSHOT")) {
            ctx.put("version", "SNAPSHOT");
        }

        Object[] objs = {"word", "世界"};
        ctx.put("names", objs);
        ctx.put("interface", interf);
        ctx.put("interfaceName", interf.getQualified());

        String author = getServiceTagText(interf, "@author");
        if (StringUtils.isEmpty(author)) {
            author = "";
        }
        ctx.put("interfaceAuthor", author);

        String title = getServiceTagText(interf, "@title");
        if (StringUtils.isEmpty(title)) {
            title = "";
        }
        ctx.put("interfaceDesc", title);

        String date = getServiceTagText(interf, "@date");
        if (StringUtils.isEmpty(date)) {
            date = "";
        }
        ctx.put("updateYmd", date);

        String since = getServiceTagText(interf, "@since");
        if (StringUtils.isEmpty(since)) {
            since = "1.0.0";
        }
        ctx.put("interfaceSince", since);

        StringWriter writer = new StringWriter();
        servicePageTem.merge(ctx, writer);

        String htmlStr = writer.toString();
        if (debug) {
            logger.info(interf.getName() + " 页面内容：" + htmlStr);
        }
        return htmlStr;
    }

    /**
     * 拼装 Service 页面的内容
     */
    private String getMethodPageContent(Interface interf, Method method, Map<String, Class> allClasses, Map<String, Enum> allEnums) {

        List<MethodParameter> parameter = method.getParameter();
        if (parameter.size() == 0) {
            throw new RuntimeException("缺少对外接口的参数：" + method.getQualified());
        }
        if (parameter.size() > 1) {
            throw new RuntimeException("对外接口的参数不能超过1个，请把多个参数封装！" + method.getQualified());
        }
        MethodParameter methodParameter = parameter.get(0);

        MethodPageVO methodPageVO = new MethodPageVO();
        methodPageVO.setServiceName(HtmlUtils.htmlEncode(interf.getQualified()));
        String author = getMethodTagText(method, "@author");
        if (author == null) {
            author = getServiceTagText(interf, "@author");
        }
        if (author == null) {
            author = "";
        }
        methodPageVO.setAuthor(HtmlUtils.htmlEncode(author));
        methodPageVO.setDescription(HtmlUtils.htmlEncode(method.getComment()));

        StringBuilder sb = new StringBuilder();
        sb.append(getTypeInfoStr(method.getReturn()));
        sb.append(" ");
        sb.append(method.getName());
        sb.append(method.getSignature());
        methodPageVO.setSignature(HtmlUtils.htmlEncode(sb.toString()));

        String returnSimpleType = getTypeInfoStrSimple(method.getReturn());
        String paramSimpleType = getTypeInfoStrSimple(methodParameter.getType());
        // 设置入参类型
        methodPageVO.setParamType(HtmlUtils.htmlEncode(paramSimpleType));
        // 设置入参名称
        methodPageVO.setParamName(methodParameter.getName());
        // 设置返回值类型
        methodPageVO.setReturnType(HtmlUtils.htmlEncode(returnSimpleType));

        sb.setLength(0);
        sb.append(returnSimpleType);
        sb.append(" ");
        sb.append(method.getName());
        sb.append("(");
        sb.append(paramSimpleType);
        sb.append(")");
        methodPageVO.setSignatureAbbrev(HtmlUtils.htmlEncode(sb.toString()));

        String since = getMethodTagText(method, "@since");
        if (since == null) {
            since = "";
        }
        // 设置来自版本信息
        methodPageVO.setSince(HtmlUtils.htmlEncode(since));

        String sample = getMethodTagText(method, "@sample");
        if (sample == null) {
            sample = "";
        }
        // 设置示例信息
        methodPageVO.setSample(HtmlUtils.htmlEncode(sample, true));

        String paramTag = getMethodTagText(method, "@param");
        if (paramTag == null) {
            paramTag = "";
        }
        // 设置入参注释信息
        methodPageVO.setParamTag(HtmlUtils.htmlEncode(paramTag));

        String returnTag = getMethodTagText(method, "@return");
        if (returnTag == null) {
            returnTag = "";
        }
        // 设置出参注释信息
        methodPageVO.setReturnTag(HtmlUtils.htmlEncode(returnTag));

        String tables = getMethodTagText(method, "@tables");
        if (StringUtils.isEmpty(tables)) {
            tables = "";
        }
        // 设置涉及表注释信息
        methodPageVO.setTables(tables);

        String special = getMethodTagText(method, "@special");
        if (StringUtils.isEmpty(special)) {
            special = "";
        }
        // 设置特殊说明注释信息
        methodPageVO.setSpecial(special);

        String uri = getMethodTagText(method, "@uri");
        if (StringUtils.isEmpty(uri)) {
            uri = "";
        }
        // 设置前端请求接口地址信息
        methodPageVO.setUri(uri);

        // 设置入参对象
        methodPageVO.setParamClassList(generateClassList(methodParameter.getType(), allClasses, allEnums));
        // 设置出参对象
        methodPageVO.setReturnClassList(generateClassList(method.getReturn(), allClasses, allEnums));

        // 设置接口是否废弃
        methodPageVO.setDeprecated(isDeprecated(method.getAnnotation()).toString());

        VelocityContext ctx = new VelocityContext();
        ctx.put("method", methodPageVO);

        StringWriter writer = new StringWriter();
        methodPageTem.merge(ctx, writer);

        String htmlStr = writer.toString();

        if (debug) {

            try {

                BufferedWriter bfWriter = new BufferedWriter(new FileWriter(new File("target/" + method.getName() + ".html")));
                bfWriter.write(htmlStr);
                bfWriter.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return htmlStr;
    }

    /**
     * 判断接口是否废弃
     * @param annotations 注解集合
     * @return 是否废弃
     */
    private Boolean isDeprecated(List<AnnotationInstance> annotations) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            for (AnnotationInstance annotation : annotations) {
                if (annotation.getName().equalsIgnoreCase("Deprecated")) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ParamClass> generateClassList(TypeInfo typeInfo, Map<String, Class> allClasses, Map<String, Enum> allEnums) {
        List<ParamClass> ret = new ArrayList<>();
        List<String> classNameList = getClassNameOfTypeInfo(typeInfo);
        List<Class> needCheckClasses = new ArrayList<>();
        for (String name : classNameList) {
            if (allClasses.containsKey(name)) {
                needCheckClasses.add(allClasses.get(name));
            }
        }

        for (Class checkClass : needCheckClasses) {
            ParamClass paramClass = new ParamClass();
            ret.add(paramClass);
            paramClass.setQualified(HtmlUtils.htmlEncode(checkClass.getQualified()));
            paramClass.setSimpleName(HtmlUtils.htmlEncode(getSimpleStr(checkClass.getQualified())));

            List<Field> currentFields = checkClass.getField();
            // 添加当前类的所有属性
            List<Field> allFields = new ArrayList<>(currentFields);
            // 获取当前类所有父类中的属性
            getSuperClassField(checkClass.getClazz(), allClasses, allFields);

            List<FieldInfoVO> fields = new ArrayList<>();
            for (Field field : allFields) {
                if (field.isStatic() || field.isFinal()) {
                    continue;
                }
                FieldInfoVO fieldInfoVO = new FieldInfoVO();
                fields.add(fieldInfoVO);
                fieldInfoVO.setName(HtmlUtils.htmlEncode(field.getName()));
                fieldInfoVO.setType(HtmlUtils.htmlEncode(getTypeInfoStrSimple(field.getType())));

                String comment = field.getComment();
                String context = getAnnotationContext(field.getAnnotation());
                // 添加注释信息
                if (StringUtils.isNotBlank(context)) {
                    comment = comment + "；" + context;
                }
                fieldInfoVO.setDescription(HtmlUtils.htmlEncode(comment));

                // 默认可以为空
                fieldInfoVO.setCanBeEmpty("true");
                for (AnnotationInstance ai : field.getAnnotation()) {
                    if (ai.getName().equalsIgnoreCase("NotNull") || ai.getName().equalsIgnoreCase("NotEmpty")
                            || ai.getName().equalsIgnoreCase("NotBlank")) {
                        fieldInfoVO.setCanBeEmpty("false");
                    }
                }

                List<ParamClass> tmp = generateClassList(field.getType(), allClasses, allEnums);
                ret.addAll(tmp);
            }
            paramClass.setFields(fields);
        }
        return ret;
    }

    private void getSuperClassField(TypeInfo clazz, Map<String, Class> allClasses, List<Field> superClassAllFields) {

        // 获取父类信息
        if (Objects.nonNull(clazz)) {
            String qualified = clazz.getQualified();
            Class superClass = allClasses.get(qualified);
            if (Objects.nonNull(superClass)) {
                superClassAllFields.addAll(superClass.getField());
                getSuperClassField(superClass.getClazz(), allClasses, superClassAllFields);
            }

        }
    }

    /**
     * 获取属性上的注释类容 如 @Min(value = 0)
     * @param annotations 注释
     * @return 内容
     */
    private String getAnnotationContext(List<AnnotationInstance> annotations) {

        List<String> context = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(annotations)) {
            for (AnnotationInstance annotation : annotations) {

                StringBuilder builder = new StringBuilder();
                String name = annotation.getName();

                if (name.equalsIgnoreCase("NotNull") || name.equalsIgnoreCase("NotEmpty")
                        || name.equalsIgnoreCase("NotBlank")) {
                    continue;
                }

                builder.append(name);
                List<AnnotationArgument> arguments = annotation.getArgument();
                if (CollectionUtils.isNotEmpty(arguments)) {
                    builder.append("(");
                    for (int i = 0; i < arguments.size(); i++) {
                        AnnotationArgument argument = arguments.get(i);
                        builder.append(argument.getName())
                                .append(" ")
                                .append("=")
                                .append(" ")
                                .append(argument.getValue().get(0));
                        if (i != arguments.size() - 1) {
                            builder.append(" ").append(",").append(" ");
                        }
                    }
                    builder.append(")");
                }

                context.add(builder.toString());
            }
        }

        return Joiner.on("；").join(context);
    }

    /**
     * 查找 interface 的注释中的 tag 值，不存在tag就返回null
     */
    private String getServiceTagText(Interface interf, String tagName) {
        for (TagInfo tagInfo : interf.getTag()) {
            if (tagInfo.getName().equals(tagName)) {
                return tagInfo.getText();
            }
        }
        return null;
    }

    /**
     * 查找 method 的注释中的 tag 值，不存在tag就返回null
     */
    private String getMethodTagText(Method method, String tagName) {
        for (TagInfo tagInfo : method.getTag()) {
            if (tagInfo.getName().equals(tagName)) {
                return tagInfo.getText();
            }
        }
        return null;
    }

    /**
     * 查找interface 的注释中的 @title，若没有，返回接口名
     */
    private String getServicePageTitle(Interface interf) {
        String ret = getServiceTagText(interf, "@title");
        if (ret == null) {
            throw new RuntimeException("@title missing in " + interf.getQualified());
        }
        return ret;
    }

    /**
     * 查找 method 的注释中的 @title，若没有，返回方法
     */
    private String getMethodPageTitle(Method method) {
        String ret = getMethodTagText(method, "@title");
        if (ret == null) {
            throw new RuntimeException("@title missing in " + method.getQualified());
        }
        return ret;
    }

    /**
     * 获取类型 字串
     */
    private String getTypeInfoStr(TypeInfo typeInfo) {
        StringBuilder ret = new StringBuilder(typeInfo.getQualified());
        if (typeInfo.getGeneric() != null && typeInfo.getGeneric().size() > 0) {
            ret.append("<");
            for (int i = 0; i < typeInfo.getGeneric().size(); i++) {//TypeInfo gg : typeInfo.getGeneric()) {
                TypeInfo gg = typeInfo.getGeneric().get(i);
                String tmp = getTypeInfoStr(gg);
                ret.append(tmp);
                if (i != (typeInfo.getGeneric().size() - 1)) {
                    ret.append(", ");
                }
            }
            ret.append(">");
        }
        return ret.toString();
    }

    /**
     * 获取类型 字串 简化的字串
     */
    private String getTypeInfoStrSimple(TypeInfo typeInfo) {
        StringBuilder ret = new StringBuilder(getSimpleStr(typeInfo.getQualified()));
        if (typeInfo.getGeneric() != null && typeInfo.getGeneric().size() > 0) {
            ret.append("<");
            for (int i = 0; i < typeInfo.getGeneric().size(); i++) {//TypeInfo gg : typeInfo.getGeneric()) {
                TypeInfo gg = typeInfo.getGeneric().get(i);
                String tmp = getTypeInfoStrSimple(gg);
                ret.append(tmp);
                if (i != (typeInfo.getGeneric().size() - 1)) {
                    ret.append(", ");
                }
            }
            ret.append(">");
        }
        return ret.toString();
    }

    private String getSimpleStr(String qualified) {
        String[] tmp = qualified.split("\\.");
        return tmp[tmp.length - 1];
    }

    /**
     * 获取 TypeInfo 里的所有类名
     */
    private List<String> getClassNameOfTypeInfo(TypeInfo typeInfo) {
        List<String> ret = new ArrayList<String>();
        ret.add(typeInfo.getQualified());
        if (typeInfo.getGeneric() != null && typeInfo.getGeneric().size() > 0) {
            for (int i = 0; i < typeInfo.getGeneric().size(); i++) {
                // TypeInfo gg : typeInfo.getGeneric()) {
                TypeInfo gg = typeInfo.getGeneric().get(i);
                ret.addAll(getClassNameOfTypeInfo(gg));
            }
        }
        return ret;
    }

    /**
     * 获取所有class
     */
    private Map<String, Class> getAllClassesMap(Root root) {
        Map<String, Class> ret = new HashMap<>();
        for (Package pkg : root.getPackage()) {
            for (Class clz : pkg.getClazz()) {
                ret.put(clz.getQualified(), clz);
            }
        }
        return ret;
    }

    /**
     * 获取所有 Enum
     */
    private Map<String, Enum> getAllEnumsMap(Root root) {
        Map<String, Enum> ret = new HashMap<>();
        for (Package pkg : root.getPackage()) {
            for (Enum eum : pkg.getEnum()) {
                ret.put(eum.getQualified(), eum);
            }
        }
        return ret;
    }

    /**
     * 获取所有接口
     */
    private List<Interface> getAllInterfaces(Root root) {
        List<Interface> ret = new ArrayList<>();
        for (Package pkg : root.getPackage()) {
            ret.addAll(pkg.getInterface());
        }
        return ret;
    }

    /**
     * 初始化 Velocity
     */
    private void initVelocity() {

        VelocityEngine ve = new VelocityEngine();
        // 可选值："class"--从classpath中读取，"file"--从文件系统中读取
        ve.setProperty("resource.loader", "class");
        // 如果从文件系统中读取模板，那么属性值为org.apache.velocity.runtime.resource.loader.FileResourceLoader
        ve.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        ve.init();
        servicePageTem = ve.getTemplate(VM_FILENAME_SERVICE, "utf-8");
        methodPageTem = ve.getTemplate(VM_FILENAME_METHOD, "utf-8");
    }

    /**
     * 以 pom.xml 的<properties>下 <confluence.page.title> 的值为父页面titie，查找父页面id
     */
    private long getParentPageId() {
        ConfluencePageInfo pageInfo = ConfluenceUtils.getPageInfo(parentPageTitleInPom);
        if (pageInfo.getId() == null) {
            // 如果confluence没有创建，则自动创建在API下面
            pageInfo = ConfluenceUtils.getPageInfo("API");
            pageInfo = ConfluenceUtils.createPage(parentPageTitleInPom, pageInfo.getId(), pageInfo.getBody().getStorage().getValue());

        }
        parentPageInfo = pageInfo;
        ConfluenceUtils.setParentId(pageInfo.getId());
        return pageInfo.getId();
    }

    /**
     * 读取 pom 中的配置信息
     */
    private void readPom() {
        SAXReader reader = new SAXReader();
        // 读取并解析XML文档
        try {
            Document doc = reader.read(new File(rootPath + "/pom.xml"));
            // 取得Root节点
            Element root = doc.getRootElement();
            Element prop = root.element("properties");
            if (prop == null) {
                throw new RuntimeException("pom.xml 格式错误，缺少<properties>");
            }
            Element eTitle = prop.element("confluence.page.title");
            if (eTitle == null) {
                // throw new RuntimeException("pom.xml 格式错误，缺少<properties><confluence.page.title>");
            }
            parentPageTitleInPom = "巴音孟克运单服务";

        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 比较内容是否相同
     */
    private boolean compareHtml(String title, String html1, String html2) {
        String str1, str2;
        try {
            str1 = formatHtml("<html>" + html1 + "</html>");

        } catch (Exception e) {
            logger.error("比对页面 '" + title + "' 内容时错误：");
            logger.error("parse html1 error:" + html1, e);
            checkErrPages.add(title);
            return false;
        }

        try {
            str2 = formatHtml("<html>" + html2 + "</html>");
        } catch (Exception e) {
            logger.error("parse html2 error:" + html2, e);
            throw new RuntimeException("parse html error:", e);
        }

        return str1.equals(str2);
    }

    /**
     * html 必须是格式良好的
     * @param str
     * @return
     * @throws Exception
     */
    private String formatHtml(String str) throws Exception {

        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setIndent(false);
        format.setXHTML(true);
        format.setPadText(false);
        format.setEncoding("utf-8");

        StringWriter writer = new StringWriter();
        HTMLWriter htmlWriter = new HTMLWriter(writer, format);

        Document document = DocumentHelper.parseText(str);
        htmlWriter.write(document);
        htmlWriter.close();
        return writer.toString().replaceAll(format.getLineSeparator(), "");
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        ConfluenceUtils.setDebug(true);
    }

    public void setServices(String services) {
        this.services = services;
    }

    public void setMethods(String methods) {
        this.methods = methods;
    }
}
