/*
 * ABCDataReader.java
 * Created on 21. März 2007, 17:25
 *
 * This file is part of JAMS
 * Copyright (C) 2006 FSU Jena
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
package jams.components.demo.abc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import jams.data.*;
import jams.model.*;
import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author Sven Kralisch
 */
@JAMSComponentDescription(title = "ABCModel precip reader",
author = "Sven Kralisch",
description = "ABC model climate data reader",
date = "17.11.2010",
version = "1.0.0")
public class ABCDataReader extends JAMSComponent {

    /*
     *  Component variables
     */
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
                        description = "Input data file name")
                        public Attribute.String fileName;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.WRITE,
                        description = "Precip value read from file")
                        public Attribute.Double precip;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.WRITE,
                        description = "Runoff value read from file")
                        public Attribute.Double runoff;
    
    
    ArrayList<double[]> data = new ArrayList<double[]>();
    int counter = 0;
    /*
     *  Component run stages
     */
    @Override
    public void init() {
        BufferedReader reader=null;
        try {            
            if (fileName == null) {
                getModel().getRuntime().sendHalt("You should specify a file for ABCDataReader");
                return;
            } 
            File file = new File(this.getModel().getWorkspaceDirectory(), fileName.getValue());
            
            if (!file.isFile()) {
                getModel().getRuntime().sendHalt("The  file " + fileName.getValue() + " is not existing. Aborting.");
            }
            reader = new BufferedReader(new FileReader(file));
            while (!reader.readLine().startsWith("="));
            
            String line, token;
            while ((line = reader.readLine()) != null){
            if (line == null) {
                getModel().getRuntime().sendHalt("There is no more data in" + " "
                        + this.fileName + ". Check your data file or timeInterval");
                return;
            }
            StringTokenizer st = new StringTokenizer(line);
            token = st.nextToken();
            //token = st.nextToken();
            
            double dataset[] = new double[2];
            dataset[0] = Double.parseDouble(st.nextToken());
            dataset[1] = Double.parseDouble(st.nextToken());
            data.add(dataset);
            }
            reader.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        counter=0;
    }

    @Override
    public void run() {
        precip.setValue(data.get(counter)[0]);
        runoff.setValue(data.get(counter)[1]);
        counter++;
    }

    @Override
    public void cleanup() {
        
    }
}
