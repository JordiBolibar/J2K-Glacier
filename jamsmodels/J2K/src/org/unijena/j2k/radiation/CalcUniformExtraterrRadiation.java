/*
 * CalcAdditionalHRUAttribs.java
 * Created on 24. November 2005, 11:46
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



package org.unijena.j2k.radiation;

import jams.data.*;
import jams.model.*;

/**
 *
 * @author Peter Krause
 */
@JAMSComponentDescription(
        title="CalcAdditionalHRUAttribs",
        author="Peter Krause",
        description="Calculates additional attributes from existent ones"
        )
        public class CalcUniformExtraterrRadiation extends JAMSComponent {

    /*
     *  Component variables
     */
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "entity latidute [deg]"
            )
            public Attribute.Double latitude;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "entity longitude [deg]"
            )
            public Attribute.Double longitude;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "longitude of time zone [deg]"
            )
            public Attribute.Double longTZ;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "temporal resolution [d | h | m]"
            )
            public Attribute.String tempRes;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "location from Greenwich [w | e]"
            )
            public Attribute.String locGrw;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "extraterrestric radiation of each time step of the year [MJ/m² timeUnit]"
            )
            public Attribute.DoubleArray extRadArray;

    int[] monthMean = {15,45,74,105,135,166,196,227,258,288,319,349};

    /*
     *  Component run stages
     */
    double[] extRadiation = null;

    public void init() {
        if(tempRes.getValue().equals("d"))
            extRadiation = new double[366];
        else if(tempRes.getValue().equals("h"))
            extRadiation = new double[366*24];
        else if(tempRes.getValue().equals("m"))
            extRadiation = new double[12];

        double lati = this.latitude.getValue();
        double longi = this.longitude.getValue();
        double longiTZ = this.longTZ.getValue();

        if(this.locGrw.getValue().equals("e")){
            longi = 360 - longi;
            longiTZ = 360 - longiTZ;
        }

        double latRad = org.unijena.j2k.mathematicalCalculations.MathematicalCalculations.deg2rad(lati);
        Attribute.Calendar time = getModel().getRuntime().getDataFactory().createCalendar();
        time.set(2000, 0, 1, 0, 0, 0); //just a leap year, that's the reason for 2000
        if(tempRes.getValue().equals("m")){
            for(int i = 0; i < 12; i++){
                double declination = org.unijena.j2k.physicalCalculations.SolarRadiationCalculationMethods.calc_SunDeclination(monthMean[i]);
                double solarConstant = org.unijena.j2k.physicalCalculations.SolarRadiationCalculationMethods.calc_SolarConstant(monthMean[i]);
                double invRelDistEarthSun = org.unijena.j2k.physicalCalculations.SolarRadiationCalculationMethods.calc_InverseRelativeDistanceEarthSun(monthMean[i]);
                double sunsetHourAngle = org.unijena.j2k.physicalCalculations.DailySolarRadiationCalculationMethods.calc_SunsetHourAngle(latRad, declination);
                extRadiation[i] = org.unijena.j2k.physicalCalculations.DailySolarRadiationCalculationMethods.calc_DailyExtraterrestrialRadiation(solarConstant,
                        invRelDistEarthSun, sunsetHourAngle, latRad, declination);
            }
        } else{
            for(int i = 0; i < 366; i++){
                int hour = 0;
                int julDay = i+1;
                double declination = org.unijena.j2k.physicalCalculations.SolarRadiationCalculationMethods.calc_SunDeclination(julDay);
                double solarConstant = org.unijena.j2k.physicalCalculations.SolarRadiationCalculationMethods.calc_SolarConstant(julDay);
                double invRelDistEarthSun = org.unijena.j2k.physicalCalculations.SolarRadiationCalculationMethods.calc_InverseRelativeDistanceEarthSun(julDay);
                if(tempRes.getValue().equals("d")){
                    double sunsetHourAngle = org.unijena.j2k.physicalCalculations.DailySolarRadiationCalculationMethods.calc_SunsetHourAngle(latRad, declination);
                    extRadiation[i] = org.unijena.j2k.physicalCalculations.DailySolarRadiationCalculationMethods.calc_DailyExtraterrestrialRadiation(solarConstant,
                            invRelDistEarthSun, sunsetHourAngle, latRad, declination);
                    //time.add(time.DATE, 1);
                }else if(tempRes.getValue().equals("h")){
                    while(hour < 24){
                        double midTimeHourAngle = org.unijena.j2k.physicalCalculations.HourlySolarRadiationCalculationMethods.calc_midTimeHourAngle(hour, julDay, longi, longiTZ, false);
                        double startTimeHourAngle = org.unijena.j2k.physicalCalculations.HourlySolarRadiationCalculationMethods.calc_startTimeHourAngle(midTimeHourAngle);
                        double endTimeHourAngle = org.unijena.j2k.physicalCalculations.HourlySolarRadiationCalculationMethods.calc_endTimeHourAngle(midTimeHourAngle);
                        int idx = i * 24 + hour;
                        extRadiation[idx] = org.unijena.j2k.physicalCalculations.HourlySolarRadiationCalculationMethods.calc_HourlyExtraterrestrialRadiation(solarConstant, invRelDistEarthSun, startTimeHourAngle, endTimeHourAngle, latRad, declination);
                        hour++;
                        //time.add(time.HOUR_OF_DAY, 1);
                    }
                }
            }
        }
    }

    public void run() throws Attribute.Entity.NoSuchAttributeException{
        this.extRadArray.setValue(extRadiation);
    }

    public void cleanup() {

    }
}
