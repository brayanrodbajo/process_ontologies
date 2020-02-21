/*
 * This file is part of the OWL API.
 *
 * The contents of this file are subject to the LGPL License, Version 3.0.
 *
 * Copyright (C) 2011, The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0
 * in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 *
 * Copyright 2011, The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;

import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;


/** Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 11-Jan-2007 */

public class Example {
	
    public static String getLabel(OWLClass cls, OWLOntology o, OWLDataFactory df) {
    	
    	for(OWLAnnotation a : (EntitySearcher.getAnnotations(cls, o, df.getRDFSLabel()))) {
    	    OWLAnnotationValue val = a.getValue();
    	    if(val instanceof OWLLiteral) {
    	        return ((OWLLiteral) val).getLiteral(); 
    	    }
    	}
    	return null;
    }
    
    public static boolean hasPL(OWLClass cls, OWLOntology ont) {
    	for(OWLAnnotation a : (EntitySearcher.getAnnotations(cls, ont))) {
    	    OWLAnnotationProperty prop = a.getProperty();
	        return (prop.toString().contains("preferredLabel"));
    	}
    	return false;
    }
    
    public static boolean isParent(OWLClass cls, OWLReasoner reasoner) {
    	NodeSet<OWLClass> children =  reasoner.getSubClasses(cls, true);
    	for (Node<OWLClass> node : children.getNodes()) {
    		for (OWLClass clazz : node.getEntities()) {
    			if (clazz.isOWLNothing()) {
    				return false;
    			}
    			return true;
    		}
    	}
    	return false;
    }
    
    public static OWLOntology removeClass(OWLOntology ont, OWLOntologyManager manager, OWLClass cls) {        
    	Set<OWLOntology> onts = new HashSet<>(Arrays.asList(ont));
    	OWLEntityRemover remover = new OWLEntityRemover(onts);
    	cls.accept(remover);
    	manager.applyChanges(remover.getChanges());

        return ont;
    }
    
    public static OWLOntology removeDup(OWLOntologyManager manager, OWLDataFactory df, OWLOntology ont, OWLReasoner reasoner, Set<OWLClass> classes) throws OWLOntologyStorageException {
    	List<String> labels = classes.stream()
    									.map(clazz->getLabel(clazz, ont, df))
    									.collect(Collectors.toList()); 
		Set<String> set1 = new HashSet<>();
		OWLOntology ontM = ont;
		int index = 0;
		OWLClass[] classesArray = new OWLClass[classes.size()];
		classes.toArray(classesArray);
		for (String a : labels) {
			if (!set1.add(a)) {
				OWLClass cls = classesArray[index];
				if (!isParent(cls, reasoner)) {
					ontM = removeClass(ontM, manager, cls);
					System.out.println(cls);
				}
			}
			index+=1;
		}
		return ontM;

		
    }

    	

    /** The examples here show how to load ontologies
     * 
     * @throws OWLOntologyCreationException 
     * @throws OWLOntologyStorageException */
    public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException {
    	
    	Example objExample = new Example();
    	
        // Get hold of an ontology manager
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File file = new File("/media/brayan/Acer/Users/Brayan/Desktop/alex/Ontologies/msp.owl");
        // Now load the local copy
        OWLOntology localPizza = manager.loadOntologyFromOntologyDocument(file);
        System.out.println("Loaded ontology: " + localPizza);
        // We can always obtain the location where an ontology was loaded from
        IRI documentIRI = manager.getOntologyDocumentIRI(localPizza);
        OWLDataFactory df = OWLManager.getOWLDataFactory();
        StructuralReasonerFactory rf = new StructuralReasonerFactory();
        OWLReasoner reasoner = rf.createReasoner(localPizza);

        System.out.println("    from: " + documentIRI);
        
        Set<OWLClass> classes = localPizza.getClassesInSignature();
        
        OWLOntology ontM = removeDup(manager, df, localPizza, reasoner, classes);

        classes = ontM.getClassesInSignature();
        
        System.out.println(classes.size());
        
        
        for (OWLClass cls : classes) {
        	if (!hasPL(cls, ontM)) {
        		String name = getLabel(cls, ontM, df);
        		
        		OWLLiteral lbl = df.getOWLLiteral(name, OWL2Datatype.RDFS_LITERAL);
        		OWLAnnotation label =
        		  df.getOWLAnnotation( 
        		    df.getOWLAnnotationProperty(IRI.create("http://ns.ontoforce.com/2013/disqover#preferredLabel")), lbl);
        		OWLAxiom axiom = df.getOWLAnnotationAssertionAxiom(cls.getIRI(), label);
        		List<OWLOntologyChange> changes = new 
        				ArrayList<OWLOntologyChange>();
        		changes.add(new AddAxiom(ontM, axiom));
        		manager.applyChanges(changes);

        	}
        }
        
        

		File outFile = new File("msp_m.owl");

		OWLDocumentFormat format = manager.getOntologyFormat(ontM);
		OWLXMLOntologyFormat owlxmlFormat = new OWLXMLOntologyFormat();
		if (format.isPrefixOWLOntologyFormat()) { 
		  owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat()); 
		}
		manager.saveOntology(ontM, owlxmlFormat, IRI.create(outFile.toURI()));
        
        
        // Remove the ontology again so we can reload it later
        manager.removeOntology(localPizza);
        
    }
}