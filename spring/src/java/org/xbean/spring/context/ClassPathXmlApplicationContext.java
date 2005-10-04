/**
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.xbean.spring.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.xbean.spring.context.impl.XBeanXmlBeanDefinitionParser;

/**
 * An XBean version of the regular Spring class to provide improved XML handling.
 * 
 * @author James Strachan
 * @version $Revision: 1.1 $
 */
public class ClassPathXmlApplicationContext extends org.springframework.context.support.ClassPathXmlApplicationContext {

    public ClassPathXmlApplicationContext(String arg0) throws BeansException {
        super(arg0);
    }

    public ClassPathXmlApplicationContext(String[] arg0, ApplicationContext arg1) throws BeansException {
        super(arg0, arg1);
    }

    public ClassPathXmlApplicationContext(String[] arg0, boolean arg1, ApplicationContext arg2) throws BeansException {
        super(arg0, arg1, arg2);
    }

    public ClassPathXmlApplicationContext(String[] arg0, boolean arg1) throws BeansException {
        super(arg0, arg1);
    }

    public ClassPathXmlApplicationContext(String[] arg0) throws BeansException {
        super(arg0);
    }

    protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
        super.initBeanDefinitionReader(reader);
        XBeanXmlBeanDefinitionParser.configure(this, reader);
    }

    protected DefaultListableBeanFactory createBeanFactory() {
        DefaultListableBeanFactory beanFactory = super.createBeanFactory();
        XBeanXmlBeanDefinitionParser.registerCustomEditors(beanFactory);
        return beanFactory;
    }


}
