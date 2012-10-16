/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.core.designer;

/**
 * @since 8.0
 */
public class TeiidDesignerProcessingException extends TeiidDesignerException {

    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public TeiidDesignerProcessingException() {
        super();
    }

    /**
     * @param message
     */
    public TeiidDesignerProcessingException(String message) {
        super(message);
    }

    /**
     * @param childException
     */
    public TeiidDesignerProcessingException(Throwable childException) {
        super(childException);
    }

    /**
     * @param childException
     * @param message
     */
    public TeiidDesignerProcessingException(Throwable childException, String message) {
        super(childException, message);
    }

}
