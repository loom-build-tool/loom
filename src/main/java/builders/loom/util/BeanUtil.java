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

package builders.loom.util;

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

import builders.loom.api.PluginSettings;

public class BeanUtil {

    public static void set(final String plugin, final PluginSettings pluginSettings, final String propertyName, final String propertyValue) {

        final List<PropertyDescriptor> propertyDescriptors =
            getPropertyDescriptors(plugin, pluginSettings);


        final Method setter = findSetter(propertyDescriptors, propertyName)
            .orElseThrow(() -> new IllegalStateException(
                String.format("No property %s found in plugin %s. Available properties are: %s",
                    propertyName, plugin, propertyDescriptors.stream()
                        .map(FeatureDescriptor::getName).collect(Collectors.toList()))));

        if (setter.getParameterCount() != 1) {
            throw new IllegalStateException("Setter " + setter + " expected to has exactly one parameter");
        }

        try {
            Class<?> parameterClass = setter.getParameterTypes()[0];
            if (parameterClass.isAssignableFrom(boolean.class)) {
                setter.invoke(pluginSettings, propertyValue.equalsIgnoreCase("true"));
            } else {
                setter.invoke(pluginSettings, propertyValue);
            }
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(
                String.format("Error calling %s with args %s on plugin %s",
                    setter, propertyValue, plugin), e);
        }

    }

    private static List<PropertyDescriptor> getPropertyDescriptors(
        final String plugin, final PluginSettings pluginSettings) {

        final BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(pluginSettings.getClass());
        } catch (final IntrospectionException e) {
            throw new IllegalStateException("Can't inspect plugin " + plugin, e);
        }

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
