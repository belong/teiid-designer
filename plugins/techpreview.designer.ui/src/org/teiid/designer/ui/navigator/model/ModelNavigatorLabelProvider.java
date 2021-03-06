/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.ui.navigator.model;

import static org.teiid.designer.ui.UiConstants.Util;
import static org.teiid.designer.ui.navigator.model.ModelNavigatorMessages.genericDiagramLabel;
import static org.teiid.designer.ui.navigator.model.ModelNavigatorMessages.genericTransformationLabel;
import static org.teiid.designer.ui.navigator.model.ModelNavigatorMessages.problemMarkerBrackets;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.IDescriptionProvider;
import org.eclipse.xsd.XSDAttributeUse;
import org.eclipse.xsd.XSDParticle;
import org.teiid.designer.core.ModelerCore;
import org.teiid.designer.extension.ExtensionPlugin;
import org.teiid.designer.extension.definition.ModelExtensionAssistant;
import org.teiid.designer.extension.definition.ModelObjectExtensionAssistant;
import org.teiid.designer.extension.registry.ModelExtensionRegistry;
import org.teiid.designer.metamodels.diagram.Diagram;
import org.teiid.designer.metamodels.transformation.TransformationMappingRoot;
import org.teiid.designer.ui.PluginConstants;
import org.teiid.designer.ui.UiPlugin;
import org.teiid.designer.ui.viewsupport.DiagramLabelProvider;
import org.teiid.designer.ui.viewsupport.ExtendedModelObjectLabelProvider;
import org.teiid.designer.ui.viewsupport.IExtendedModelObject;
import org.teiid.designer.ui.viewsupport.ImportContainer;
import org.teiid.designer.ui.viewsupport.MarkerUtilities;
import org.teiid.designer.ui.viewsupport.ModelIdentifier;
import org.teiid.designer.ui.viewsupport.ModelUtilities;


