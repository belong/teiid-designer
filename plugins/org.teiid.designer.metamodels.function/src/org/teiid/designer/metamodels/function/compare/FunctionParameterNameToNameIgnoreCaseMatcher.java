/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.metamodels.function.compare;

import org.eclipse.emf.ecore.EObject;
import org.teiid.designer.core.compare.AbstractEObjectNameMatcher;
import org.teiid.designer.metamodels.function.FunctionParameter;



/** 
 * @since 8.0
 */
public class FunctionParameterNameToNameIgnoreCaseMatcher extends AbstractEObjectNameMatcher {

    /**
     * Construct an instance of FunctionParameterNameToNameIgnoreCaseMatcher.
     * 
     */
    public FunctionParameterNameToNameIgnoreCaseMatcher() {
        super();
    }

    @Override
    protected String getInputKey( final EObject entity ) {
        if(entity instanceof FunctionParameter) {
            final String name = ((FunctionParameter)entity).getName();
            if(name != null) {
                return name.toUpperCase();
            }
        }
        return null;
    }

    @Override
    protected String getOutputKey( final EObject entity ) {
        if(entity instanceof FunctionParameter) {
            final String name = ((FunctionParameter)entity).getName();
            if(name != null) {
                return name.toUpperCase();
            }
        }
        return null;
    }

}
