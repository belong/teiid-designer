/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.metamodels.xml.aspects.validation;

import org.teiid.designer.core.metamodel.aspect.MetamodelEntity;
import org.teiid.designer.core.validation.ValidationRuleSet;



/** 
 * XmlDocumentNodeAspect
 * @since 8.0
 */
public class XmlDocumentNodeAspect extends AbstractXmlNodeAspect {

    /** 
     * @param entity
     * @since 4.2
     */
    public XmlDocumentNodeAspect(MetamodelEntity entity) {
        super(entity);
    }
    
    /**
     * Get all the validation rules for XmlDocumentNode.
     */
    @Override
    public ValidationRuleSet getValidationRules() {
        addRule(DOCUMENT_NODE_NAME_RULE);
        addRule(DOCUMENT_NODE_LENGTH_RULE);
        addRule(DOCUMENT_NODE_TYPE_RULE);
        return super.getValidationRules();
    }
}
