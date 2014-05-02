/** 
 * (C) Copyright 2012 Hal Hildebrand, All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.hellblazer.dropwizard;

import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.setup.Environment;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hellblazer.slp.ServiceScope;
import com.hellblazer.slp.ServiceType;
import com.hellblazer.slp.ServiceURL;

/**
 * @author hhildebrand
 * 
 */
public class ServiceRegistrar implements ServerLifecycleListener {
    public static enum ENDPOINT {
        INTERNAL {
            @Override
            public boolean matches(Connector connector) {
                return "internal".equals(connector.getName());
            }
        },
        MAIN {
            @Override
            public boolean matches(Connector connector) {
                return "main".equals(connector.getName());
            }
        };

        abstract public boolean matches(Connector connector);
    }

    public static class Registration {
        public final ENDPOINT            endpoint;
        public final Map<String, String> properties;
        public final ServiceType         serviceType;
        public final String              urlPath;

        /**
         * @param endpoint
         * @param serviceType
         * @param urlPath
         */
        @SuppressWarnings("unchecked")
        public Registration(ENDPOINT endpoint, ServiceType serviceType,
                            String urlPath, Map<String, String> properties) {
            assert endpoint != null : "endpoint must not be null";
            this.endpoint = endpoint;
            assert serviceType != null : "service type must not be null";
            this.serviceType = serviceType;
            if (urlPath == null) {
                urlPath = "/";
            }
            if (!urlPath.startsWith("/")) {
                urlPath = "/" + urlPath;
            }
            this.urlPath = urlPath;
            if (properties == null) {
                properties = Collections.EMPTY_MAP;
            }
            this.properties = properties;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return String.format("Registration[%s:%s:%s:%s]", serviceType,
                                 endpoint, urlPath, properties);
        }
    }

    private static Logger              log           = LoggerFactory.getLogger(ServiceRegistrar.class);

    protected final Environment        environment;
    protected final List<Registration> registrations = new ArrayList<>();
    protected final ServiceScope       scope;

    public ServiceRegistrar(ServiceScope scope, Environment environment)
                                                                        throws Exception {
        this.scope = scope;
        this.environment = environment;
    }

    public void add(Registration registration) {
        registrations.add(registration);
    }

    /* (non-Javadoc)
     * @see com.yammer.dropwizard.lifecycle.ServerLifecycleListener#serverStarted()
     */
    @Override
    public void serverStarted(Server server) {
        InetSocketAddress main = null;
        InetSocketAddress internal = null;
        for (Connector connector : server.getConnectors()) {
            if (ENDPOINT.INTERNAL.matches(connector)) {
                try {
                    internal = endpointOf(connector);
                } catch (UnknownHostException e) {
                    log.error(String.format("Cannot determine internal endpoint address %s",
                                            connector));
                }
            } else if (ENDPOINT.MAIN.matches(connector)) {
                try {
                    main = endpointOf(connector);
                } catch (UnknownHostException e) {
                    log.error(String.format("Cannot determine main endpoint address %s",
                                            connector));
                    return;
                }
            }
        }
        try {
            register(main, internal);
        } catch (UnknownHostException | MalformedURLException
                | URISyntaxException e) {
            log.error(String.format("Error registering services for endpoints %s, %s",
                                    main, internal), e);
        }
    }

    protected InetSocketAddress endpointOf(Connector connector)
                                                               throws UnknownHostException {
        for (EndPoint endpoint : connector.getConnectedEndPoints()) {
            InetSocketAddress address = endpoint.getLocalAddress();
            String host = address.getHostName() == null ? InetAddress.getLocalHost().getCanonicalHostName()
                                                       : address.getHostName();
            return new InetSocketAddress(host, address.getPort());
        }
        throw new IllegalStateException(
                                        String.format("No connected endpoints on: %s",
                                                      connector));
    }

    protected void register(InetSocketAddress main, InetSocketAddress internal)
                                                                               throws UnknownHostException,
                                                                               MalformedURLException,
                                                                               URISyntaxException {
        for (Registration registration : registrations) {
            switch (registration.endpoint) {
                case INTERNAL:
                    register(internal, registration);
                    break;
                case MAIN:
                    register(main, registration);
                    break;
            }
        }
    }

    /**
     * @param main
     * @param registration
     * @throws UnknownHostException
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    protected void register(InetSocketAddress endpoint,
                            Registration registration)
                                                      throws UnknownHostException,
                                                      MalformedURLException,
                                                      URISyntaxException {
        String host = endpoint.getAddress().isAnyLocalAddress() ? InetAddress.getLocalHost().getCanonicalHostName()
                                                               : endpoint.getHostName();
        URL base = new URL(registration.serviceType.getConcreteTypeName(),
                           host, endpoint.getPort(), registration.urlPath);

        ServiceURL service = new ServiceURL(registration.serviceType, base);
        log.info(String.format("Registering service %s", service));
        Map<String, String> properties = new HashMap<>();
        scope.register(service, properties);
    }
}
