/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.designer.runtime.adapter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.internal.JBossServer;
import org.teiid.designer.runtime.DqpPlugin;

/**
 *
 */
public class JBossServerUtil {

    /**
     * Determine whether the jboss server is contactable by attempting
     * to create a socket to its web port
     * 
     * @param parentServer
     * @param jbossServer
     * 
     * @return true if server can be connected
     */
    public static boolean isJBossServerConnected(IServer parentServer, JBossServer jbossServer) {
        if (!serverStarted(parentServer)) 
            return false;

        return isHostConnected(jbossServer.getHost(), jbossServer.getJBossWebPort());
    }

    /**
     * @param jbossServer
     * @return
     */
    protected static boolean isHostConnected(String host, int port) {
        Socket socket = null;
        InetSocketAddress endPoint = new InetSocketAddress(host, port);

        if (endPoint.isUnresolved()) {
            DqpPlugin.Util.log(IStatus.WARNING, DqpPlugin.Util.getString("jbossServerConnectionFailureMessage", endPoint)); //$NON-NLS-1$
            return false;
        }

        try {
            socket = new Socket();
            socket.connect(endPoint, 1024);

            return true;
        } catch (Exception ex) {
            // Connection failed - no need to log exception
            return false;
        } finally {
            if (socket != null && socket.isConnected()) {
                try {
                    socket.close();
                    socket = null;
                } catch (IOException ex) {
                    DqpPlugin.Util.log(IStatus.WARNING, ex, DqpPlugin.Util.getString("jbossServerConnectionFailureMessage", endPoint)); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Determine whether the given server has teiid support
     * 
     * @param parentServer
     * @param jbossServer
     * 
     * @return true is server has teiid support, false otherwise
     */
    public static boolean isTeiidServer(IServer parentServer, JBossServer jbossServer) {
        if (!serverStarted(parentServer)) 
            return false;

        //TODO can we try harder to determine this??
       
        return true;
    }

    /**
     * Test the given server for whether its been started
     * 
     * @param server
     * 
     * @return true is server is in the started state
     */
    protected static boolean serverStarted(IServer server) {
        if (server == null) 
            return false;

        if (server.getServerState() != IServer.STATE_STARTED) 
            return false;

        return true;
    }
}
