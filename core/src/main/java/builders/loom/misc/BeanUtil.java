/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.misc;

import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class BeanUtil {

    private BeanUtil() {
    }

    public static void set(final Object bean, final String propertyName, final String propertyValue)
        throws IntrospectionException {

        final List<PropertyDescriptor> propertyDescriptors =
            getPropertyDescriptors(bean);

        final Method setter = findSetter(propertyDescriptors, propertyName)
            .orElseThrow(() -> new IllegalStateException(
                String.format("No property %s found. Available properties are: %s", propertyName,
                    propertyDescriptors.stream()
                        .map(FeatureDescriptor::getName).collect(Collectors.toList()))));

        if (setter.getParameterCount() != 1) {
            throw new IllegalStateException("Setter " + setter + " expected to has exactly one "
                + "parameter");
        }

        try {
            final Class<?> parameterClass = setter.getParameterTypes()[0];
            setter.invoke(bean, parseValue(propertyValue, parameterClass));
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(
                String.format("Error calling %s with args %s", setter, propertyValue), e);
        }
    }

    @SuppressWarnings({"checkstyle:returncount", "checkstyle:npathcomplexity",
        "checkstyle:cyclomaticcomplexity"})
    private static Object parseValue(final String propertyValue, final Class<?> parameterClass) {
        if (parameterClass.isAssignableFrom(boolean.class)) {
            return Boolean.parseBoolean(propertyValue);
        }
        if (parameterClass.isAssignableFrom(Boolean.class)) {
            return Boolean.valueOf(propertyValue);
        }
        if (parameterClass.isAssignableFrom(byte.class)) {
            return Byte.parseByte(propertyValue);
        }
        if (parameterClass.isAssignableFrom(Byte.class)) {
            return Byte.valueOf(propertyValue);
        }
        if (parameterClass.isAssignableFrom(short.class)) {
            return Short.parseShort(propertyValue);
        }
        if (parameterClass.isAssignableFrom(Short.class)) {
            return Short.valueOf(propertyValue);
        }
        if (parameterClass.isAssignableFrom(int.class)) {
            return Integer.parseInt(propertyValue);
        }
        if (parameterClass.isAssignableFrom(Integer.class)) {
            return Integer.valueOf(propertyValue);
        }
        if (parameterClass.isAssignableFrom(long.class)) {
            return Long.parseLong(propertyValue);
        }
        if (parameterClass.isAssignableFrom(Long.class)) {
            return Long.valueOf(propertyValue);
        }
        if (parameterClass.isAssignableFrom(float.class)) {
            return Float.parseFloat(propertyValue);
        }
        if (parameterClass.isAssignableFrom(Float.class)) {
            return Float.valueOf(propertyValue);
        }
        if (parameterClass.isAssignableFrom(double.class)) {
            return Double.parseDouble(propertyValue);
        }
        if (parameterClass.isAssignableFrom(Double.class)) {
            return Double.valueOf(propertyValue);
        }

        return propertyValue;
    }

    private static List<PropertyDescriptor> getPropertyDescriptors(final Object bean)
        throws IntrospectionException {

        final BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());

        return Arrays.stream(beanInfo.getPropertyDescriptors())
            .filter(pd -> pd.getWriteMethod() != null)
            .collect(Collectors.toList());
    }

    private static Optional<Method> findSetter(final List<PropertyDescriptor> propertyDescriptors,
                                               final String propertyName) {
        return propertyDescriptors.stream()
            .filter(pd -> pd.getName().equals(propertyName))
            .findFirst()
            .map(PropertyDescriptor::getWriteMethod);
    }

}
