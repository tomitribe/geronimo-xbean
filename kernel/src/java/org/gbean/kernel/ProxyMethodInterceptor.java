/**
 *
 * Copyright 2004 The Apache Software Foundation
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
package org.gbean.kernel;

import java.lang.reflect.Method;
import java.util.Map;
import javax.management.ObjectName;

import net.sf.cglib.asm.Type;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.reflect.FastClass;
import org.apache.geronimo.gbean.GOperationSignature;
import org.apache.geronimo.kernel.Kernel;
import org.gbean.beans.GBeanInstance;
import org.gbean.beans.RawInvoker;

/**
 * @version $Rev: 106345 $ $Date: 2004-11-23 12:37:03 -0800 (Tue, 23 Nov 2004) $
 */
public class ProxyMethodInterceptor implements MethodInterceptor {
    /**
     * Type of the proxy interface
     */
    private final Class proxyType;

    /**
     * The object name to which we are connected.
     */
    private final ObjectName objectName;

    /**
     * GBeanInvokers keyed on the proxy interface method index
     */
    private final ProxyInvoker[] gbeanInvokers;

    private final Object data;

    public ProxyMethodInterceptor(Class proxyType, Kernel kernel, ObjectName objectName) {
        this(proxyType, kernel, objectName, null);
    }

    public ProxyMethodInterceptor(Class proxyType, Kernel kernel, ObjectName objectName, Object data) {
        assert proxyType != null;
        assert kernel != null;
        assert objectName != null;

        this.proxyType = proxyType;
        this.objectName = objectName;
        this.data = data;
        gbeanInvokers = createGBeanInvokers(kernel, objectName);
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public Object getData() {
        return data;
    }

    public final Object intercept(final Object object, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
        ProxyInvoker gbeanInvoker;

        int interfaceIndex = proxy.getSuperIndex();
        gbeanInvoker = gbeanInvokers[interfaceIndex];
        if (gbeanInvoker == null) {
            throw new UnsupportedOperationException("No implementation method: objectName=" + objectName + ", method=" + method);
        }

        return gbeanInvoker.invoke(objectName, args);
    }

    private ProxyInvoker[] createGBeanInvokers(Kernel kernel, ObjectName objectName) {
        ProxyInvoker[] invokers;
        try {
            RawInvoker rawInvoker = (RawInvoker) kernel.getAttribute(objectName, GBeanInstance.RAW_INVOKER);
            invokers = createRawGBeanInvokers(rawInvoker, proxyType);
        } catch (Exception e) {
            throw new AssertionError("Could not get raw invoker for gbean: " + objectName);
        }

        // handle equals, hashCode and toString directly here
        try {
            invokers[getSuperIndex(proxyType, proxyType.getMethod("equals", new Class[]{Object.class}))] = new EqualsInvoke(this);
            invokers[getSuperIndex(proxyType, proxyType.getMethod("hashCode", null))] = new HashCodeInvoke(this);
            invokers[getSuperIndex(proxyType, proxyType.getMethod("toString", null))] = new ToStringInvoke(proxyType.getName());
        } catch (Exception e) {
            // this can not happen... all classes must implement equals, hashCode and toString
            throw new AssertionError(e);
        }

        return invokers;
    }

    private ProxyInvoker[] createRawGBeanInvokers(RawInvoker rawInvoker, Class proxyType) {
        Map operations = rawInvoker.getOperationIndex();
//        Map attributes = rawInvoker.getAttributeIndex();

        // build the method lookup table
        FastClass fastClass = FastClass.create(proxyType);
        ProxyInvoker[] invokers = new ProxyInvoker[fastClass.getMaxIndex() + 1];
        Method[] methods = proxyType.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            int interfaceIndex = getSuperIndex(proxyType, method);
            if (interfaceIndex >= 0) {
                invokers[interfaceIndex] = createRawGBeanInvoker(rawInvoker, method, operations/*, attributes*/);
            }
        }

        return invokers;
    }

    private ProxyInvoker createRawGBeanInvoker(RawInvoker rawInvoker, Method method, Map operations/*, Map attributes*/) {
        if (operations.containsKey(new GOperationSignature(method))) {
            int methodIndex = ((Integer) operations.get(new GOperationSignature(method))).intValue();
            return new RawOperationInvoker(rawInvoker, methodIndex);
        }

        return null;
    }

    private static int getSuperIndex(Class proxyType, Method method) {
        Signature signature = new Signature(method.getName(), Type.getReturnType(method), Type.getArgumentTypes(method));
        MethodProxy methodProxy = MethodProxy.find(proxyType, signature);
        if (methodProxy != null) {
            return methodProxy.getSuperIndex();
        }
        return -1;
    }

    static interface ProxyInvoker {
        Object invoke(ObjectName objectName, Object[] arguments) throws Throwable;
    }

    static final class HashCodeInvoke implements ProxyInvoker {
        private final MethodInterceptor methodInterceptor;

        public HashCodeInvoke(MethodInterceptor methodInterceptor) {
            this.methodInterceptor = methodInterceptor;
        }

        public Object invoke(ObjectName objectName, Object[] arguments) throws Throwable {
            return new Integer(methodInterceptor.hashCode());
        }
    }

    static final class EqualsInvoke implements ProxyInvoker {
        private final MethodInterceptor methodInterceptor;

        public EqualsInvoke(MethodInterceptor methodInterceptor) {
            this.methodInterceptor = methodInterceptor;
        }

        public Object invoke(ObjectName objectName, Object[] arguments) throws Throwable {
            return Boolean.valueOf(methodInterceptor.equals(arguments[0]));
        }
    }

    static final class ToStringInvoke implements ProxyInvoker {
        private final String interfaceName;

        public ToStringInvoke(String interfaceName) {
            this.interfaceName = "[" + interfaceName + ": ";
        }

        public Object invoke(ObjectName objectName, Object[] arguments) throws Throwable {
            return interfaceName + objectName + "]";
        }
    }

    static final class RawOperationInvoker implements ProxyInvoker {
        private final RawInvoker rawInvoker;
        private final int methodIndex;

        public RawOperationInvoker(RawInvoker rawInvoker, int methodIndex) {
            this.rawInvoker = rawInvoker;
            this.methodIndex = methodIndex;
        }

        public Object invoke(final ObjectName objectName, final Object[] arguments) throws Throwable {
            return rawInvoker.invoke(methodIndex, arguments);
        }
    }
}
