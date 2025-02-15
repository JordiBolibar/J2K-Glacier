/*
 * SumAggregator.java
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

@JAMSComponentDescription(
        title="Sum aggregator",
        author="Sven Kralisch",
        description="Takes a list of double values as input and adds each "
        + "element to a corresponding element of an equally sized list of "
        + "output values. Can be used to sum up any given number of double "
        + "values over a number of iteration steps.",
        version="1.0_0",
        date="2005-05-22"
        )
public class SumAggregator extends JAMSComponent {
    
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "list of input values"
            )
            public Attribute.Double[] value;
    
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READWRITE,
            description = "list of sum values"
            )
            public Attribute.Double[] sum;
    

    
    public void init() {
        for (int i = 0; i < value.length; i++) {
            sum[i].setValue(0);
        }
    }

    public void run() {
        for (int i = 0; i < value.length; i++) {
            sum[i].setValue(sum[i].getValue()+ (value[i].getValue()));
        }
    }
    
    public void cleanup(){
        for (int i = 0; i < value.length; i++) {
            //sum[i].setValue(0);
        }
    }
    
}
