/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.core.validation;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.teiid.designer.core.ModelerCore;


/**
 * ValidationProblemImpl
 *
 * @since 8.0
 */
public class ValidationProblemImpl implements ValidationProblem {
    
    private final int code;
    private final int severity;
    private final String message;
    private boolean hasPreference;
    private final String uri;
    private final String location;
    
    /**
     * Construct an instance of ValidationProblemImpl.
     * @param status - The IStatus of the problem 
     */
    public ValidationProblemImpl(final IStatus status) {
        this(status.getCode(), status.getSeverity(), status.getMessage());
    }

    /**
     * Construct an instance of ValidationProblemImpl.
     * @param code - code for this exception 
     * @param severity - Severity using IStatus constants
     * @param msg - Message to report to the user
     */
    public ValidationProblemImpl(final int code, final int severity, final String message) {
        this(code, severity, message, null, null);
    }

    /**
     * 
     * @param code - code for this exception 
     * @param severity - Severity using IStatus constants
     * @param message - Message to report to the user
     * @param location - the location (generally, the parent) of the issue.  May be null.
     * @param uri - the URI of the EObject causing the issue.  May be null.
     */
    public ValidationProblemImpl(final int code, final int severity, final String message, String location, String uri) {
        validateSeverity(severity);

        this.code = code;
        this.severity = severity;
        this.message = message;
        this.uri = uri;
        this.location = location;
    }

    /**
     * @param severity
     */
    private void validateSeverity(final int severity) {
        if(severity == IStatus.ERROR || severity == IStatus.INFO || severity == IStatus.OK || severity == IStatus.WARNING){
            return;
        }
        throw new IllegalArgumentException(ModelerCore.Util.getString("ValidationProblemImpl.Invalid_severity.__Value_must_be_one_of_valid_status_constants_from_IStatus_class_1")); //$NON-NLS-1$
    }

    /**
     * @return code
     */
    @Override
	public int getCode() {
        return code;
    }

    /**
     * @return msg
     */
    @Override
	public String getMessage() {
        if(!hasPreference) {
            return message;
        }
        return message+ModelerCore.Util.getString("ValidationProblemImpl.option_preference"); //$NON-NLS-1$
    }

    /**
     * @return severity
     */
    @Override
	public int getSeverity() {
        return severity;
    }
    
    /** 
     * @param hasPreference The hasPreference to set.
     * @since 4.2
     */
    @Override
	public void setHasPreference(boolean hasPreference) {
        this.hasPreference = hasPreference;
    }

    @Override
    public String toString(){
        final StringBuffer buffer = new StringBuffer();
        
        buffer.append(getSeverityString() );
        buffer.append(" - "); //$NON-NLS-1$
        buffer.append(getMessage() );

        return buffer.toString();
    }
    
    private String getSeverityString(){
        switch (this.severity) {
            case IStatus.ERROR : return ModelerCore.Util.getString("ValidationProblemImpl.Error_1"); //$NON-NLS-1$
            case IStatus.INFO : return ModelerCore.Util.getString("ValidationProblemImpl.Info_2"); //$NON-NLS-1$
            case IStatus.OK : return ModelerCore.Util.getString("ValidationProblemImpl.OK_3"); //$NON-NLS-1$
            case IStatus.WARNING : return ModelerCore.Util.getString("ValidationProblemImpl.Warning_4"); //$NON-NLS-1$

            default :
                return(ModelerCore.Util.getString("ValidationProblemImpl.Unknown_Severity_5")); //$NON-NLS-1$
        }
    }

    /** 
     * @see org.teiid.designer.core.validation.ValidationProblem#getStatus()
     * @since 4.2
     */
    @Override
	public IStatus getStatus() {
        return new Status(severity, ModelerCore.PLUGIN_ID, code, message, null);
    }

    @Override
	public String getURI() {
        return uri;
    }

    @Override
	public String getLocation() {
        return location;
    }
}
