/**
 *
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
package org.apache.xbean.recipe;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KotlinConstructorTest {

    /**
     * Reproduces the TomEE/OpenEJB NPE when using CASE_INSENSITIVE_PROPERTIES
     * with NAMED_PARAMETERS on a Kotlin class.  Kotlin generates a synthetic
     * constructor for default parameter values whose parameter names may
     * contain null entries in the ASM local variable table.  When
     * availableProperties is a TreeSet with CASE_INSENSITIVE_ORDER,
     * containsAll(parameterNames) throws NPE on the null element because
     * the comparator cannot compare null strings.
     *
     * This is the scenario that triggers the bug in TomEE:
     *   ObjectRecipe.findFactory -> ReflectionUtil.findConstructor
     *   with availableProperties as TreeSet(CASE_INSENSITIVE_ORDER)
     */
    @Test
    public void testCaseInsensitiveWithKotlinSyntheticConstructor() {
        final Set<String> availableProperties = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Collections.addAll(availableProperties, "host", "port", "path");

        // This is the code path TomEE uses: no parameterNames, no parameterTypes,
        // NAMED_PARAMETERS + CASE_INSENSITIVE_PROPERTIES.  findConstructor iterates
        // all declared constructors (including Kotlin synthetics) calling
        // getParameterNames(constructor) via ASM. If any returned name is null,
        // TreeSet.containsAll throws NPE.
        final ReflectionUtil.ConstructorFactory factory = ReflectionUtil.findConstructor(
                KotlinConfigClass.class,
                null,
                null,
                availableProperties,
                EnumSet.of(Option.NAMED_PARAMETERS, Option.CASE_INSENSITIVE_PROPERTIES));

        assertNotNull("Constructor should be found for Kotlin class", factory);
        assertEquals(3, factory.getParameterTypes().size());
    }
}
