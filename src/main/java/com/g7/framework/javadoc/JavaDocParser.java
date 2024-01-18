package com.g7.framework.javadoc;

import com.github.markusbernhardt.xmldoclet.xjc.Class;
import com.github.markusbernhardt.xmldoclet.xjc.Enum;
import com.github.markusbernhardt.xmldoclet.xjc.Interface;
import com.github.markusbernhardt.xmldoclet.xjc.Method;
import com.github.markusbernhardt.xmldoclet.xjc.MethodParameter;
import com.github.markusbernhardt.xmldoclet.xjc.Package;
import com.github.markusbernhardt.xmldoclet.xjc.Root;
import com.github.markusbernhardt.xmldoclet.xjc.TypeInfo;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JavaDocParser {

    private final static Logger logger = Logger.getLogger(JavaDocParser.class);
    private String javadocXmlPath;

    private Root root;

    private Map<String, Interface> allInterfaces;

    private Map<String, Class> allClasses;

    private Map<String, Enum> allEnums;

    public JavaDocParser(String javadocXmlPath) {
        parseJavadoc(javadocXmlPath);
    }

    /**
     * @param interfaceName 接口名，包含包名 如： com.zbj.xxx.TestService
     * @param methodName    方法名
     * @return 方法的入参 及出参的
     */
    public List<GenericTypeInfo> getGenericInfoOfMethod(String interfaceName, String methodName) {
        Interface interf = allInterfaces.get(interfaceName);
        if (interf == null) {
            throw new RuntimeException("没有这个接口：" + interfaceName);
        }
        Method targetMethod = null;
        for (Method method : interf.getMethod()) {
            if (method.getName().equals(methodName)) {
                targetMethod = method;
            }
        }
        if (targetMethod == null) {
            throw new RuntimeException("没有这个方法：" + methodName);
        }
        List<GenericTypeInfo> ret = new ArrayList<GenericTypeInfo>();
        ret.add(getTypeInfoStr(targetMethod.getReturn()));

        for (MethodParameter param : targetMethod.getParameter()) {
            ret.add(getTypeInfoStr(param.getType()));
        }

        return ret;

    }

    public GenericTypeInfo getTypeInfoStr(TypeInfo typeInfo) {
        GenericTypeInfo ret = new GenericTypeInfo();
        ret.setName(typeInfo.getQualified());
        ret.setSimpleName(getSimpleStr(typeInfo.getQualified()));
        if (typeInfo.getGeneric() != null && typeInfo.getGeneric().size() > 0) {
            List<GenericTypeInfo> subGeneric = new ArrayList<GenericTypeInfo>(typeInfo.getGeneric().size());
            ret.setGenericTypeInfos(subGeneric);
            for (int i = 0; i < typeInfo.getGeneric().size(); i++) {//TypeInfo gg : typeInfo.getGeneric()) {
                TypeInfo gg = typeInfo.getGeneric().get(i);
                GenericTypeInfo tmp = getTypeInfoStr(gg);
                subGeneric.add(tmp);
            }
        }
        return ret;
    }

    private String getSimpleStr(String qualified) {
        String[] tmp = qualified.split("\\.");
        return tmp[tmp.length - 1];
    }

    /**
     * 解析 Javadoc， 组装好 List<PreparedServicePage> servicePages
     */
    private void parseJavadoc(String javadocXmlPath) {
        logger.info("===解析javadoc");
        this.javadocXmlPath = javadocXmlPath;
        long start = System.currentTimeMillis();

        Root root = null;
        String xmlPath = javadocXmlPath;
        File xmlFile = new File(xmlPath);
        try {
            String xmlStr = FileUtils.readFileToString(xmlFile, "UTF-8");
            JAXBContext context = JAXBContext.newInstance(Root.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            root = (Root) unmarshaller.unmarshal(new StringReader(xmlStr));
            allInterfaces = getAllInterfaces(root);
            if (allInterfaces.size() == 0) {
                throw new RuntimeException("本工程并没有接口！！");
            }
            allClasses = getAllClassesMap(root);
            if (allClasses.size() == 0) {
                logger.warn("本工程并class！！");
            }
            allEnums = getAllEnumsMap(root);

        } catch (JAXBException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long cost = System.currentTimeMillis() - start;
        logger.info("===耗时ms:" + cost);
        return;
    }

    /**
     * 获取所有class
     */
    private Map<String, Class> getAllClassesMap(Root root) {
        Map<String, Class> ret = new HashMap<String, Class>();
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
        Map<String, Enum> ret = new HashMap<String, Enum>();
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
    private Map<String, Interface> getAllInterfaces(Root root) {
        Map<String, Interface> ret = new HashMap<String, Interface>();
        for (Package pkg : root.getPackage()) {
            for (Interface intef : pkg.getInterface()) {
                ret.put(intef.getQualified(), intef);
            }
        }
        return ret;
    }

    public class GenericTypeInfo {
        private String name;
        private String simpleName;
        private List<GenericTypeInfo> genericTypeInfos;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSimpleName() {
            return simpleName;
        }

        public void setSimpleName(String simpleName) {
            this.simpleName = simpleName;
        }

        public List<GenericTypeInfo> getGenericTypeInfos() {
            return genericTypeInfos;
        }

        public void setGenericTypeInfos(List<GenericTypeInfo> genericTypeInfos) {
            this.genericTypeInfos = genericTypeInfos;
        }
    }
}
