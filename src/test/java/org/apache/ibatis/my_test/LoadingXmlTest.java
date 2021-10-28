package org.apache.ibatis.my_test;

import java.io.InputStream;
import java.util.Properties;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

/**
 * 测试mybatis的config.xml配置加载流程
 *
 * @author YDG
 * @description
 * @since 2021/10/21
 */
public class LoadingXmlTest {

    /**
     * 测试配置了相关的属性的配置文件的加载
     *
     * @throws Exception
     */
    @Test
    public void testShouldSuccessfullyLoadXMLConfigFile() throws Exception {
        String resource = "org/apache/ibatis/builder/CustomizedSettingsMapperConfig.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        Properties props = new Properties();
        props.put("prop2", "cccc");
        XMLConfigBuilder builder = new XMLConfigBuilder(inputStream, null, props);
        Configuration config = builder.parse();
        inputStream.close();
    }

}
