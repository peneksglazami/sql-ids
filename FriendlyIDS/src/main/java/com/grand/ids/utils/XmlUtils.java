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
package com.grand.ids.utils;

import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Утилиты для выполнения преобразований XML->Object и Object->XML
 * @author Andrey Grigorov
 */
public final class XmlUtils {

    public static void marshal(Object obj, OutputStream out, Class... boundClasses) {
        try {
            Marshaller m = JAXBContext.newInstance(boundClasses).createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(obj, out);
        } catch (Exception ex) {
        }
    }

    public static <T> T unmarshal(InputStream in, Class... boundClasses) {
        try {
            Unmarshaller u = JAXBContext.newInstance(boundClasses).createUnmarshaller();
            return (T) u.unmarshal(in);
        } catch (Exception ex) {
        }
        return null;
    }
}
