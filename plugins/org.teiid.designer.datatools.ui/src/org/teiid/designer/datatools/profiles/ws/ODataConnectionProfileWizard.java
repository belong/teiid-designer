/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.designer.datatools.profiles.ws;

import java.util.Properties;

import org.eclipse.datatools.connectivity.ui.wizards.NewConnectionProfileWizard;
import org.teiid.designer.datatools.ui.DatatoolsUiConstants;

public class ODataConnectionProfileWizard extends NewConnectionProfileWizard
		implements DatatoolsUiConstants {
	
	Properties profileProperties = new Properties();

	@Override
	public void addCustomPages() {
		addPage(new ODataProfileDetailsWizardPage(UTIL.getString("WSProfileDetailsWizardPage.Name"))); //$NON-NLS-1$
	}

	@Override
	public Properties getProfileProperties() {
		return profileProperties;
	}

}
