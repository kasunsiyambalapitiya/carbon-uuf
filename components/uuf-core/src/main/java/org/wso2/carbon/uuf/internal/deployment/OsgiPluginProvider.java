/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.uuf.internal.deployment;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.uuf.internal.exception.PluginLoadingException;

import java.util.HashMap;
import java.util.Map;

/**
 * Plugins provider for plugins that are OSGi services.
 *
 * @since 1.0.0
 */
@Component(name = "org.wso2.carbon.uuf.internal.deployment.OsgiPluginProvider",
           service = PluginProvider.class,
           immediate = true,
           property = {
                   "componentName=wso2-uuf-OSGi-plugin-provider"
           }
)
public class OsgiPluginProvider implements PluginProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsgiPluginProvider.class);
    private final Map<Class, ServiceTracker> serviceTrackers = new HashMap<>();
    private BundleContext bundleContext = null;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        LOGGER.debug("OsgiPluginProvider activated.");
    }

    @Deactivate
    protected void deactivate(BundleContext bundleContext) {
        serviceTrackers.values().forEach(ServiceTracker::close);
        serviceTrackers.clear();
        this.bundleContext = null;
        LOGGER.debug("OsgiPluginProvider deactivated.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getPluginInstance(Class<T> type, String className, ClassLoader classLoader)
            throws PluginLoadingException {
        T plugin = loadOsgiPlugin(type, className);
        return (plugin != null) ? plugin : loadNonOsgiPlugin(type, className, classLoader);
    }

    private <T> T loadOsgiPlugin(Class<T> type, String className) {
        if (bundleContext == null) {
            return null;
        }

        ServiceTracker<T, T> serviceTracker = getServiceTracker(type);
        @SuppressWarnings("unchecked")
        T[] services = serviceTracker.getServices((T[]) new Object[serviceTracker.size()]);
        for (T service : services) {
            if ((service != null) && (service.getClass().getName().equals(className))) {
                return service;
            }
        }

        return null;
    }

    private <T> T loadNonOsgiPlugin(Class<T> type, String className, ClassLoader classLoader) {
        Object pluginInstance;
        try {
            pluginInstance = classLoader.loadClass(className).newInstance();
        } catch (ClassNotFoundException e) {
            throw new PluginLoadingException(
                    "Cannot load plugin '" + className + "' via the class loader '" + classLoader + "'.", e);

        } catch (IllegalAccessException | InstantiationException | SecurityException e) {
            throw new PluginLoadingException(
                    "Cannot instantiation plugin '" + className + "' via the class loader '" + classLoader + "'.", e);
        }

        try {
            return type.cast(pluginInstance);
        } catch (ClassCastException e) {
            throw new PluginLoadingException(
                    "Plugin '" + className + "' is not a sub class of the plugin type '" + type.getName() + "'.", e);
        }
    }

    private <T> ServiceTracker<T, T> getServiceTracker(Class<T> serviceClass) {
        @SuppressWarnings("unchecked")
        ServiceTracker<T, T> serviceTracker = serviceTrackers.get(serviceClass);
        if (serviceTracker == null) {
            serviceTracker = new ServiceTracker<>(bundleContext, serviceClass, null);
            serviceTracker.open();
            serviceTrackers.put(serviceClass, serviceTracker);
        }
        return serviceTracker;
    }
}
