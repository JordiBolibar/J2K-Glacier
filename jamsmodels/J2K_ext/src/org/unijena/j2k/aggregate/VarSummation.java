/*
 * VarSummation.java
 * Created on 22. Februar 2005, 15:01
 *
 * This file is part of JAMS
 * Copyright (C) 2005 S. Kralisch and P. Krause
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 *
 */

package org.unijena.j2k.aggregate;

import jams.model.*;
import jams.data.*;

/**
 *
 * @author S. Kralisch
 */
public class VarSummation extends JAMSComponent {
    
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "input values"
            )
            public Attribute.Double[] value;
    
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READWRITE,            
            description = "output sum"
            )
            public Attribute.Double sum;
    

    
    @Override
    public void init() {
        
    }

    @Override
    public void run() {
        //System.out.println("in:" + value[0].getValue() + " " + value[1].getValue());
        for (int i = 0; i < value.length; i++) {
            sum.setValue(sum.getValue()+ value[i].getValue());
        }
        //System.out.println("\tout:"+sum.getValue());
    }
    
    @Override
    public void cleanup(){
        for (int i = 0; i < value.length; i++) {
            sum.setValue(0);
        }
    }
    
}
