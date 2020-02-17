/*
 * ArrayGrabber.java
 * Created on 24. November 2005, 10:52
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

 /*
 <component class="org.unijena.j2k.ArrayGrabber" name="j2kArrayGrabber">
    <jamsvar name="time" provider="TemporalContext" providervar="current"/>
    <jamsvar name="LAIArray" provider="HRUContext" providervar="currentEntity.LAIArray"/>
    <jamsvar name="effHArray" provider="HRUContext" providervar="currentEntity.effHArray"/>
    <jamsvar name="slAsCfArray" provider="HRUContext" providervar="currentEntity.slAsCfArray"/>
    <jamsvar name="rsc0Array" provider="HRUContext" providervar="currentEntity.rsc0Array"/>
    <jamsvar name="extRadArray" provider="HRUContext" providervar="currentEntity.extRadArray"/>
    <jamsvar name="actLAI" provider="HRUContext" providervar="currentEntity.actLAI"/>
    <jamsvar name="actEffH" provider="HRUContext" providervar="currentEntity.actEffH"/>
    <jamsvar name="actSlAsCf" provider="HRUContext" providervar="currentEntity.actSlAsCf"/>
    <jamsvar name="actRsc0" provider="HRUContext" providervar="currentEntity.actRsc0"/>
    <jamsvar name="actExtRad" provider="HRUContext" providervar="currentEntity.actExtRad"/>
    <jamsvar name="tempRes" value="d"/>
</component>
 */
package j2k.highres.io;

import jams.JAMS;
import jams.data.*;
import jams.model.*;

/**
 *
 * @author Peter Krause
 */
@JAMSComponentDescription(
        title = "J2KArrayGrabber",
        author = "Peter Krause",
        description = "This component selects data values from arrays representing"
        + "a standard year.",
        version = "1.0_0",
        date = "2011-05-30"
)
public class ArrayGrabber extends JAMSComponent {

    /*
     *  Component variables
     */
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "time"
    )
    public Attribute.Calendar time;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "extraTerrRadiationArray"
    )
    public Attribute.DoubleArray extRadArray;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "LeafAreaIndexArray"
    )
    public Attribute.DoubleArray LAIArray;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "EffectiveHeightArray"
    )
    public Attribute.DoubleArray effHArray;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "rsc0 Array"
    )
    public Attribute.DoubleArray rsc0Array;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "slopeAscpectCorrectionFactorArray"
    )
    public Attribute.DoubleArray slAsCfArray;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "actExtraTerrRadiation"
    )
    public Attribute.Double actExtRad;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "actLAI"
    )
    public Attribute.Double actLAI;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "actEffH"
    )
    public Attribute.Double actEffH;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "actRsc0"
    )
    public Attribute.Double actRsc0;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "Haude factor array"
    )
    public Attribute.DoubleArray haudeFactorArray;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "actHaudeFactor",
            defaultValue = "0"
    )
    public Attribute.Double actHaudeFactor;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "actSlopeAscpectCorrectionFactor"
    )
    public Attribute.Double actSlAsCf;

    /*
     *  Component run stages
     */
    public void init() throws Attribute.Entity.NoSuchAttributeException {

    }

    public void run() throws Attribute.Entity.NoSuchAttributeException {

        int monthCount = time.get(Attribute.Calendar.MONTH);
        int dayCount = time.get(Attribute.Calendar.DAY_OF_YEAR) - 1;
        int hourCount = time.get(Attribute.Calendar.HOUR_OF_DAY) + (24 * dayCount);

        double in_LAI = JAMS.getMissingDataValue();
        double in_effH = JAMS.getMissingDataValue();
        double in_extRad = JAMS.getMissingDataValue();
        double in_scf = JAMS.getMissingDataValue();
        double in_rsc0 = JAMS.getMissingDataValue();
        double in_haudeF = JAMS.getMissingDataValue();

        if (this.rsc0Array != null) {
            in_rsc0 = this.rsc0Array.getValue()[monthCount];
        }

        if (this.haudeFactorArray != null) {
            in_haudeF = this.haudeFactorArray.getValue()[monthCount];
        }

        if (this.LAIArray != null) {
            in_LAI = this.LAIArray.getValue()[dayCount];
        }

        if (this.effHArray != null) {
            in_effH = this.effHArray.getValue()[dayCount];
        }

        if (this.extRadArray != null) {
            in_extRad = this.extRadArray.getValue()[hourCount];
        } 

        if (this.slAsCfArray != null) {
            in_scf = this.slAsCfArray.getValue()[dayCount];
        }

        this.actLAI.setValue(in_LAI);
        this.actEffH.setValue(in_effH);
        this.actRsc0.setValue(in_rsc0);
        this.actSlAsCf.setValue(in_scf); 
        this.actExtRad.setValue(in_extRad);
        this.actHaudeFactor.setValue(in_haudeF);

    }

    public void cleanup() {

    }

}