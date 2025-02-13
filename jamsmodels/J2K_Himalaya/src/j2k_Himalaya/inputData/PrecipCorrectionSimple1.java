/*
 * RainCorrectionRichter.java
 * Created on 24. November 2005, 09:48
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
package j2k_Himalaya.inputData;

import jams.data.*;
import jams.model.*;
import java.util.Arrays;

/**
 *
 * @author Peter Krause
 */
@JAMSComponentDescription(title = "PrecipCorrection",
        author = "Peter Krause",
        description = "A simple method to increase or decrease precipitation by certain % factor + now precipitation can be corrected by months"
        + "such as increase in monsoon season only")
public class PrecipCorrectionSimple1 extends JAMSComponent {

    /*
     *  Component variables
     */
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "the precip values")
    public Attribute.DoubleArray inputValues;
    
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "Start of the month for correctionMonth, 1+startmont (july=6)",
            defaultValue = "1.0")
    public Attribute.Double startMonth;

    
        @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "End of the month for correctionMonth, 1+startmont (Oct=9)",
            defaultValue = "1.0")
    public Attribute.Double endMonth;
    
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.WRITE,
            description = "the precip values")
    public Attribute.DoubleArray outputValues;
    
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "Correction Factor for specific months",
            defaultValue = "1.0")
    public Attribute.Double correctionMonth;
    
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "Correction Factor for the whole year",
            defaultValue = "1.0")
    public Attribute.Double correctionYear;
    
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "time")
    public Attribute.Calendar time;
    /*
     *  Component run stages
     */
        
    @Override
    public void run() {
        int nowmonth = time.get(Attribute.Calendar.MONTH);
        
        double[] inputValues = this.inputValues.getValue();
        double[] outputValues = new double[inputValues.length];
        //  double correctionFactor = this.correctionFactor.getValue();
        //  double correctionMonth = this.correctionMonth.getValue();

 // (time.get(time.MONTH) + 1);
  //int nowmonth = (time.get(time.MONTH) + 1 );

        
        for (int i = 0; i < inputValues.length; i++) {
            if (this.correctionMonth.getValue() == 1.0) {
                outputValues[i] = inputValues[i] * correctionYear.getValue();
            } else {
                if ((nowmonth >= this.startMonth.getValue()) && (nowmonth <= this.endMonth.getValue())) {
                    outputValues[i] = inputValues[i] * this.correctionMonth.getValue();
                } else {
                    outputValues[i] = inputValues[i];
                }                
            }
        }
        
        this.outputValues.setValue(outputValues);
    }
  
   
    
    public static void main(String[] args) {
        PrecipCorrectionSimple1 comp = new PrecipCorrectionSimple1();
        comp.correctionMonth = DefaultDataFactory.getDataFactory().createDouble();
        comp.correctionYear = DefaultDataFactory.getDataFactory().createDouble();
        comp.inputValues = DefaultDataFactory.getDataFactory().createDoubleArray();
        comp.inputValues.setValue(new double[]{1, 2, 3, 2, 1, 6, 5, 4, 3, 2});
        comp.outputValues = DefaultDataFactory.getDataFactory().createDoubleArray();
        comp.time = DefaultDataFactory.getDataFactory().createCalendar();

        System.out.println("Test case 1 yearly correction activated");
        System.out.println("Input:" + Arrays.toString(comp.inputValues.getValue()));
        comp.correctionYear.setValue(1.33);
        comp.correctionMonth.setValue(1.0);
        comp.time.set(2000, 1, 12, 12, 0, 0);
        comp.run();
        System.out.println("Output@" + comp.time.toString() + ":"+ Arrays.toString(comp.outputValues.getValue()));
        comp.time.set(2000, 5, 12, 12, 0, 0);
        comp.run();
        System.out.println("Output@" + comp.time.toString() + ":"+ Arrays.toString(comp.outputValues.getValue()));
        comp.time.set(2000, 7, 12, 12, 0, 0);
        comp.run();
        System.out.println("Output@" + comp.time.toString() + ":"+ Arrays.toString(comp.outputValues.getValue()));
        comp.time.set(2000, 10, 12, 12, 0, 0);                
        comp.run();
        System.out.println("Output@" + comp.time.toString() + ":"+ Arrays.toString(comp.outputValues.getValue()));
        
        System.out.println("Test case 2 monthly correction activated");
        System.out.println("Input:" + Arrays.toString(comp.inputValues.getValue()));
        comp.correctionYear.setValue(1.0);
        comp.correctionMonth.setValue(0.77);
        comp.time.set(2000, 1, 12, 12, 0, 0);
        comp.run();
        System.out.println("Output@" + comp.time.toString() + ":"+ Arrays.toString(comp.outputValues.getValue()));
        comp.time.set(2000, 5, 12, 12, 0, 0);
        comp.run();
        System.out.println("Output@" + comp.time.toString() + ":"+ Arrays.toString(comp.outputValues.getValue()));
        comp.time.set(2000, 7, 12, 12, 0, 0);
        comp.run();
        System.out.println("Output@" + comp.time.toString() + ":"+ Arrays.toString(comp.outputValues.getValue()));
        comp.time.set(2000, 10, 12, 12, 0, 0);                
        comp.run();
        System.out.println("Output@" + comp.time.toString() + ":"+ Arrays.toString(comp.outputValues.getValue()));
    }
}
