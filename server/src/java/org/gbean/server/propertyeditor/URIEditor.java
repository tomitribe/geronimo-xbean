/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gbean.server.propertyeditor;

import java.beans.PropertyEditorSupport;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * InetAddressEditor is a java beans property editor that can convert an URI to and from a String.
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class URIEditor extends PropertyEditorSupport {
    public void setAsText(String value) throws IllegalArgumentException {
        try {
            setValue(new URI(value));
        } catch (URISyntaxException e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
    }

    public String getAsText() {
        URI uri = (URI) getValue();
        String text = uri.toString();
        return text;
    }
}