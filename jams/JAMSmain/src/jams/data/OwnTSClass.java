/*
 * OwnTSClass.java
 *
 * Created on 14. März 2007, 16:44
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package jams.data;

import java.io.*;

/**
 *
 * @author ncb
 */
public class OwnTSClass implements Serializable {

    /** Creates a new instance of OwnTSClass */
    /*
     **  Licensed Materials - Property of IBM
     **
     **  (C) COPYRIGHT IBM Corp. 1996, 1997  All rights reserved
     **
     **  US Government Users Restricted Rights - Use, duplication or
     **  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
     **
     **/
    /**
     ** This class is just a dummy Userdefined Class 
     **  
     ** It must implenemt Serializable or Externalizable 
     ** 
     ** 
     ** @see Serializable
     ** @see Externalizable
     ** @version $Revision: 2.1 $ $Date: 1999/11/05 22:22:39 $
     ** @author John Thomas
     */
    /**
     ** serialVersionUID is generated by the Java 
     **  serialver program.  It should be regenerated when 
     **  an incompatible change is made to the class
     **
     **  run:
     **    set CLASSPATH=classpath	
     **    \java\jdk1.1.4\bin\serialver -show 
     **  enter:
     **     com.ibm.tspaces.examples.simple.ExamplerObj
     **  Then cut/paste the result into the line below   
     */
    static final long serialVersionUID = -2475757193974756987L;
    /**
     ** data to be serialized with the object 
     */
    private String userdata = null;
    /** 
     ** Data that it does not make sense to save should be marked 
     ** as "transient"
     ** GUI interface fields are often candidates for this.
     **
     */
    private transient long garbage;

    /** 
     ** Default public constructor is required if you implement Externalizable   
     **  
     */
    public OwnTSClass() {
        this("Dummy");
    }

    /** 
     ** Constructor with string specified 
     */
    public OwnTSClass(String data) {
        userdata = data;		// save the userdata.

    }

    /** 
     ** This method will return the data object.
     */
    public String getData() {

        return userdata;
    }

    /**
     ** An equals method is needed if you are going to do any searching
     ** of TupleSpace for Tuples that have a match on the user object.
     ** The default Object.equals(Object 0) returns true only on for
     **  byte by byte equality which may not make sense for objects 
     **  that consist of multiple data fields
     **  
     **  The following equals method determines equality by comparing 
     **  the "userdata" field.  
     **  The point is that for User objects, the equality is determined
     **  by the purpose and use of the object not by any predetermined 
     **  Java specification.
     ** 
     **  Note that it is important to protect against  
     **  NullPointerExceptions
     **
     ** @param other  The value from the corresponding Field in the 
     **         Tuple that we are comparing against.
     ** @return boolean true if the values are equal.
     */
    public boolean equals(Object other) {
        // no match if null target or target not the right type
        if ((other == null) || !(other instanceof OwnTSClass)) {
            return false;
        // Match if both values are null.
        }
        if (userdata == null) {
            return (((OwnTSClass) other).getData() == null);        // Match if String userdata equals String returned from getData
        }
        return userdata.equals(((OwnTSClass) other).getData());

    }  // end equals()
} // end class Example4Obj

