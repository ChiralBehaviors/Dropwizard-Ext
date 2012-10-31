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

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.eclipse.jetty.server.Server;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.yammer.dropwizard.AbstractService;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.ServerFactory;
import com.yammer.dropwizard.logging.Log;

/**
 * @author hhildebrand
 * 
 */
public class JettyRetainingServerCommand<T extends Configuration> extends
        ConfiguredCommand<T> {
    private final Class<T> configurationClass;
    private Server         server;

    @Override
    protected Class<T> getConfigurationClass() {
        return configurationClass;
    }

    /**
     * @param configurationClass
     */
    public JettyRetainingServerCommand(Class<T> configurationClass) {
        super("server", "Starts an HTTP server running the service");
        this.configurationClass = configurationClass;
    }

    protected void run(AbstractService<T> service, T configuration,
                       CommandLine params) throws Exception {
        final Environment environment = new Environment(service, configuration);
        service.initializeWithBundles(configuration, environment);
        server = new ServerFactory(configuration.getHttpConfiguration(),
                                   service.getName()).buildServer(environment);
        final Log log = Log.forClass(JettyRetainingServerCommand.class);
        logBanner(service, log);
        try {
            server.start();
            server.join();
            if (service instanceof JettyRetainingService) {
                ((JettyRetainingService<T>) service).setServer(server);
            }
        } catch (Exception e) {
            log.error(e, "Unable to start server, shutting down");
            server.stop();
        }
    }

    private void logBanner(AbstractService<T> service, Log log) {
        try {
            final String banner = Resources.toString(Resources.getResource("banner.txt"),
                                                     Charsets.UTF_8);
            log.info("Starting {}\n{}", service.getName(), banner);
        } catch (IllegalArgumentException ignored) {
            // don't display the banner if there isn't one
            log.info("Starting {}", service.getName());
        } catch (IOException ignored) {
            log.info("Starting {}", service.getName());
        }
    }

    public Server getServer() {
        return server;
    }
}
