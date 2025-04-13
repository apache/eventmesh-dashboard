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
