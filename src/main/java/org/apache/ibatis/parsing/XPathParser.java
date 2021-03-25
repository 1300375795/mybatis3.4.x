/**
 * Copyright 2009-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.parsing;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.ibatis.builder.BuilderException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * X path解析器
 *
 * @author Clinton Begin
 */
public class XPathParser {

    /**
     * 文档
     */
    private final Document document;

    /**
     * 是否有效
     */
    private boolean validation;

    /**
     * 实体解析器
     */
    private EntityResolver entityResolver;

    /**
     * 配置属性
     */
    private Properties variables;

    /**
     * X path
     */
    private XPath xpath;

    /**
     * 构造函数
     *
     * @param xml xml文件路径
     */
    public XPathParser(String xml) {
        commonConstructor(false, null, null);
        this.document = createDocument(new InputSource(new StringReader(xml)));
    }

    /**
     * 构造函数
     *
     * @param reader
     */
    public XPathParser(Reader reader) {
        commonConstructor(false, null, null);
        this.document = createDocument(new InputSource(reader));
    }

    /**
     * 构造函数
     *
     * @param inputStream
     */
    public XPathParser(InputStream inputStream) {
        commonConstructor(false, null, null);
        this.document = createDocument(new InputSource(inputStream));
    }

    /**
     * 构造函数
     *
     * @param document
     */
    public XPathParser(Document document) {
        commonConstructor(false, null, null);
        this.document = document;
    }

    /**
     * 构造函数
     *
     * @param xml
     * @param validation
     */
    public XPathParser(String xml, boolean validation) {
        commonConstructor(validation, null, null);
        this.document = createDocument(new InputSource(new StringReader(xml)));
    }

    /**
     * 构造函数
     *
     * @param reader
     * @param validation
     */
    public XPathParser(Reader reader, boolean validation) {
        commonConstructor(validation, null, null);
        this.document = createDocument(new InputSource(reader));
    }

    /**
     * 构造函数
     *
     * @param inputStream
     * @param validation
     */
    public XPathParser(InputStream inputStream, boolean validation) {
        commonConstructor(validation, null, null);
        this.document = createDocument(new InputSource(inputStream));
    }

    /**
     * 构造函数
     *
     * @param document
     * @param validation
     */
    public XPathParser(Document document, boolean validation) {
        commonConstructor(validation, null, null);
        this.document = document;
    }

    /**
     * 构造函数
     *
     * @param xml
     * @param validation
     * @param variables
     */
    public XPathParser(String xml, boolean validation, Properties variables) {
        commonConstructor(validation, variables, null);
        this.document = createDocument(new InputSource(new StringReader(xml)));
    }

    /**
     * 构造函数
     *
     * @param reader
     * @param validation
     * @param variables
     */
    public XPathParser(Reader reader, boolean validation, Properties variables) {
        commonConstructor(validation, variables, null);
        this.document = createDocument(new InputSource(reader));
    }

    /**
     * 构造函数
     *
     * @param inputStream
     * @param validation
     * @param variables
     */
    public XPathParser(InputStream inputStream, boolean validation, Properties variables) {
        commonConstructor(validation, variables, null);
        this.document = createDocument(new InputSource(inputStream));
    }

    /**
     * 构造函数
     *
     * @param document
     * @param validation
     * @param variables
     */
    public XPathParser(Document document, boolean validation, Properties variables) {
        commonConstructor(validation, variables, null);
        this.document = document;
    }

    /**
     * 构造函数
     *
     * @param xml
     * @param validation
     * @param variables
     * @param entityResolver
     */
    public XPathParser(String xml, boolean validation, Properties variables, EntityResolver entityResolver) {
        commonConstructor(validation, variables, entityResolver);
        this.document = createDocument(new InputSource(new StringReader(xml)));
    }

