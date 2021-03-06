/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.runtime.ui.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import net.jcip.annotations.GuardedBy;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.as.wst.server.ui.xpl.ServerToolTip;
import org.teiid.designer.runtime.DqpPlugin;
import org.teiid.designer.runtime.spi.ExecutionConfigurationEvent;
import org.teiid.designer.runtime.spi.IExecutionConfigurationListener;
import org.teiid.designer.runtime.spi.ITeiidServer;
import org.teiid.designer.runtime.ui.DqpUiConstants;
import org.teiid.designer.runtime.ui.server.RuntimeAssistant;
import org.teiid.designer.runtime.ui.views.content.AbstractTeiidFolder;
import org.teiid.designer.runtime.ui.views.content.DataSourcesFolder;
import org.teiid.designer.runtime.ui.views.content.ITeiidContainerNode;
import org.teiid.designer.runtime.ui.views.content.ITeiidContentNode;
import org.teiid.designer.runtime.ui.views.content.ITeiidResourceNode;
import org.teiid.designer.runtime.ui.views.content.TeiidDataNode;
import org.teiid.designer.runtime.ui.views.content.TeiidResourceNode;
import org.teiid.designer.runtime.ui.views.content.TranslatorsFolder;
import org.teiid.designer.runtime.ui.views.content.VdbsFolder;
import org.teiid.designer.ui.common.util.UiUtil;
import org.teiid.designer.ui.viewsupport.ModelerUiViewUtils;


/**
 * Class provides content and label information for ConnectorBindings and ModelInfos in ConnectorsView
 * 
 * @since 8.0
 */
public class TeiidServerContentProvider implements ICommonContentProvider {

    /** Represents a pending request in the tree. */
    private static final String LOAD_ELEMENT_JOB = DqpUiConstants.UTIL.getString(TeiidServerContentProvider.class.getSimpleName() + ".jobName"); //$NON-NLS-1$
    private static final Object PENDING = new Object();

    private ConcurrentMap<ITeiidContainerNode, Object> pendingUpdates = new ConcurrentHashMap<ITeiidContainerNode, Object>();
    private transient TreeViewer viewer;

    /**
     * Servers that a connection can't be established. Value is the last time establishing a connection was tried.
     */
    @GuardedBy( "offlineServersLock" )
    private final Map<ITeiidServer, Long> offlineServerMap = new HashMap<ITeiidServer, Long>();
    
    private boolean showVDBs = true;
    private boolean showDataSources = true;
    private boolean showTranslators = true;
    
    private class RefreshThread extends Thread {
        
        private boolean die = false;
        
        private LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
        
        public RefreshThread() {
            super(TeiidServerContentProvider.this + "." + RefreshThread.class.getSimpleName()); //$NON-NLS-1$
            setDaemon(true);
        }
        

        /**
         * End the thread
         */
        public void die() {
            die = true;
        }
        
        public void refresh() {
            if (die)
                return;
            
            queue.add(new Object());
        }
        
        @Override
        public void run() {
            while (! Thread.currentThread().isInterrupted() && ! die) {
                try {
                    if (pendingUpdates.size() == 0) {
                        queue.take();
                        
                        // Successfully taken from the queue so do the refresh
                        doRefreshWork();
                    }
                    
                    Thread.yield();
                } catch (InterruptedException inte) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        private void doRefreshWork() {
            UiUtil.runInSwtThread(new Runnable() {
                @Override
                public void run() {
                    if (viewer.getTree().isDisposed())
                        return;

                    TreePath[] expandedElements = viewer.getExpandedTreePaths();

                    // Refresh the viewer
                    viewer.refresh();

                    // Re-expand the viewer
                    reExpand(expandedElements);
                }

            }, false);
        }
    }
    
    /**
     * Loads content for specified nodes, then refreshes the content in the
     * tree.
     */
    private Job loadElementJob = new Job(LOAD_ELEMENT_JOB) {
        
        @Override
        public boolean shouldRun() {
            return pendingUpdates.size() > 0;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            monitor.beginTask(LOAD_ELEMENT_JOB, IProgressMonitor.UNKNOWN);
            try {
                final List<ITeiidContainerNode> updated = new ArrayList<ITeiidContainerNode>(pendingUpdates.size());
                for (ITeiidContainerNode node : pendingUpdates.keySet()) {
                    try {
                        node.load();
                        updated.add(node);
                    } catch (Exception e) {
                    }
                    if (monitor.isCanceled()) {
                        pendingUpdates.keySet().removeAll(updated);
                        return Status.CANCEL_STATUS;
                    }
                }
                
                if (viewer == null) {
                    pendingUpdates.keySet().clear();
                } else {
                    
                    UiUtil.runInSwtThread(new Runnable() {
                        
                        @Override
                        public void run() {
                            if (viewer.getControl().isDisposed())
                                return;
                            
                            for (Object node : updated) {
                                pendingUpdates.remove(node);
                                viewer.refresh(node);
                                
                                if (node instanceof ITeiidResourceNode) {
                                    viewer.setExpandedState(node, true);
                                    viewer.expandToLevel(node, 2);
                                }
                            }
                        }
                    }, true);
                }
            } finally {
                monitor.done();
            }
            return Status.OK_STATUS;
        }
    };
    
    private IExecutionConfigurationListener configListener = new IExecutionConfigurationListener() {
        
        @Override
        public void configurationChanged( final ExecutionConfigurationEvent event ) {
            refreshThread.refresh();
        }
    };

    /**
     * Listener that responds to property changes of the previewOptionContributor
     */
    private PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (TeiidServerPreviewOptionContributor.PREVIEW_OPTIONS_PROPERTY.equals(evt.getPropertyName())) {

                if (viewer == null || viewer.isBusy())
                    return;

                TreePath[] expandedElements = viewer.getExpandedTreePaths();
                viewer.refresh();
                reExpand(expandedElements);
            }
        }
    };

