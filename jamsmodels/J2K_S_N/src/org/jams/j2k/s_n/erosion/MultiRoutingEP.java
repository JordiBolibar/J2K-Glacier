/*
 * J2KProcessMultiRouting.java
 *
 * Created on 5. Juni 2008, 14:27
 *
 * This file is part of JAMS
 * Copyright (C) 2005 FSU Jena, c0krpe
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
package org.jams.j2k.s_n.erosion;

import jams.data.*;
import jams.model.*;

/**
 *
 * @author Peter Krause
 */
@JAMSComponentDescription(title = "J2KProcessMultiRouting_h",
author = "Peter Krause, Holm Kipka, Bjoern Pfennig",
description = "Passes the output of the entities as input to the respective reach or unit")
public class MultiRoutingEP extends JAMSComponent {

    /*
     *  Component variables
     */
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
    update = JAMSVarDescription.UpdateType.RUN,
    description = "The current hru entity")
    public JAMSEntityCollection entities;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
    update = JAMSVarDescription.UpdateType.RUN,
    description = "Collection of reach objects")
    public JAMSEntityCollection reaches;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READWRITE,
    update = JAMSVarDescription.UpdateType.RUN,
    description = "HRU statevar sediment inflow")
    public JAMSDouble insed;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READWRITE,
    update = JAMSVarDescription.UpdateType.RUN,
    description = "HRU statevar sediment outflow")
    public JAMSDouble outsed;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READWRITE,
    update = JAMSVarDescription.UpdateType.RUN,
    description = "HRU statevar total phosphorus")
    public JAMSDouble totPin;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READWRITE,
    update = JAMSVarDescription.UpdateType.RUN,
    description = "HRU statevar total phosphorus")
    public JAMSDouble totPout;

    /*
     *  Component run stages
     */
    public void run() throws Attribute.Entity.NoSuchAttributeException {

        Attribute.Entity entity = entities.getCurrent();

        //receiving polygon
        Attribute.Entity[] toPolyArray = (Attribute.Entity[]) entity.getObject("to_poly");
        //receiving reach
        Attribute.Entity[] toReachArray = (Attribute.Entity[]) entity.getObject("to_reach");


        Double[] polyWeightsArray = (Double[]) entity.getObject("to_poly_weights");
        Double[] reachWeightsArray = (Double[]) entity.getObject("to_reach_weights");

        Attribute.Entity toPoly, toReach;
        double polyWeight, reachWeight;

        double sedout = outsed.getValue();

        boolean keinziel = false;


        if (toPolyArray.length > 0) {

            for (int a = 0; a < toPolyArray.length; a++) {

                if (toPolyArray[a] != null) {
                    toPoly = toPolyArray[a];
                    polyWeight = polyWeightsArray[a];
                    //double sedin = toPoly.getDouble("insed");
                    double sedin = (sedout * polyWeight) + toPoly.getDouble("insed");
                    //System.out.println(entity.getObject("ID")+" ~ "+a+" - to HRU - "+toPoly.getObject("ID")+ sedin);
                    //sedin = sedout * polyWeight;
                    //System.out.println(entity.getObject("ID")+" ~ "+a+" - in - "+ sedin);
                    toPoly.setDouble("insed", sedin);
                    double wegsed = sedout - (sedout * polyWeight);
                    toPoly.setDouble("outsed", wegsed);
                    if ((sedout == 0) && (sedin == 0)) {
                        outsed.setValue(0);
                    }
                    keinziel = true;
                }
            }
        }


        double totPout_ = totPout.getValue();
        if (toReachArray.length > 0) {
            for (int a = 0; a < toReachArray.length; a++) {
                toReach = toReachArray[a];
                reachWeight = reachWeightsArray[a];
                // nicht null!!!!
                if (toReachArray[a] != null) {
                    //double sedin = toReach.getDouble("insed");
                    double sedin = (sedout * reachWeight) + toReach.getDouble("insed");
                    //System.out.println(toReach.getObject("ID")+" Reach0 ~ "+a+" - to - "+ sedin);
                    //sedin = sedin + sedout * reachWeight;
                    //System.out.println(toReach.getObject("ID")+" Reach1 ~ "+a+" - to - "+ sedin);
                    toReach.setDouble("insed", sedin);
                    double wegsedreach = sedout - (sedout * reachWeight);
                    toReach.setDouble("outsed", wegsedreach);
                    ///---> phosphorus
                    double testtotPin = (totPout_ * reachWeight) + toReach.getDouble("totPin");
                    //if (testtotPin > 0.0) {
                    //    System.out.println("currentHRU_ID: " + entity.getDouble("ID") + " zuReachID: " + toReach.getObject("ID") + " :dry totP: " + testtotPin);
                    //}
                    toReach.setDouble("totPin", testtotPin);
                    keinziel = true;
                }
            }
        }
        //sedout = 0;
        //sedout = sedout * 0.05;
        //System.out.println(sedout);
        if (keinziel == false) {
            getModel().getRuntime().println("Current entity ID: " + (int) entity.getDouble("ID") + " has no receiver.");
        }
    }

    public void cleanup() {
    }
}