    /**
     * 构造函数
     *
     * @param reader
     * @param validation
     * @param variables
     * @param entityResolver
     */
    public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
        commonConstructor(validation, variables, entityResolver);
        this.document = createDocument(new InputSource(reader));
    }

    /**
     * 构造函数
     *
     * @param inputStream
     * @param validation
     * @param variables
     * @param entityResolver
     */
    public XPathParser(InputStream inputStream, boolean validation, Properties variables,
            EntityResolver entityResolver) {
        commonConstructor(validation, variables, entityResolver);
        this.document = createDocument(new InputSource(inputStream));
    }

    /**
     * 构造函数
     *
     * @param document
     * @param validation
     * @param variables
     * @param entityResolver
     */
    public XPathParser(Document document, boolean validation, Properties variables, EntityResolver entityResolver) {
        commonConstructor(validation, variables, entityResolver);
        this.document = document;
    }

    /**
     * 设置属性配置
     *
     * @param variables
     */
    public void setVariables(Properties variables) {
        this.variables = variables;
    }

    /**
     * 根据给出的表达解析得到string
     *
     * @param expression
     * @return
     */
    public String evalString(String expression) {
        return evalString(document, expression);
    }

    /**
     * 根据给出的root节点以及表达式解析string
     *
     * @param root
     * @param expression
     * @return
     */
    public String evalString(Object root, String expression) {
        String result = (String) evaluate(expression, root, XPathConstants.STRING);
        result = PropertyParser.parse(result, variables);
        return result;
    }

    /**
     * 根据给出的表达式解析boolean
     *
     * @param expression
     * @return
     */
    public Boolean evalBoolean(String expression) {
        return evalBoolean(document, expression);
    }

    /**
     * 根据给出的根节点以及表达式解析boolean
     *
     * @param root
     * @param expression
     * @return
     */
    public Boolean evalBoolean(Object root, String expression) {
        return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
    }

    /**
     * 根据给出表达式解析short
     *
     * @param expression
     * @return
     */
    public Short evalShort(String expression) {
        return evalShort(document, expression);
    }

    /**
     * 根据给出的根节点以及表达式解析short
     *
     * @param root
     * @param expression
     * @return
     */
    public Short evalShort(Object root, String expression) {
        return Short.valueOf(evalString(root, expression));
    }

    /**
     * 根据给出的表达式解析integer
     *
     * @param expression
     * @return
     */
    public Integer evalInteger(String expression) {
        return evalInteger(document, expression);
    }

    /**
     * 根据给出的根节点以及表达式解析integer
     *
     * @param root
     * @param expression
     * @return
     */
    public Integer evalInteger(Object root, String expression) {
        return Integer.valueOf(evalString(root, expression));
    }

    /**
     * 根据给出的表达式解析long
     *
     * @param expression
     * @return
     */
    public Long evalLong(String expression) {
        return evalLong(document, expression);
    }

    /**
     * 根据给出的根节点以及表达式解析long
     *
     * @param root
     * @param expression
     * @return
     */
    public Long evalLong(Object root, String expression) {
        return Long.valueOf(evalString(root, expression));
    }

    /**
     * 根据给出的表达式解析float
     *
     * @param expression
     * @return
     */
    public Float evalFloat(String expression) {
        return evalFloat(document, expression);
    }

    /**
     * 根据给出的根节点以及表达式解析float
     *
     * @param root
     * @param expression
     * @return
     */
    public Float evalFloat(Object root, String expression) {
        return Float.valueOf(evalString(root, expression));
    }

    /**
     * 根据给出的表达式解析double
     *
     * @param expression
     * @return
     */
    public Double evalDouble(String expression) {
        return evalDouble(document, expression);
    }

    /**
     * 根据给出的根节点以及表达式解析double
     *
     * @param root
     * @param expression
     * @return
     */
    public Double evalDouble(Object root, String expression) {
        return (Double) evaluate(expression, root, XPathConstants.NUMBER);
    }

    /**
     * 根据给出的表达式解析node节点集合
     *
     * @param expression
     * @return
     */
    public List<XNode> evalNodes(String expression) {
        return evalNodes(document, expression);
    }

    /**
     * 根据给出的根节点以及表达式解析node节点集合
     *
     * @param root
     * @param expression
     * @return
     */
    public List<XNode> evalNodes(Object root, String expression) {
        List<XNode> xnodes = new ArrayList<XNode>();
        NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            xnodes.add(new XNode(this, nodes.item(i), variables));
        }
        return xnodes;
    }

    /**
     * 根据给出的表达式解析node节点
     *
     * @param expression
     * @return
     */
    public XNode evalNode(String expression) {
        return evalNode(document, expression);
    }

    /**
     * 根据给出的根节点以及表达式解析node节点
     *
     * @param root
     * @param expression
     * @return
     */
    public XNode evalNode(Object root, String expression) {
        Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
        if (node == null) {
            return null;
        }
        return new XNode(this, node, variables);
    }

    /**
     * 根据给出的表达式、根节点以及返回类型解析得到返回类型对应的对象
     *
     * @param expression
     * @param root
     * @param returnType
     * @return
     */
    private Object evaluate(String expression, Object root, QName returnType) {
        try {
            return xpath.evaluate(expression, root, returnType);
        } catch (Exception e) {
            throw new BuilderException("Error evaluating XPath.  Cause: " + e, e);
        }
    }

    /**
     * 根据给出的xml输入源构建文档
     *
     * @param inputSource
     * @return
     */
    private Document createDocument(InputSource inputSource) {
        // important: this must only be called AFTER common constructor
        //必须要在commonConstructor之后被调用
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            //根据属性设置
            factory.setValidating(validation);
            //设置基本的一些参数
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(false);
            factory.setCoalescing(false);
            factory.setExpandEntityReferences(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(entityResolver);
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void warning(SAXParseException exception) throws SAXException {
                }
            });
            return builder.parse(inputSource);
        } catch (Exception e) {
            throw new BuilderException("Error creating document instance.  Cause: " + e, e);
        }
    }

    /**
     * 公共构造函数
     *
     * @param validation
     * @param variables
     * @param entityResolver
     */
    private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
        this.validation = validation;
        this.entityResolver = entityResolver;
        this.variables = variables;
        XPathFactory factory = XPathFactory.newInstance();
        this.xpath = factory.newXPath();
    }

}
