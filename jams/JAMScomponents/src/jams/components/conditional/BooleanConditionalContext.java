/*
 * BooleanConditionalContext.java
 * Created on 7. Januar 2008, 09:34
 *
 * This file is part of JAMS
 * Copyright (C) FSU Jena
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
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */
package jams.components.conditional;

import jams.data.*;
import jams.model.Component;
import jams.model.ComponentEnumerator;
import jams.model.JAMSComponent;
import jams.model.JAMSComponentDescription;
import jams.model.JAMSContext;
import jams.model.JAMSVarDescription;

/**
 *
 * @author S. Kralisch
 */
@JAMSComponentDescription(title = "BooleanConditionalContext", author = "Sven Kralisch", date = "7. January 2008", description = "This component represents a JAMS context which can be used to "
+ "conditionally execute components. This context must contain two components. If \"condition\" is true, the first one will be executed, otherwise the second one.")
public class BooleanConditionalContext extends JAMSContext {

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ, description = "Boolean attribute defining which component to execute", defaultValue = "1")
    public Attribute.Boolean condition;
    
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ, description = "Boolean attribute defining which component to execute", defaultValue = "1")
    public Attribute.Boolean initCondition;

    public BooleanConditionalContext() {
    }

    @Override
    public ComponentEnumerator getInitEnumerator() {
        if (initCondition == null){
            return super.getInitEnumerator();
        }
        return new InitEnumerator();
    }
    
    @Override
    public ComponentEnumerator getRunEnumerator() {
        return new RunEnumerator();
    }

    @Override
    public long getNumberOfIterations() {
        return -1;
    }

    @Override
    public long getRunCount() {
        return -1;
    }  

    class RunEnumerator implements ComponentEnumerator {

        final DummyComponent dummy = new DummyComponent();
        Component[] compArray = getCompArray();
        boolean next = true;

        public class DummyComponent extends JAMSComponent {

            public void run() {
                return;
            }
        }

        @Override
        public boolean hasNext() {
            if (next) {
                next = false;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean hasPrevious() {
            return !hasNext();
        }

        @Override
        public Component next() {
            // if condition is true return first component, else second component
            if (condition.getValue()) {
                return compArray[0];
            } else {
                if (compArray.length < 2 || compArray[1] == null) {
                    return dummy;
                }
                return compArray[1];
            }
        }

        @Override
        public Component previous() {
            return next();
        }

        @Override
        public void reset() {
            next = true;
        }
    }
    
    class InitEnumerator extends RunEnumerator {

        final DummyComponent dummy = new DummyComponent();
        Component[] compArray = getCompArray();
        boolean next = true;
        
        @Override
        public Component next() {
            // if condition is true return first component, else second component
            if (initCondition.getValue()) {
                return compArray[0];
            } else {
                if (compArray.length < 1 || compArray[1] == null) {
                    return dummy;
                }
                return compArray[1];
            }
        }
    }
}
