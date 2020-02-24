/*
 * MODIS_ET_Reader.java
 * Created on 13.04.2016, 10:33:10
 *
 * This file is part of JAMS
 * Copyright (C) FSU Jena
 *
 * JAMS is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * JAMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JAMS. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package jams.components.io;

import jams.data.*;
import jams.model.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 *
 * @author Sven Kralisch <sven.kralisch at uni-jena.de>
 */
@JAMSComponentDescription(
        title = "MODIS_ET_Reader",
        author = "Sven Kralisch",
        description = "Read TAB-separated MODIS ET data generated by MODIS processor",
        date = "2016-04-13",
        version = "1.0_1"
)
@VersionComments(entries = {
    @VersionComments.Entry(version = "1.0_0", date = "2016-04-13", comment = "Initial version"),
    @VersionComments.Entry(version = "1.0_1", date = "2016-07-01", comment = "Fixed file read error at end of file")
})
public class MODIS_ET_Reader extends JAMSComponent {

    /*
     *  Component attributes
     */
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "MODIS input file name"
    )
    public Attribute.String modisFileName;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "current model time"
    )
    public Attribute.Calendar time;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "Output values to info log?",
            defaultValue = "false"
    )
    public Attribute.Boolean debug;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "MODIS ET values of current time step"
    )
    public Attribute.DoubleArray modisETArray;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "Array of HRU IDs from MODIS file"
    )
    public Attribute.IntegerArray hruIDArray;

    transient BufferedReader reader;
    int startMonth = 0, cMonth = 0, oldMonth = 0;
    String line = "";
    List<Integer> idList = new ArrayList();
    List<Double> valueList = new ArrayList();
    int[] idArray;
    double[] valueArray;

    /*
     *  Component run stages
     */
    @Override
    public void init() {

        // get the correct filename
        File file;
        File f = new File(modisFileName.getValue());
        if (f.isAbsolute()) {
            file = new File(modisFileName.getValue());
        } else {
            file = new File(getModel().getWorkspacePath(), modisFileName.getValue());
        }

        // open the file and create a reader
        // then read the start month of the MODIS 
        try {

            reader = new BufferedReader(new FileReader(file));
            reader.readLine();

            line = reader.readLine();
            String[] parts = line.split("\t");
            startMonth = Integer.parseInt(parts[0]) * 12 + Integer.parseInt(parts[1]);
            cMonth = startMonth;

        } catch (IOException ex) {
            getModel().getRuntime().handle(ex);
        }
    }

    @Override
    public void run() {

        int month = time.get(Calendar.YEAR) * 12 + time.get(Calendar.MONTH) + 1;

        if (oldMonth == month) {
            return;
        }

        if (startMonth > month) {
            modisETArray.setValue(new double[0]);
            return;
        }

        // forward read until current date is matching file pointer
        while (true) {

            String[] parts = line.split("\t");
            cMonth = Integer.parseInt(parts[0]) * 12 + Integer.parseInt(parts[1]);

            if (cMonth < month) {
                try {
                    line = reader.readLine();
                } catch (IOException ex) {
                    getModel().getRuntime().handle(ex);
                }
            } else {
                break;
            }
        }

        if (cMonth == month) {
            oldMonth = month;
        }

        // read all data of current month
        int i = 0;
        while (line != null) {

            String[] parts = line.split("\t");
            cMonth = Integer.parseInt(parts[0]) * 12 + Integer.parseInt(parts[1]);

            if (cMonth == month) {
                try {

                    int hruID = Integer.parseInt(parts[2]);
                    double value = Double.parseDouble(parts[3]);

                    if (idArray == null) {
                        idList.add(hruID);
                        valueList.add(value);
                    } else {
                        idArray[i] = hruID;
                        valueArray[i] = value;
                        i++;
                    }

                    line = reader.readLine();
                } catch (IOException ex) {
                    getModel().getRuntime().handle(ex);
                }
            } else {
                break;
            }

        }

        // on first invocation, create two arrays (values and ids) from lists
        // subsequent invocation will use the arrays directly
        if (idArray == null) {
            idArray = new int[idList.size()];
            for (int j = 0; j < idList.size(); j++) {
                idArray[j] = idList.get(j);
            }
            valueArray = new double[valueList.size()];
            for (int j = 0; j < valueList.size(); j++) {
                valueArray[j] = valueList.get(j);
            }
            modisETArray.setValue(valueArray);
            hruIDArray.setValue(idArray);
        }

        if (debug.getValue()) {
            String s = "";
            for (int id : hruIDArray.getValue()) {
                s = s + id + " - ";
            }
            getModel().getRuntime().println(s);
            s = "";
            for (double v : modisETArray.getValue()) {
                s = s + v + " - ";
            }
            getModel().getRuntime().println(s);
        }

    }

    @Override
    public void cleanup() {
        try {
            reader.close();
        } catch (IOException ex) {
            getModel().getRuntime().handle(ex);
        }
    }

    // for testing purposes...
    public static void main(String[] args) {

        MODIS_ET_Reader r = new MODIS_ET_Reader();
        DataFactory f = DefaultDataFactory.getDataFactory();
        r.modisFileName = f.createString();
        r.modisFileName.setValue("D:\\jamsmodeldata\\JAMS-BJ_2\\input\\local\\modis-results\\modis.csv");
        r.time = f.createCalendar();
        r.modisETArray = f.createDoubleArray();
        r.hruIDArray = f.createIntegerArray();

        r.init();

        r.time.setValue("2002/1/1 12:00");
        r.run();

        for (int id : r.hruIDArray.getValue()) {
            System.out.print(id + " - ");
        }
        System.out.println("");
        for (double v : r.modisETArray.getValue()) {
            System.out.print(v + " - ");
        }
        System.out.println("");

        r.time.setValue("2002/2/1 12:00");
        r.run();

        for (int id : r.hruIDArray.getValue()) {
            System.out.print(id + " - ");
        }
        System.out.println("");
        for (double v : r.modisETArray.getValue()) {
            System.out.print(v + " - ");
        }

        r.cleanup();

    }
}