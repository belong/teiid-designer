/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.extension.ui.wizards;

import java.util.Set;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.teiid.designer.extension.ExtensionPlugin;
import org.teiid.designer.extension.definition.ModelExtensionDefinitionValidator;
import org.teiid.designer.extension.definition.ValidationStatus;
import org.teiid.designer.extension.registry.ModelExtensionRegistry;
import org.teiid.designer.extension.ui.Activator;
import org.teiid.designer.extension.ui.Messages;
import com.metamatrix.ui.internal.util.WidgetFactory;
import com.metamatrix.ui.internal.wizard.AbstractWizardPage;

/**
 * @since 5.5
 */
public class NewMedDetailsPage extends AbstractWizardPage {

    private static final int COLUMN_COUNT = 2;
    private static final String DEFAULT_METAMODEL_NAME = "Relational"; //$NON-NLS-1$
    private static final String DEFAULT_VERSION = "1"; //$NON-NLS-1$
    private static final String TOKEN_ERROR = "[Error] "; //$NON-NLS-1$
    private static final String TOKEN_WARNING = "[Warning] "; //$NON-NLS-1$
    private static final String TOKEN_INFO = "[Info] "; //$NON-NLS-1$
    private static final String CR = "\n"; //$NON-NLS-1$

    private Text namespacePrefixText, namespaceUriText, versionText, descriptionText, statusTextBox;
    private Combo cbxMetamodelUris;

    public NewMedDetailsPage() {
        super(NewMedDetailsPage.class.getSimpleName(), Messages.newMedDetailsPageTitle);
    }

    public void createControl( Composite theParent ) {

        Composite pnlMain = WidgetFactory.createPanel(theParent, SWT.NONE, GridData.FILL_BOTH);
        pnlMain.setLayout(new GridLayout(COLUMN_COUNT, false));
        setControl(pnlMain);
        createMainPanel(pnlMain);

        validatePage();
    }

    private Composite createMainPanel( Composite parent ) {
        // -----------------------------------------------------
        // Namespace Prefix
        // -----------------------------------------------------
        // NSPrefix Label
        WidgetFactory.createLabel(parent, Messages.namespacePrefixLabel);
        // NSPrefix Text widget
        this.namespacePrefixText = WidgetFactory.createTextField(parent, GridData.FILL_HORIZONTAL, 1);
        this.namespacePrefixText.setToolTipText(Messages.medNamespacePrefixTooltip);
        this.namespacePrefixText.addModifyListener(new ModifyListener() {
            public void modifyText( final ModifyEvent event ) {
                nsPrefixModified();
            }
        });

        // -----------------------------------------------------
        // Namespace Uri
        // -----------------------------------------------------
        // NSUri Label
        WidgetFactory.createLabel(parent, Messages.namespaceUriLabel);
        // NSUri text widget
        this.namespaceUriText = WidgetFactory.createTextField(parent, GridData.HORIZONTAL_ALIGN_FILL, COLUMN_COUNT - 1);
        this.namespaceUriText.setToolTipText(Messages.medNamespaceUriTooltip);
        this.namespaceUriText.addModifyListener(new ModifyListener() {
            public void modifyText( final ModifyEvent event ) {
                nsUriModified();
            }
        });

        // -----------------------------------------------------
        // Metamodel Uri
        // -----------------------------------------------------
        // MetamodelUri Label
        WidgetFactory.createLabel(parent, Messages.extendedMetamodelUriLabel);
        // MetamodelUri text widget
        this.cbxMetamodelUris = WidgetFactory.createCombo(parent, GridData.HORIZONTAL_ALIGN_FILL, COLUMN_COUNT - 1);
        this.cbxMetamodelUris.setToolTipText(Messages.medMetamodelClassTooltip);
        // populate metamodel names
        Set<String> metamodelNames = Activator.getDefault().getExtendableMetamodelNames();
        this.cbxMetamodelUris.setItems(metamodelNames.toArray(new String[metamodelNames.size()]));
        int defIndex = this.cbxMetamodelUris.indexOf(DEFAULT_METAMODEL_NAME);
        this.cbxMetamodelUris.select(defIndex);
        this.cbxMetamodelUris.addModifyListener(new ModifyListener() {
            public void modifyText( final ModifyEvent event ) {
                metamodelUriModified();
            }
        });

        // -----------------------------------------------------
        // Version
        // -----------------------------------------------------
        // Version Label
        WidgetFactory.createLabel(parent, Messages.versionLabel);
        // Version text widget
        this.versionText = WidgetFactory.createTextField(parent, GridData.HORIZONTAL_ALIGN_FILL, COLUMN_COUNT - 1);
        this.versionText.setToolTipText(Messages.medVersionTooltip);
        this.versionText.setText(DEFAULT_VERSION);
        this.versionText.addModifyListener(new ModifyListener() {
            public void modifyText( final ModifyEvent event ) {
                versionModified();
            }
        });

        // -----------------------------------------------------
        // Description
        // -----------------------------------------------------
        // Description Label
        WidgetFactory.createLabel(parent, Messages.descriptionLabel);
        // Description text widget
        this.descriptionText = WidgetFactory.createTextField(parent, GridData.HORIZONTAL_ALIGN_FILL, COLUMN_COUNT - 1);
        this.descriptionText.setToolTipText(Messages.medDescriptionToolTip);
        this.descriptionText.addModifyListener(new ModifyListener() {
            public void modifyText( final ModifyEvent event ) {
                descriptionModified();
            }
        });

        // -----------------------------------------------------
        // Status
        // -----------------------------------------------------
        // Status Label
        WidgetFactory.createLabel(parent, GridData.HORIZONTAL_ALIGN_FILL, COLUMN_COUNT, Messages.newMedDetailsPageStatusLabel);
        // Description text widget
        this.statusTextBox = WidgetFactory.createTextField(parent, GridData.FILL_BOTH,
                                                           COLUMN_COUNT,
                                                           null,
                                                           SWT.BORDER | SWT.MULTI | SWT.H_SCROLL);

        this.namespacePrefixText.setFocus();

        return parent;
    }

