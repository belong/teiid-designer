/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.designer.runtime.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.internal.JBossServer;
import org.jboss.ide.eclipse.as.core.server.internal.v7.JBoss7Server;
import org.teiid.designer.runtime.DqpPlugin;
import org.teiid.designer.runtime.TeiidServerFactory;
import org.teiid.designer.runtime.TeiidServerFactory.ServerOptions;
import org.teiid.designer.runtime.spi.ITeiidAdminInfo;
import org.teiid.designer.runtime.spi.ITeiidJdbcInfo;
import org.teiid.designer.runtime.spi.ITeiidServer;
import org.teiid.designer.runtime.spi.ITeiidServerManager;
import org.teiid.designer.runtime.version.spi.TeiidServerVersion;

/**
 * Adapter factory that can adapt an {@link IServer} to a {@link ITeiidServer}
 * 
 * @since 8.0
 */
public class TeiidServerAdapterFactory implements IAdapterFactory {
    
    private Object lock = new Object();
    private ITeiidServerManager serverManager;
    
    @Override
    public Class[] getAdapterList() {
        return new Class[] { ITeiidServer.class };
    }

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (! ITeiidServer.class.isAssignableFrom(adapterType))
            return null;
                
        try {
            if (adaptableObject instanceof IServer) {
                return adaptServer((IServer) adaptableObject, ServerOptions.ADD_TO_REGISTRY, ServerOptions.CONNECT);
            }
        } catch (Exception ex) {
            DqpPlugin.Util.log(IStatus.ERROR, ex, "Failed to determine if server supports teiid"); //$NON-NLS-1$
        }
        
