/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package com.metamatrix.modeler.dqp.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.FileEditorInput;
import org.teiid.designer.vdb.Vdb;
import com.metamatrix.modeler.internal.vdb.ui.editor.VdbEditor;
import com.metamatrix.modeler.ui.UiPlugin;
import com.metamatrix.modeler.vdb.ui.VdbUiConstants;

/**
 * @since 4.3
 */
public class DefaultVdbEditorUtil implements IVdbEditorUtil {

    private void activateEditor( final Vdb vdb,
                                 final String tabId ) {

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {

                final IPath contextVdbPath = vdb.getName();
                final IWorkbenchWindow window = UiPlugin.getDefault().getCurrentWorkbenchWindow();

                if (window != null) {
                    final IWorkbenchPage page = window.getActivePage();

                    if (page != null) {
                        VdbEditor editor = findEditorPart(page, contextVdbPath);
                        if (editor != null) {

                            page.activate(editor);
                            editor.setTab(tabId);

                        } else {

                            // at this point, there is no active editor for this context, so open it
                            final IFile vdbFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(contextVdbPath);
                            if (vdbFile != null) try {
                                editor = (VdbEditor)page.openEditor(new FileEditorInput(vdbFile),
                                                                    VdbUiConstants.Extensions.VDB_EDITOR_ID);
                                page.activate(editor);
                                editor.setTab(tabId);
                            } catch (final Exception e) {
                                DqpUiConstants.UTIL.log(e);
                            }

                        }
                    }
                }

            }
        });
    }

    /**
     * @see com.metamatrix.modeler.dqp.ui.IVdbEditorUtil#displayVdbProblems(com.metamatrix.vdb.edit.VdbEditingContext)
     * @since 4.3
     */
    public void displayVdbProblems( final Vdb vdb ) {

        activateEditor(vdb, VdbUiConstants.Extensions.PROBLEMS_TAB_ID);

    }

    VdbEditor findEditorPart( final IWorkbenchPage page,
                              final IPath contextVdbPath ) {
        // look through the open editors and see if there is one available for this model file.
        final IEditorReference[] editors = page.getEditorReferences();
        for (int i = 0; i < editors.length; ++i) {

            final IEditorPart editor = editors[i].getEditor(false);
            if (editor instanceof VdbEditor) {
                final VdbEditor vdbEditor = (VdbEditor)editor;
                final IPath editorVdbPath = vdbEditor.getVdb().getName();
                if (contextVdbPath.equals(editorVdbPath)) return vdbEditor;

            }
        }

        return null;
    }

    /**
     * @see com.metamatrix.modeler.dqp.ui.IVdbEditorUtil#openConnectorBindingsEditor(com.metamatrix.vdb.edit.VdbEditingContext)
     * @since 4.3
     */
    public void openConnectorBindingsEditor( final Vdb vdb ) {

        activateEditor(vdb, DqpUiConstants.VDB_EDITOR_CONNECTOR_BINDINGS_ID);

    }

    /**
     * @see com.metamatrix.modeler.dqp.ui.IVdbEditorUtil#openVdbEditor(com.metamatrix.vdb.edit.VdbEditingContext)
     * @since 4.3
     */
    public void openVdbEditor( final Vdb vdb,
                               final String tabId ) {

        activateEditor(vdb, tabId);

    }

}
