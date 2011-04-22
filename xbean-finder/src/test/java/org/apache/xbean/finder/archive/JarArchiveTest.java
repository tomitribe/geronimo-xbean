/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.finder.archive;

import org.acme.foo.Blue;
import org.acme.foo.Green;
import org.acme.foo.Red;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @version $Rev$ $Date$
 */
public class JarArchiveTest {

    private static final Class[] classes = {Blue.class, Blue.Navy.class, Blue.Sky.class, Green.class, Green.Emerald.class, Red.class, Red.CandyApple.class, Red.Pink.class};
    private static File classpath;
    private JarArchive archive;

    @BeforeClass
    public static void classSetUp() throws Exception {

        ClassLoader loader = JarArchiveTest.class.getClassLoader();

        classpath = File.createTempFile("path with spaces", "classes");

        // Create the ZIP file
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(classpath)));

        for (Class clazz : classes) {
            String name = clazz.getName().replace('.', File.separatorChar) + ".class";

            URL resource = loader.getResource(name);
            assertNotNull(resource);

            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(name));

            InputStream in = new BufferedInputStream(resource.openStream());

            int i = -1;
            while ((i = in.read()) != -1) {
                out.write(i);
            }

            // Complete the entry
            out.closeEntry();
        }

        // Complete the ZIP file
        out.close();
    }

    @Before
    public void setUp() throws Exception {

        URL[] urls = {new URL("jar:" + classpath.toURI().toURL() + "!/")};

        archive = new JarArchive(new URLClassLoader(urls), urls[0]);
    }


    @Test
    public void testGetBytecode() throws Exception {

        for (Class clazz : classes) {
            assertNotNull(clazz.getName(), archive.getBytecode(clazz.getName()));
        }

        try {
            archive.getBytecode("Fake");
            fail("ClassNotFoundException should have been thrown");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testLoadClass() throws Exception {
        for (Class clazz : classes) {
            assertEquals(clazz.getName(), clazz, archive.loadClass(clazz.getName()));
        }

        try {
            archive.loadClass("Fake");
            fail("ClassNotFoundException should have been thrown");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testIterator() throws Exception {
        List<String> actual = new ArrayList<String>();
        for (String classname : archive) {
            actual.add(classname);
        }

        assertFalse(0 == actual.size());

        for (Class clazz : classes) {
            assertTrue(clazz.getName(), actual.contains(clazz.getName()));
        }

        assertEquals(classes.length, actual.size());
    }


}