    /**
     * Determines the display of the preview data source and vdb options
     */
    private final TeiidServerPreviewOptionContributor previewOptionContributor;

    private RefreshThread refreshThread = new RefreshThread();
    
    private ServerToolTip tooltip;
    
    /**
     * Content will include VDBs, translators, and data sources.
     * @since 5.0
     */
    public TeiidServerContentProvider() {
        super();

        refreshThread.start();

        // Wire as listener to server manager and to receive configuration changes
        DqpPlugin.getInstance().getServerManager().addListener(configListener);

        // Wire as a listener to the default preview options contributor
        previewOptionContributor = TeiidServerPreviewOptionContributor.getDefault();
        previewOptionContributor.addPropertyChangeListener(propertyChangeListener);
    }

    /**
     * Used for display of server content in dialogs
     *
     * @param showVDBs 
     * @param showTranslators 
     * @param showDataSources 
     * 
     * @since 5.0
     */
    public TeiidServerContentProvider( boolean showVDBs,
                                  boolean showTranslators,
                                  boolean showDataSources ) {
        this();
        this.showVDBs = showVDBs;
        this.showTranslators = showTranslators;
        this.showDataSources = showDataSources;
    }
    
    /**
     * @return whether to show data sources
     */
    public boolean isShowDataSources() {
        return this.showDataSources;
    }

    /**
     * @return whether to show preview data sources
     */
    public boolean isShowPreviewDataSources() {
        return previewOptionContributor.isShowPreviewDataSources();
    }

    /**
     * @return whether to show vdbs
     */
    public boolean isShowVDBs() {
        return this.showVDBs;
    }

    /**
     * @return whether to show preview vdbs
     */
    public boolean isShowPreviewVDBs() {
        return previewOptionContributor.isShowPreviewVdbs();
    }

