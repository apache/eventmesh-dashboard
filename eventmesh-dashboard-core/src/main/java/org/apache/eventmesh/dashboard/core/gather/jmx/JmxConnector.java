/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.eventmesh.dashboard.core.gather.jmx;

import javax.management.remote.JMXConnector;

public class JmxConnector {

    private JMXConnector jmxConnector;

    private void createJmxConnector() {
    /*        String jmxUrl = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", jmxHost, jmxPort);
        try {
            Map<String, Object> environment = new HashMap<String, Object>();
            if (!ValidateUtils.isBlank(this.jmxConfig.getUsername()) && !ValidateUtils.isBlank(this.jmxConfig.getToken())) {
                // fixed by riyuetianmu
                environment.put(JMXConnector.CREDENTIALS, new String[] {this.jmxConfig.getUsername(), this.jmxConfig.getToken()});
            }
            if (jmxConfig.getOpenSSL() != null && this.jmxConfig.getOpenSSL()) {
                environment.put(Context.SECURITY_PROTOCOL, "ssl");
                SslRMIClientSocketFactory clientSocketFactory = new SslRMIClientSocketFactory();
                environment.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, clientSocketFactory);
                environment.put("com.sun.jndi.rmi.factory.socket", clientSocketFactory);
            }

            jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl), environment);
            LOGGER.info(
                "method=createJmxConnector||clientLogIdent={}||jmxHost={}||jmxPort={}||msg=jmx connect success.",
                clientLogIdent, jmxHost, jmxPort
            );
            return true;
        }catch (Exception e){

        }*/
    }
}
