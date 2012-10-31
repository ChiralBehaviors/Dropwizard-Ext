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

import org.eclipse.jetty.server.Server;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Configuration;

/**
 * @author hhildebrand
 * 
 */
abstract public class JettyRetainingService<T extends Configuration> extends
        Service<T> {

    protected JettyRetainingService() {
        super();
        addCommand(new JettyRetainingServerCommand<>(getConfigurationClass()));
    }

    protected JettyRetainingService(String name) {
        super(name);
        addCommand(new JettyRetainingServerCommand<>(getConfigurationClass()));
    }

    /**
     * Set the Jetty server. The server has been intialized and started.
     * 
     * @param server
     */
    abstract protected void setServer(Server server);
}