    /**
     * @return the showTranslators
     */
    public boolean isShowTranslators() {
        return this.showTranslators;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     * @since 4.2
     */
    @Override
    public Object[] getChildren( Object parentElement ) {
        if (parentElement == null)
            return new Object[0];
        
        if (parentElement instanceof IServer) {
            ITeiidResourceNode node = TeiidResourceNode.getInstance((IServer) parentElement, this);
            node.setDirty();
            return new Object[] { node };
            
        } else if (parentElement instanceof ITeiidContainerNode) {
            ITeiidContainerNode<?> container = (ITeiidContainerNode<?>) parentElement;
            if (pendingUpdates.containsKey(container)) {
                return new Object[] { PENDING };
            }
            List<? extends ITeiidContentNode<?>> children = container.getChildren();
            if (children == null) {
                pendingUpdates.putIfAbsent(container, PENDING);
                loadElementJob.schedule();
                return new Object[] { PENDING };
            }
            return children.toArray();
        }

        return new Object[0];
    }

    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     * @since 4.2
     */
    @Override
    public Object[] getElements( Object inputElement ) {
        return getChildren(inputElement);
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     * @since 4.2
     */
    @Override
    public Object getParent( Object element ) {
        if (element instanceof ITeiidContentNode) {
            Object parent = ((ITeiidContentNode<?>) element).getContainer();
            if (parent == null) {
                parent = ((ITeiidContentNode<?>) element).getServer();
            }
            return parent;
        }
        return null;
    }

    /**
     * @param server the server whose Data Source folder is being requested
     * @return the folder
     */
    public Object getDataSourceFolder(Object server) {
        for (Object child : getChildren(server)) {
            if (child instanceof DataSourcesFolder) {
                return child;
            }
        }
        
        return null;
    }
    
    /**
     * @param server the server whose Translators folder is being requested
     * @return the folder
     */
    public Object getTranslatorFolder(Object server) {
        for (Object child : getChildren(server)) {
            if (child instanceof TranslatorsFolder) {
                return child;
            }
        }
        
        return null;
    }
    
    /**
     * @param server the server whose VDBs folder is being requested
     * @return the folder
     */
    public Object getVdbFolder(Object server) {
        for (Object child : getChildren(server)) {
            if (child instanceof VdbsFolder) {
                return child;
            }
        }
        
        return null;
    }
    
    static Object getPending() {
        return PENDING;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     * @since 4.2
     */
    @Override
    public boolean hasChildren( Object element ) {
        if (element instanceof IServer) {
            return true;
        } else if (element instanceof ITeiidContainerNode) {
            return true;
        }
        
        return false;
    }

    private boolean isTeiidResourceNode(Object o) {
        return o instanceof ITeiidResourceNode;
    }

    /**
     * Re-expand the tree if it was previously expanded.
     *
     * If the TeiidResourceNode is expanded then we will need to
     * re-expand it since it has now been refreshed.
     *
     * If not found, then the node was never expanded so nothing
     * need be done.
     *
     * Requires the current selection prior to a refresh in order to try and
     * re-apply it once the nodes have been re-expanded.
     */
    private void reExpand(TreePath[] expandedElements) {
        ITeiidResourceNode trn = null;

        for (TreePath o : expandedElements) {
            Object element = o.getLastSegment();

            if (isTeiidResourceNode(element)) {
                trn = (ITeiidResourceNode) element;
                break;
            }

            Object[] children = getChildren(element);
            for (Object child : children) {
                if (isTeiidResourceNode(child)) {
                    trn = (ITeiidResourceNode) child;
                    break;
                }
            }

            if (isTeiidResourceNode(trn)) {
                break;
            }
        }

        // Re-expand the TeiidResourceNode if
        // it was expanded previously
        if (trn != null) {
            viewer.setExpandedState(trn, true);
            viewer.expandToLevel(trn, 2);
        }

        // Refresh the Model Explorer too
        ModelerUiViewUtils.refreshModelExplorerResourceNavigatorTree();
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object,
     *      java.lang.Object)
     * @since 4.2
     */
    @Override
    public void init(ICommonContentExtensionSite aConfig) {
        /*
         * Restore state does not get called conventionally since the provider
         * is an adjunct to the viewer so call in manually when it is initialised.
         */
        restoreState(aConfig.getMemento());
    }

    @Override
    public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
        if (viewer instanceof TreeViewer) {
            this.viewer = (TreeViewer) viewer;

            if( tooltip != null )
                tooltip.deactivate();
            
            tooltip = new ServerToolTip(this.viewer.getTree()) {
                
                @Override
                protected boolean isMyType(Object selected) {
                    return RuntimeAssistant.adapt(selected, AbstractTeiidFolder.class) != null ||
                        RuntimeAssistant.adapt(selected, TeiidDataNode.class) != null ||
                        RuntimeAssistant.adapt(selected, ITeiidServer.class) != null;
                }
                @Override
                protected void fillStyledText(Composite parent, StyledText sText, Object o) {
                   if (o instanceof TreeItem) {
                       String text = ((TreeItem) o).getData().toString();
                       sText.setText(text.replace("\n", "<br>"));  //$NON-NLS-1$ //$NON-NLS-2$
                   }
                }
            };
            tooltip.setShift(new Point(15, 8));
            tooltip.setPopupDelay(500); // in ms
            tooltip.setHideOnMouseDown(true);
            tooltip.activate();
        } else {
            this.viewer = null;
        }
    }
    
    @Override
    public void dispose() {
        viewer = null;
        loadElementJob.cancel();
        pendingUpdates.clear();
        
        DqpPlugin.getInstance().getServerManager().removeListener(configListener);
        
        refreshThread.die();
        refreshThread = null;
    }

    @Override
    public void saveState(IMemento memento) {
        previewOptionContributor.saveState(memento);
    }

    @Override
    public void restoreState(IMemento aMemento) {
        previewOptionContributor.restoreState(aMemento);
    }
}