        return null;
    }
    
    private ITeiidServerManager getTeiidServerManager() {
        if (serverManager == null)
            serverManager = DqpPlugin.getInstance().getServerManager();
        
        return serverManager;
    }
    
    /**
     * Adapt the given server to an {@link ITeiidServer} passing in any {@link ServerOptions}
     * if required
     * 
     * @param server
     * @param options 
     * 
     * @return {@link ITeiidServer} or null
     */
    public ITeiidServer adaptServer(final IServer server, ServerOptions... options) {
        JBoss7Server jb7 = (JBoss7Server) server.loadAdapter(JBoss7Server.class, null);
        if (jb7 != null) {
            return adaptJBoss7Server(server, jb7, options);
        } else {
            JBossServer jb = (JBossServer) server.loadAdapter(JBossServer.class, null);
            if (jb != null)
                return adaptJBossServer(server, jb, options);
        }
        
        return null;
    }
    
    /**
     * Adapt the older JBoss Server to an {@link ITeiidServer} only if
     * JB is started and contains a Teiid Instance
     * 
     * @param jbossServer
     * @param options
     * 
     * @return
     */
    private ITeiidServer adaptJBossServer(IServer parentServer, JBossServer jbossServer, ServerOptions... options) {
        ITeiidServer teiidServer = null;
        
        List<ServerOptions> optionList = Collections.emptyList(); 
        if (options != null)
            optionList = Arrays.asList(options);
        
        /* 
         * In some cases we want to return a new Teiid Instance even if its in the registry
         * Such Teiid Instances should be disposed of and not kept around.
         */
        if (! optionList.contains(ServerOptions.NO_CHECK_SERVER_REGISTRY)) {
            // No specific option specifying we should not check the registry so check
            // and return any Teiid Instance found that fits the url
         
            
            teiidServer = getTeiidServerManager().getServer(parentServer);
            if (teiidServer != null)
                return teiidServer;
        }
        
        synchronized (lock) {
            if (! optionList.contains(ServerOptions.NO_CHECK_CONNECTION)) {
                if (! JBossServerUtil.isJBossServerConnected(parentServer, jbossServer))
                    return null;
            
                if (! JBossServerUtil.isTeiidServer(parentServer, jbossServer))
                    return null;
            }

            // Check again in case the thread had to wait for the lock
            if (! optionList.contains(ServerOptions.NO_CHECK_SERVER_REGISTRY)) {
                teiidServer = getTeiidServerManager().getServer(parentServer);
            }
            
            if (teiidServer == null)
                teiidServer = createJbossTeiidServer(parentServer, jbossServer, options);
        
            return teiidServer;
        }
    }

    /**
     * Adapt the JBoss 7 server to an {@link ITeiidServer} only if
     * JB is started and contains a Teiid Instance.
     * 
     * @param parentServer
     * @param jboss7Server
     * @param options
     * 
     * @return
     */
    private ITeiidServer adaptJBoss7Server(IServer parentServer, JBoss7Server jboss7Server, ServerOptions... options) {
        ITeiidServer teiidServer = null;
        
        List<ServerOptions> optionList = Collections.emptyList(); 
        if (options != null)
            optionList = Arrays.asList(options);
        
        /* 
         * In some cases we want to return a new Teiid Instance even if its in the registry
         * Such Teiid Instances should be disposed of and not kept around.
         */
        if (! optionList.contains(ServerOptions.NO_CHECK_SERVER_REGISTRY)) {
            // No specific option specifying we should not check the registry so check
            // and return any Teiid Instance found that fits the url
         
            
            teiidServer = getTeiidServerManager().getServer(parentServer);
            if (teiidServer != null)
                return teiidServer;
        }
        
        synchronized (lock) {
            if (! optionList.contains(ServerOptions.NO_CHECK_CONNECTION)) {
                if (! JBoss7ServerUtil.isJBossServerConnected(parentServer, jboss7Server))
                    return null;
            
                if (! JBoss7ServerUtil.isTeiidServer(parentServer, jboss7Server))
                    return null;
            }

            // Check again in case the thread had to wait for the lock
            if (! optionList.contains(ServerOptions.NO_CHECK_SERVER_REGISTRY)) {
                teiidServer = getTeiidServerManager().getServer(parentServer);
            }
            
            if (teiidServer == null)
                teiidServer = createJboss7TeiidServer(parentServer, jboss7Server, options);
        
            return teiidServer;
        }
    }
    
    /**
     * Create a new {@link ITeiidServer}.
     * 
     * @see ServerOptions for the options parameter.
     * 
     * @param parentServer
     * @param jboss7Server
     * @param options
     * 
     * @return new {@link ITeiidServer}
     */
    private ITeiidServer createJboss7TeiidServer(final IServer parentServer, final JBoss7Server jboss7Server, ServerOptions... options) {
        TeiidServerFactory factory = new TeiidServerFactory();
        ITeiidServer teiidServer = factory.createTeiidServer(JBoss7ServerUtil.getTeiidRuntimeVersion(parentServer, jboss7Server),
                                                                                        getTeiidServerManager(),
                                                                                        parentServer, 
                                                                                        new Integer(jboss7Server.getManagementPort()).toString(), 
                                                                                        jboss7Server.getUsername(), 
                                                                                        jboss7Server.getPassword(), 
                                                                                        JBoss7ServerUtil.getJdbcPort(parentServer, jboss7Server), 
                                                                                        ITeiidJdbcInfo.DEFAULT_JDBC_USERNAME, 
                                                                                        ITeiidJdbcInfo.DEFAULT_JDBC_PASSWORD, 
                                                                                        options);
       
        return teiidServer;
    }
    
    /**
     * Create a new {@link ITeiidServer}.
     * 
     * @see ServerOptions for the options parameter.
     * 
     * @param parentServer
     * @param jbossServer
     * @param options
     * 
     * @return new {@link ITeiidServer}
     */
    private ITeiidServer createJbossTeiidServer(final IServer parentServer, final JBossServer jbossServer, ServerOptions... options) {
        
        List<ServerOptions> optionList = new ArrayList<ServerOptions>();
        if (options != null) {
            for (ServerOptions serverOption : options) {
                optionList.add(serverOption);
            }
        }
        
        // Default teiid instances prior to 8.x had the admin connection secured
        optionList.add(ServerOptions.ADMIN_SECURE_CONNECTION);
        
        TeiidServerFactory factory = new TeiidServerFactory();
        ITeiidServer teiidServer = factory.createTeiidServer(TeiidServerVersion.DEFAULT_TEIID_7_SERVER,
                                                                                        getTeiidServerManager(),
                                                                                        parentServer, 
                                                                                        ITeiidAdminInfo.DEFAULT_LEGACY_PORT, 
                                                                                        ITeiidAdminInfo.DEFAULT_ADMIN_USERNAME,
                                                                                        ITeiidAdminInfo.DEFAULT_ADMIN_PASSWORD, 
                                                                                        ITeiidJdbcInfo.DEFAULT_PORT, 
                                                                                        ITeiidJdbcInfo.DEFAULT_JDBC_USERNAME, 
                                                                                        ITeiidJdbcInfo.DEFAULT_JDBC_PASSWORD, 
                                                                                        optionList.toArray(new ServerOptions[0]));
       
        return teiidServer;
    }

    /**
     * @param server
     * 
     * @return true if connected
     */
    public boolean isParentServerConnected(IServer server) {
        if (server.getServerState() != IServer.STATE_STARTED)
            return false;
        
        JBoss7Server jb7 = (JBoss7Server) server.loadAdapter(JBoss7Server.class, null);
        if (jb7 != null) {
            return JBoss7ServerUtil.isJBossServerConnected(server, jb7);
        } else {
            JBossServer jb = (JBossServer) server.loadAdapter(JBossServer.class, null);
            if (jb != null)
                return JBossServerUtil.isJBossServerConnected(server, jb);
        }
        
        return false;
    }
}