public class ModelNavigatorLabelProvider extends LabelProvider implements IDescriptionProvider, ILightweightLabelDecorator,
        PluginConstants.Images {

    private final ILabelProvider defaultProvider;
    private final DiagramLabelProvider diagramLabelProvider;
    private final IBaseLabelProvider eventSource;
    private final ExtendedModelObjectLabelProvider extendedModelObjectLabelProvider;
    final ListenerList myListeners = new ListenerList(ListenerList.IDENTITY);
    private final IResourceChangeListener resrcChgListener;

    public ModelNavigatorLabelProvider() {
        this.eventSource = this;
        this.defaultProvider = new WorkbenchLabelProvider();

        // Add listener for model validation completion events
        this.resrcChgListener = new IResourceChangeListener() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
             */
            @Override
            public void resourceChanged( final IResourceChangeEvent event ) {
                final Display display = Display.getDefault();

                if (display.isDisposed()) {
                    return;
                }

                // Don't do any work if ther are no listeners.... :)
                if (myListeners.isEmpty()) {
                    return;
                }

                final IMarkerDelta[] deltas = event.findMarkerDeltas(null, true);

                if ((deltas != null) && (deltas.length > 0)) {
                    Set resources = new HashSet();

                    for (IMarkerDelta delta : deltas) {
                        resources.add(delta.getResource());
                    }

                    final Object[] resourcesToUpdate = resources.toArray();

                    display.asyncExec(new Runnable() {
                        /**
                         * {@inheritDoc}
                         * 
                         * @see java.lang.Runnable#run()
                         */
                        @Override
                        public void run() {
                            changeLabel(resourcesToUpdate);
                        }
                    });
                }
            }
        };

        ModelerCore.getWorkspace().addResourceChangeListener(this.resrcChgListener);
        this.diagramLabelProvider = new DiagramLabelProvider();
        this.extendedModelObjectLabelProvider = new ExtendedModelObjectLabelProvider();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.BaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    @Override
    public void addListener( ILabelProviderListener listener ) {
        super.addListener(listener);

        // Method declared on IBaseLabelProvider. Defect 23509 - we need to keep track of # of listeners in this class because super
        // does not have a getter
        myListeners.add(listener);
    }

    void changeLabel( Object[] resourcesToUpdate ) {
        fireLabelProviderChanged(new LabelProviderChangedEvent(getLabelProviderChangedEventSource(), resourcesToUpdate));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
     */
    @Override
    public void decorate( final Object element,
                          final IDecoration decoration ) {
        final Display display = Display.getDefault();

        if (display.isDisposed()) {
            return;
        }

        final IResource resrc = getResource(element);

        if ((resrc == null) || !resrc.exists() || ((resrc instanceof IProject) && !((IProject)resrc).isOpen())) {
            return;
        }

        IMarker[] markers = null;
        boolean errorOccurred = false;

        try {
            markers = resrc.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
        } catch (CoreException ex) {
            Util.log(ex);
            errorOccurred = true;
        }

        if (!errorOccurred) {
            ImageDescriptor decorationIcon = getDecorationIcon(markers);

            if (decorationIcon != null) {
                decoration.addOverlay(decorationIcon);
            }
        }

        try {
            // add suffix for special warnings:
            IMarker[] problems = resrc.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);

            for (IMarker marker : problems) {
                String value = marker.getAttribute(ModelerCore.MARKER_PROBLEM_DECORATOR_TEXT, null);

                if (value != null) {
                    decoration.addSuffix(problemMarkerBrackets);
                    break;
                }
            }
        } catch (CoreException ex) {
            Util.log(ex);
        }

        // Lastly, decorate with Extension if applicable
        if (element instanceof IFile) {
            File file = ((IFile)element).getLocation().toFile();
            ModelExtensionRegistry registry = ExtensionPlugin.getInstance().getRegistry();

            try {
                for (String namespacePrefix : registry.getAllNamespacePrefixes()) {
                    ModelExtensionAssistant assistant = registry.getModelExtensionAssistant(namespacePrefix);

                    if ((assistant instanceof ModelObjectExtensionAssistant)
                            && ((ModelObjectExtensionAssistant)assistant).hasExtensionProperties(file)) {
                        decoration.addOverlay(UiPlugin.getDefault().getExtensionDecoratorImage(), IDecoration.TOP_LEFT);
                        break;
                    }
                }
            } catch (Exception e) {
                Util.log(e);
            }
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
     */
    @Override
    public void dispose() {
        if (this.resrcChgListener != null) {
            ModelerCore.getWorkspace().removeResourceChangeListener(this.resrcChgListener);
        }
    }

    private ImageDescriptor getDecorationIcon( IMarker[] markers ) {
    	ImageDescriptor icon = null;
    	
        final boolean startedTxn = ModelerCore.startTxn(false, false, null, this);
        boolean succeeded = false;
        try {
        	for (int ndx = markers.length; --ndx >= 0;) {
        		final Object attr = MarkerUtilities.getMarkerAttribute(markers[ndx], IMarker.SEVERITY); // markers[ndx].getAttribute(IMarker.SEVERITY);

        		if (attr == null) {
        			continue;
        		}

        		// Asserting attr is an Integer...
        		final int severity = ((Integer)attr).intValue();

        		if (severity == IMarker.SEVERITY_ERROR) {
        			icon = UiPlugin.getDefault().getErrorDecoratorImage();
        			break;
        		}

        		if ((icon == null) && (severity == IMarker.SEVERITY_WARNING)) {
        			icon = UiPlugin.getDefault().getWarningDecoratorImage();
        		}
        	}
        	
        	succeeded = true;
        } finally {
        	if (startedTxn) {
        		if (succeeded)
        			ModelerCore.commitTxn();
        		else
        			ModelerCore.rollbackTxn();
        	}
        }

        return icon;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.navigator.IDescriptionProvider#getDescription(java.lang.Object)
     */
    @Override
    public String getDescription( Object element ) {
        if (element instanceof EObject) {
            String description;
            EObject eObj = (EObject)element;

            if (eObj instanceof ENamedElement) {
                description = eObj.eClass().getName() + ": " + ((ENamedElement)eObj).getName(); //$NON-NLS-1$
            } else if (eObj instanceof XSDParticle) {
                description = ((XSDParticle)eObj).getTerm().eClass().getName();
            } else if (eObj instanceof XSDAttributeUse) {
                description = ((XSDAttributeUse)eObj).getAttributeDeclaration().eClass().getName();
            } else {
                description = eObj.eClass().getName();
            }

            return description + ": " + ModelerCore.getModelEditor().getModelRelativePathIncludingModel(eObj); //$NON-NLS-1$
        }

        if (element instanceof ImportContainer) {
            return ((ImportContainer)element).toString();
        }

        if (element instanceof IExtendedModelObject) {
            return ((IExtendedModelObject)element).getStatusLabel();
        }

        return getText(element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
     */
    @Override
    public Image getImage( Object element ) {
    	Image result = null;
    	
        final boolean startedTxn = ModelerCore.startTxn(false, false, null, this);
        boolean succeeded = false;

        try {

            if (element instanceof Diagram) {
                result = this.diagramLabelProvider.getImage(element);

                if (result == null) {
                    result = UiPlugin.getDefault().getImage("icons/full/obj16/Diagram.gif"); //$NON-NLS-1$
                }
            }
            else if (element instanceof ImportContainer) {
                result = UiPlugin.getDefault().getImage(IMPORT_CONTAINER);
            }
            else if (element instanceof EObject) {
                if (element instanceof TransformationMappingRoot)
                    result = UiPlugin.getDefault().getImage("icons/full/obj16/Transform.gif"); //$NON-NLS-1$
                else
                	result = ModelUtilities.getEMFLabelProvider().getImage(element);
            } else if (element instanceof IFile && ModelUtilities.isModelFile((IFile)element) && ((IFile)element).exists()) {
                result = ModelIdentifier.getModelImage((IResource)element);
            } else {
            	result = this.extendedModelObjectLabelProvider.getImage(element);
            }
            
            if (result == null) {
                result = this.defaultProvider.getImage(element);
            }
            
            succeeded = true;
            
        } catch (final Exception err) {
            Util.log(err);
        } finally {
            if (startedTxn) {
            	if (succeeded)
            		ModelerCore.commitTxn();
            	else
            		ModelerCore.rollbackTxn();
            }
        }

        if (result != null)
        	return result;
        else
        	return super.getImage(element);
    }

    public IBaseLabelProvider getLabelProviderChangedEventSource() {
        return this.eventSource;
    }

    /**
     * Returns the resource for the specified element, or null if there is no resource associated with it.
     * 
     * @param element The element for which to find an associated resource
     * @return The resource for the specified element; may be null.
     */
    private IResource getResource( final Object element ) {
        if (element instanceof IResource) {
            return (IResource)element;
        }

        if (element instanceof IAdaptable) {
            return (IResource)((IAdaptable)element).getAdapter(IResource.class);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText( Object element ) {
    	String result = null;
        final boolean startedTxn = ModelerCore.startTxn(false, false, null, this);
        boolean succeeded = false;
        
        try {
            if (element instanceof Diagram) {
                result = this.diagramLabelProvider.getText(element);

                if (result == null) {
                    result = genericDiagramLabel;
                }
            }
            else if (element instanceof ImportContainer) {
                result = element.toString();
            }
            else if (element instanceof EObject) {
                
            	if (element instanceof TransformationMappingRoot) {
                    result = genericTransformationLabel;
                }
            	else {
            		ILabelProvider p = ModelUtilities.getEMFLabelProvider();
            		result = p.getText(element);
            	}
            } else {
            	result = this.extendedModelObjectLabelProvider.getText(element);
            }

            if (result == null)
            	result = this.defaultProvider.getText(element);
            
            succeeded = true;
        } finally {
            if (startedTxn) {
            	if (succeeded)
            		ModelerCore.commitTxn();
            	else
            		ModelerCore.rollbackTxn();
            }
        }
        
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.BaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    @Override
    public void removeListener( ILabelProviderListener listener ) {
        super.removeListener(listener);

        // Method declared on IBaseLabelProvider. Defect 23509 - we need to keep track of # of listeners in this class because super
        // does not have a getter
        this.myListeners.remove(listener);
    }

}
