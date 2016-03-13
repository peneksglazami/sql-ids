/*
 * Copyright 2009-2016 Andrey Grigorov
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.grand.ids.utils.xslt;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Класс, реализующий функцию xsl-трансформатора
 * @author Andrey Grigorov
 */
public class XslTransformator {

    public final static String XML_ENCODING = "Cp1251";
    public final static String XSLT_ENCODING = "UTF-8";
    public final static String RESULT_ENCODING = "Cp1251";

    /**
     * Выполнение xsl-трансформации
     * @param xml поток, содержащий XML документ в кодировке windows-1251
     * @param xslt поток, содержащий xsl-шаблон в кодировке UTF-8
     * @return поток, который содержит документ в кодировке windows-1251,
     * полученный результате трансформации
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     * @throws TransformerConfigurationException
     * @throws TransformerException
     */
    public static OutputStream transform(InputStream xml, InputStream xslt) throws UnsupportedEncodingException, FileNotFoundException, TransformerConfigurationException, TransformerException {
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");

        BufferedReader bufferReaderXml = new BufferedReader(new InputStreamReader(xml, XML_ENCODING));
        BufferedReader bufferReaderXslt = new BufferedReader(new InputStreamReader(xslt, XSLT_ENCODING));

        Source xmlSource = new StreamSource(bufferReaderXml);
        Source xsltSource = new StreamSource(bufferReaderXslt);

        ByteArrayOutputStream byteArrayOutputStreamResult = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriterResult = new OutputStreamWriter(byteArrayOutputStreamResult, RESULT_ENCODING);
        StreamResult streamResult = new StreamResult(outputStreamWriterResult);

        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer(xsltSource);
        transformer.setOutputProperty(OutputKeys.ENCODING, XSLT_ENCODING);
        transformer.transform(xmlSource, streamResult);

        return byteArrayOutputStreamResult;
    }
}
