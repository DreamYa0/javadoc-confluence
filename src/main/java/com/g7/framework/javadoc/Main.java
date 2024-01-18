package com.g7.framework.javadoc;

import com.g7.framework.javadoc.util.ConfluenceUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {

    private final static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {

        String rootPath = "/Users/dreamyao/Documents/javadoc-confluence";
        String services = "all";
        String methods = "all";

        try {
            InputStream propertiesStream = Main.class.getResourceAsStream("/javadoc-confluence.properties");
            Properties properties = new Properties();
            properties.load(propertiesStream);
            String url = properties.getProperty("confluence.url");
            String spaceKey = properties.getProperty("confluence.spacekey");
            String username = properties.getProperty("confluence.username");
            String password = properties.getProperty("confluence.password");
            if (StringUtils.isEmpty(url) || StringUtils.isEmpty(spaceKey) || StringUtils.isEmpty(username) ||
                    StringUtils.isEmpty(password)) {

                throw new RuntimeException("properties file lack of config item!!");
            }

            ConfluenceUtils.setUrl(url);
            ConfluenceUtils.setSpaceKey(spaceKey);
            ConfluenceUtils.setUsername(username);
            ConfluenceUtils.setPassword(password);

            ConfluenceExecutor executor = new ConfluenceExecutor(rootPath);
            if (args.length == 5 && args[4].equals("debug")) {
                executor.setDebug(true);
            }

            executor.setServices(services);
            executor.setMethods(methods);

            executor.execute();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String usage() {
        return "参数包含: rootPath confPath services methods [debug]\nrootPath : api project root path"
                + "\nconfPath : properties file and velocity files path"
                + "\nservices : 要检查更新的接口名，逗号分隔，不能有空格 例如 TradePayService,OrderService"
                + "\nmethods : 要检查更新的方法名，逗号分隔，不能有空格 例如 queryTradeList,createOrder"
                + "\ndebug : is debug mode"
                + "\n\tSample : ";
    }
}