    private void nsPrefixModified() {
        validatePage();
    }

    private void nsUriModified() {
        validatePage();
    }

    private void metamodelUriModified() {
        validatePage();
    }

    private void versionModified() {
        validatePage();
    }

    private void descriptionModified() {
        validatePage();
    }

    public String getNamespacePrefix() {
        return this.namespacePrefixText.getText();
    }

    public String getNamespaceUri() {
        return this.namespaceUriText.getText();
    }

    public String getMetamodelUri() {
        String metamodelName = this.cbxMetamodelUris.getText();
        return Activator.getDefault().getMetamodelUri(metamodelName);
    }

    public String getVersion() {
        return this.versionText.getText();
    }

    public int getVersionInt() {
        int versionInt = 1;
        try {
            versionInt = Integer.parseInt(this.versionText.getText());
        } catch (Exception e) {
        }
        return versionInt;
    }

    public String getDescription() {
        return this.descriptionText.getText();
    }

    /**
     * Validation logic for the page. Wizard completion is allowed, even with errors - but the problems should be displayed.
     * 
     * @since 7.6
     */
    private void validatePage() {
        ValidationStatus nsPrefixStatus = ModelExtensionDefinitionValidator.validateNamespacePrefix(getNamespacePrefix(), getRegistry().getAllNamespacePrefixes());
        ValidationStatus nsUriStatus = ModelExtensionDefinitionValidator.validateNamespaceUri(getNamespaceUri(),
                                                                                              getRegistry().getAllNamespaceUris());
        ValidationStatus metaModelUriStatus = ModelExtensionDefinitionValidator.validateMetamodelUri(getMetamodelUri(),
                                                                                                     getRegistry().getExtendableMetamodelUris());
        ValidationStatus definitionStatus = ModelExtensionDefinitionValidator.validateDescription(getDescription());
        ValidationStatus versionStatus = ModelExtensionDefinitionValidator.validateVersion(getVersion());

        StringBuffer sb = new StringBuffer();
        addStatusMessage(sb, nsPrefixStatus);
        addStatusMessage(sb, nsUriStatus);
        addStatusMessage(sb, metaModelUriStatus);
        addStatusMessage(sb, versionStatus);
        addStatusMessage(sb, definitionStatus);

        this.statusTextBox.setText(sb.toString());

        setMessage(Messages.newMedDetailsPageMsg);
        setPageComplete(true);
    }

    private void addStatusMessage( StringBuffer sb,
                                   ValidationStatus status ) {
        if (status.isError()) {
            sb.append(TOKEN_ERROR + status.getMessage() + CR);
        } else if (status.isWarning()) {
            sb.append(TOKEN_WARNING + status.getMessage() + CR);
        } else if (status.isInfo()) {
            sb.append(TOKEN_INFO + status.getMessage() + CR);
        }
    }

    /**
     * @return the model extension registry (never <code>null</code>)
     */
    protected ModelExtensionRegistry getRegistry() {
        return ExtensionPlugin.getInstance().getRegistry();
    }

}