/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.designer.ui.viewsupport;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.teiid.designer.core.workspace.ModelUtil;
import org.teiid.designer.metamodels.core.ModelType;
import org.teiid.designer.ui.UiConstants;
import org.teiid.designer.ui.common.util.WidgetUtil;
import org.teiid.designer.ui.explorer.ModelExplorerContentProvider;
import org.teiid.designer.ui.explorer.ModelExplorerLabelProvider;


/**
 * @since 8.0
 */
public class MetamodelSelectionUtilities {

	/**
	 * Launches a workspace selection dialog targeted to show and allow selection of one view relational model
	 * @param type
	 * @param metamodelURI
	 * @return
	 */
	public static IFile selectViewModelInWorkspace() {
		final Object[] selections = WidgetUtil.showWorkspaceObjectSelectionDialog(
			UiConstants.Util.getString("MetamodelSelectionUtilities.selectViewModel_title"), //$NON-NLS-1$
			UiConstants.Util.getString("MetamodelSelectionUtilities.selectViewModel_msg"), false, null,  //$NON-NLS-1$
			getModelViewerFilter(ModelIdentifier.RELATIONAL_VIEW_MODEL_TYPE, ModelIdentifier.RELATIONAL_VIEW_MODEL_URI), 
			new ModelResourceSelectionValidator(false), 
			new ModelExplorerLabelProvider(), 
			new ModelExplorerContentProvider());

		if (selections != null && selections.length == 1) {
			if (selections[0] instanceof IFile) {
				return (IFile) selections[0];
			}
		}

		if (selections != null && selections.length == 1) {
			if (selections[0] instanceof IFile) {
				return (IFile) selections[0];
			}
		}
		
		return null;
	}
	
	/**
	 * Launches a workspace selection dialog targeted to show and allow selection of one view relational model
	 * @param type
	 * @param metamodelURI
	 * @return
	 */
	public static IFile selectSourceModelInWorkspace() {
		final Object[] selections = WidgetUtil.showWorkspaceObjectSelectionDialog(
			UiConstants.Util.getString("MetamodelSelectionUtilities.selectSourceModel_title"), //$NON-NLS-1$
			UiConstants.Util.getString("MetamodelSelectionUtilities.selectSourceModel_msg"), false, null,  //$NON-NLS-1$
			getModelViewerFilter(ModelIdentifier.RELATIONAL_SOURCE_MODEL_TYPE, ModelIdentifier.RELATIONAL_SOURCE_MODEL_URI), 
			new ModelResourceSelectionValidator(false), 
			new ModelExplorerLabelProvider(), 
			new ModelExplorerContentProvider());

		if (selections != null && selections.length == 1) {
			if (selections[0] instanceof IFile) {
				return (IFile) selections[0];
			}
		}
		
		return null;
	}
	
	public static ViewerFilter getModelViewerFilter(final ModelType type, final String metamodelURI) {
		return new ModelWorkspaceViewerFilter(true) {

			@Override
			public boolean select(final Viewer viewer, final Object parent, final Object element) {
				boolean doSelect = false;
				if (element instanceof IResource) {
					// If the project is closed, dont show
					boolean projectOpen = ((IResource) element).getProject().isOpen();
					if (projectOpen) {
						// Show open projects
						if (element instanceof IProject) {
							doSelect = true;
						} else if (element instanceof IContainer) {
							doSelect = true;
						} else if (element instanceof IFile && ModelUtil.isModelFile((IFile) element)) {
							doSelect = ModelIdentifier.isModel((IFile)element, type, metamodelURI);
						}
					}
				} else if (element instanceof IContainer) {
					doSelect = true;
				}

				return doSelect;
			}
		};
	}
	
}
