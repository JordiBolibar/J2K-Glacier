/*
 * WasserfallEntityReader.java
 *
 * Created on 5. Juni 2008, 14:22
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
package org.unijena.j2k.io;

import java.io.*;
import org.unijena.j2k.*;
import jams.data.*;
import jams.model.*;
import java.util.*;
import jams.JAMS;
import java.lang.Math.*;
import jams.tools.FileTools;

/**
 *
 * @author S. Kralisch, fuer mehrdimensionale Topologie modifiziert von D.Varga und B.Pfennig
 * 09.10.2008
 */
public class MultiEntityReaderTS extends JAMSComponent {

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
    description = "HRU parameter file name")
    public Attribute.String hruFileName;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
    description = "Reach parameter file name")
    public Attribute.String reachFileName;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
    description = "Parameter file name for topological linkage with receiver entities")
    public Attribute.String to_hru_FileName;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
    description = "Parameter file name for weighting of contribution area to receiver entities")
    public Attribute.String bfl_FileName;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.WRITE,
    description = "Collection of hru objects")
    public Attribute.EntityCollection hrus;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.WRITE,
    description = "Collection of reach objects")
    public Attribute.EntityCollection reaches;
    @JAMSVarDescription(access = JAMSVarDescription.AccessType.WRITE,
    description = "Collection of hru objects with their topology")
    public Attribute.EntityCollection topology;

    public void init() throws Attribute.Entity.NoSuchAttributeException, FileNotFoundException, IOException {

         //read hru parameter        
        ArrayList<Attribute.Entity> hruCollection = J2KFunctions.readParas(FileTools.createAbsoluteFileName(getModel().getWorkspaceDirectory().getPath(), hruFileName.getValue()), getModel());
                
        //assign IDs to all hru entities
        for (Attribute.Entity e : hruCollection) {
            try {
                e.setId((long) e.getDouble(hruFileName.getValue()));
            } catch (Attribute.Entity.NoSuchAttributeException nsae) {
                getModel().getRuntime().sendErrorMsg("Couldn't find attribute \"ID\" while reading J2K HRUu parameter file (" + hruFileName.getValue() + ")!");
            }
        }
        hrus.setEntities(hruCollection);
        
        //read reach parameter
        ArrayList<Attribute.Entity> reachCollection = J2KFunctions.readParas(FileTools.createAbsoluteFileName(getModel().getWorkspaceDirectory().getPath(), reachFileName.getValue()), getModel());
        
        //assign IDs to all reach entities
        for (Attribute.Entity e : reachCollection) {
            try {
                e.setId((long) e.getDouble(reachFileName.getValue()));
            } catch (Attribute.Entity.NoSuchAttributeException nsae) {
                getModel().getRuntime().sendErrorMsg("Couldn't find attribute \"ID\" while reading J2K Reach parameter file (" + reachFileName.getValue() + ")!");
            }
        }
        
        reaches.setEntities(reachCollection);
        //create object associations from id attributes for hrus and reaches
        createTopology();

        //create total order on hrus and reaches that allows processing them subsequently
        getModel().getRuntime().println("Create ordered reach-list", JAMS.VERBOSE);
        createOrderedList(reaches, "to_reach");
        getModel().getRuntime().println("Create ordered hru-list", JAMS.VERBOSE);
        createOrderedList(hrus, "to_poly");
        getModel().getRuntime().println("Entities read successfull!", JAMS.VERBOSE);

    }

    private void createTopology() {

        BufferedReader reader1, reader2;
        HashMap<Double, Attribute.Entity> hruMap = new HashMap<Double, Attribute.Entity>();
        HashMap<Double, Attribute.Entity> reachMap = new HashMap<Double, Attribute.Entity>();
        Iterator<Attribute.Entity> hruIterator;
        Iterator<Attribute.Entity> reachIterator;
        Attribute.Entity aktuelleHRU, aktuellerReach, zielHRU, zielReach;
        ArrayList<Attribute.Entity> toHRUsArrayList;
        ArrayList<Attribute.Entity> toReachesArrayList;
        ArrayList<Double> toHRUsWeightsArrayList, toReachesWeightsArrayList, toHRUsBFlArrayList;

        //put all entities into a HashMap with their ID as key
        hruIterator = hrus.getEntities().iterator();
        while (hruIterator.hasNext()) {
            aktuelleHRU = hruIterator.next();
            hruMap.put(aktuelleHRU.getDouble("ID"), aktuelleHRU);
        }

        reachIterator = reaches.getEntities().iterator();
        while (reachIterator.hasNext()) {
            aktuellerReach = reachIterator.next();
            reachMap.put(aktuellerReach.getDouble("ID"), aktuellerReach);
        }

        //create empty entities, i.e. those that are linked to in case there is no linkage ;-)
        /*Attribute.Entity nullEntity = (Attribute.Entity) getModel().getRuntime().getDataFactory().createInstance(Attribute.Entity.class, getModel().getRuntime());
        nullEntity.setValue((HashMap<String, Object>) null);
        reachMap.put(new Integer(0), nullEntity);*/
        
        Attribute.Entity nullEntity = getModel().getRuntime().getDataFactory().createEntity();
        hruMap.put(new Double(0), nullEntity);
        reachMap.put(new Double(0), nullEntity);
        
        try {
            reader1 = new BufferedReader(new FileReader(getModel().getWorkspaceDirectory().getPath() + "/" + to_hru_FileName.getValue()));
            reader2 = new BufferedReader(new FileReader(getModel().getWorkspaceDirectory().getPath() + "/" + bfl_FileName.getValue()));

            String toHRUsLine = "#";
            while (toHRUsLine.startsWith("#")) {
                toHRUsLine = reader1.readLine();
            }

            String toHRUsWeightsLine = "#";
            while (toHRUsWeightsLine.startsWith("#")) {
                toHRUsWeightsLine = reader2.readLine();
            }

            while ((toHRUsLine != null) && !toHRUsLine.startsWith("#")) {
                //String zeichenkette = "\t";
                String zeichenkette = ",-8888.000,";

                String[] toHRUsSplitArray = toHRUsLine.split(zeichenkette);
                String[] toHRUsWeightsSplitArray = toHRUsWeightsLine.split(zeichenkette);

                int HRUsID = Integer.parseInt(toHRUsSplitArray[0]);
                double HRUsBFl = Double.parseDouble(toHRUsSplitArray[1]);
                String toHRUsString = toHRUsSplitArray[2];

                aktuelleHRU = hruMap.get(HRUsID);
                aktuelleHRU.setDouble("BFLDouble", HRUsBFl);

                int HRUsID2 = Integer.parseInt(toHRUsWeightsSplitArray[0]);
                String toHRUsWeightsString = toHRUsWeightsSplitArray[2];

                //Die Tabellen topologie_to_hru und topologie_bfl muessen identisch sortiert sein
                if (HRUsID != HRUsID2) {
                    getModel().getRuntime().sendHalt("Tabellen sind nicht gleich sortiert.");
                }

                toHRUsArrayList = new ArrayList<Attribute.Entity>();
                toReachesArrayList = new ArrayList<Attribute.Entity>();

                toHRUsWeightsArrayList = new ArrayList<Double>();
                toReachesWeightsArrayList = new ArrayList<Double>();

                toHRUsBFlArrayList = new ArrayList<Double>();

                //Tokenizer zum Unterteilen bei ","
                StringTokenizer toHRUsToken = new StringTokenizer(toHRUsString, ","); //Tokenizer fuer Empfaenger-HRUs
                StringTokenizer toHRUsWeightsToken = new StringTokenizer(toHRUsWeightsString, ","); //Tokenizer fuer die Wichtungen der Empfaenger-HRUs

                boolean tschuessnull = false;
                double sumWeights = 1;

                while (toHRUsToken.hasMoreTokens() && tschuessnull == false) {
                    String stringID = toHRUsToken.nextToken();
                    double toHRUsID = Double.parseDouble(stringID);

                    String stringWeight = toHRUsWeightsToken.nextToken();
                    double toHRUsWeight = Double.parseDouble(stringWeight);

                    sumWeights = 0.0001 * Math.round((sumWeights - toHRUsWeight) * 10000);

                    if ((toHRUsID == 0 && toHRUsWeight != 0) || (toHRUsID != 0 && toHRUsWeight == 0)) {
                        getModel().getRuntime().sendHalt("Fehler bei HRU " + HRUsID + ". Anzahl der Empfaenger-HRUs und der Wichtungen stimmen nicht ueberein.");
                    }

                    //for receiver HRUs
                    if (toHRUsID > 0) {
                        zielHRU = hruMap.get(toHRUsID);
                        toHRUsArrayList.add(zielHRU);
                        toHRUsWeightsArrayList.add(toHRUsWeight);
                    }

                    //for receiver Reaches
                    if (toHRUsID < 0) {
                        double toReachesID = toHRUsID * (-1);
                        zielReach = reachMap.get(toReachesID);
                        toReachesArrayList.add(zielReach);
                        toReachesWeightsArrayList.add(toHRUsWeight);
                    }

                    if (toHRUsID == 0) {
                        tschuessnull = true;
                    }
                }

                sumWeights = Math.abs(sumWeights);
                if (sumWeights >= 0.001) {
                    getModel().getRuntime().sendHalt("Fehler bei HRU " + HRUsID + ". Summe der einzelnen Gewichte ungleich 1");
                }

                //converting the ArrayLists into Arrays
                Attribute.Entity[] toHRUsArray = toHRUsArrayList.toArray(new Attribute.Entity[toHRUsArrayList.size()]);
                Attribute.Entity[] toReachesArray = toReachesArrayList.toArray(new Attribute.Entity[toReachesArrayList.size()]);

                Double[] toHRUsWeightsArray = toHRUsWeightsArrayList.toArray(new Double[toHRUsWeightsArrayList.size()]);
                Double[] toReachesWeightsArray = toReachesWeightsArrayList.toArray(new Double[toReachesWeightsArrayList.size()]);

                Double[] toHRUsBflArray = toHRUsBFlArrayList.toArray(new Double[toHRUsWeightsArrayList.size()]);

                //creating new Objects for each entity
                aktuelleHRU.setObject("to_poly", toHRUsArray);
                aktuelleHRU.setObject("to_reach", toReachesArray);

                aktuelleHRU.setObject("to_poly_weights", toHRUsWeightsArray);
                aktuelleHRU.setObject("to_reach_weights", toReachesWeightsArray);

                aktuelleHRU.setObject("bfl", toHRUsBflArray);

                sumWeights = 1;

                //Auslesen der jeweils naechsten Zeile
                toHRUsLine = reader1.readLine();
                toHRUsWeightsLine = reader2.readLine();
            }

        } catch (IOException ioe) {
            getModel().getRuntime().handle(ioe);
        }

        //associate the reach entities with their downstream entity
        reachIterator = reaches.getEntities().iterator();
        while (reachIterator.hasNext()) {
            aktuellerReach = reachIterator.next();
            aktuellerReach.setObject("to_reach", reachMap.get((int)aktuellerReach.getDouble("to-reach")));
        }
    }

    protected void createOrderedList(Attribute.EntityCollection hrus, String asso) throws Attribute.Entity.NoSuchAttributeException, FileNotFoundException, IOException {


        Iterator<Attribute.Entity> hruIterator;
        Iterator<Attribute.Entity> hruIterator2;

        Attribute.Entity aktuelleHRU = null;
        ArrayList<Attribute.Entity> hruList = new ArrayList<Attribute.Entity>();

        HashMap<Attribute.Entity, Integer> statusMap = new HashMap<Attribute.Entity, Integer>();
        HashMap<Attribute.Entity, Attribute.Entity> fromHRUMap = new HashMap<Attribute.Entity, Attribute.Entity>();
        HashMap<Attribute.Entity, Integer> fromIMap = new HashMap<Attribute.Entity, Integer>();
        
        HashMap<Attribute.Entity, Integer> depthMap = new HashMap<Attribute.Entity, Integer>();

        Integer eDepth, fDepth;
        boolean aufloesbar = false, unaufloesbar = false, geaendert;
        BufferedWriter writer2;

        //Identifikation und Aufloesung von Zirkeln
        /*/if ((asso.toString()).equals("to_poly")) {

            writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getModel().getWorkspaceDirectory().getPath() + "/unaufloesbareZirkel.txt")));

            Attribute.Entity aktuelleHRU = null;

            hruIterator = hrus.getEntities().iterator();

            marke:
            while (hruIterator.hasNext()) {
                if (aufloesbar == false) {
                    aktuelleHRU = hruIterator.next();
                }

                System.out.println("Untersuche HRU " + (int)aktuelleHRU.getDouble("ID") + " auf Zirkelbezuege");

                aufloesbar = false;
                Attribute.Entity zielHRU_neu = aktuelleHRU;

                hruIterator2 = hrus.getEntities().iterator();
                while (hruIterator2.hasNext()) {
                    Attribute.Entity initialisierungHRU = hruIterator2.next();
                    statusMap.put(initialisierungHRU, new Integer(0)); //Status 0: Noch nicht markiert, Status 1: Markiert, Bearbeitung aber noch nicht gestartet, Status -2: Bearbeitung gestartet, aber ncch nicht abgeschlossen, STatus -3: Bearabeitung abgeschlossen
                    fromHRUMap.put(initialisierungHRU, null);
                    fromIMap.put(initialisierungHRU, new Integer(0));
                }

                while (statusMap.get(aktuelleHRU) != -3) {
                    Attribute.Entity zielHRU = zielHRU_neu;

                    statusMap.put(zielHRU, new Integer(-2));
                    Attribute.Entity[] zielHRU_toHRU = (Attribute.Entity[]) zielHRU.getObject("to_poly");

                    if (zielHRU_toHRU.length > 0) {

                        for (int i = 0; i < zielHRU_toHRU.length; i++) {
                            Attribute.Entity untersuchteHRU = zielHRU_toHRU[i];

                            if (untersuchteHRU != null) {

                                Double[] zielHRUsWeights = (Double[]) zielHRU.getObject("to_poly_weights");
                                Double[] zielHRU_toHRU_BFl = (Double[]) zielHRU.getObject("bfl");
                                zielHRU_toHRU_BFl[i] = zielHRU.getDouble("BFLDouble") * zielHRUsWeights[i];
                                zielHRU.setObject("bfl", zielHRU_toHRU_BFl);

                                if (aktuelleHRU == untersuchteHRU) {
                                    // Identifikation der kleinsten Beitragenden Flaeche
                                    Attribute.Entity zirkelHRU = zielHRU, HRU_BflMin = null;
                                    int iZirkel = i, iZirkelMin = -1, teilerMin = -1;
                                    double bflZirkel, bflZirkelMin = -1;

                                    while (zirkelHRU != null) {
                                        Attribute.Entity[] zirkelHRU_toReaches = (Attribute.Entity[]) zirkelHRU.getObject("to_reach");
                                        Attribute.Entity[] zirkelHRU_toHRU = (Attribute.Entity[]) zirkelHRU.getObject("to_poly");

                                        int zirkelHRU_Richtungen = 0;
                                        for (int l = 0; l < zirkelHRU_toHRU.length; l++) {
                                            
                                            if (zirkelHRU_toHRU[l] != null) {
                                                zirkelHRU_Richtungen += 1;
                                            }
                                        }
                                        
                                        if (zirkelHRU_Richtungen + zirkelHRU_toReaches.length > 1) {
                                            Double[] zirkelHRU_toHRU_BFl = (Double[]) zirkelHRU.getObject("bfl");
                                            bflZirkel = zirkelHRU_toHRU_BFl[iZirkel];
                                            
                                            if (bflZirkelMin == -1 || bflZirkel < bflZirkelMin) {
                                                aufloesbar = true;
                                                bflZirkelMin = bflZirkel;
                                                HRU_BflMin = zirkelHRU;
                                                iZirkelMin = iZirkel;
                                                teilerMin = zirkelHRU_Richtungen;
                                            }
                                        }
                                        iZirkel = fromIMap.get(zirkelHRU);
                                        zirkelHRU = fromHRUMap.get(zirkelHRU);
                                    }

                                    //Zirkelaufloesung
                                    if (HRU_BflMin != null && iZirkelMin != -1 && teilerMin != -1) {

                                        Attribute.Entity[] HRU_BFlMin_toHRUs = (Attribute.Entity[]) HRU_BflMin.getObject("to_poly");
                                        Attribute.Entity[] HRU_BFlMin_toReaches = (Attribute.Entity[]) HRU_BflMin.getObject("to_reach");                                        

                                        Double[] HRU_BFlMin_toHRUsWeights = (Double[]) HRU_BflMin.getObject("to_poly_weights");
                                        Double[] HRU_BFlMin_toReachesWeights = (Double[]) HRU_BflMin.getObject("to_reach_weights");                                        

                                        int HRU_BFlMin_toHRUsID = (int)HRU_BFlMin_toHRUs[iZirkelMin].getDouble("ID");

                                        if (HRU_BFlMin_toReaches.length > 0) {
                                            int HRU_BFlMin_toReachesID = (int)HRU_BFlMin_toReaches[0].getDouble("ID");

                                            //Uebergabe des Wassers der zirkelausloesenden Flaeche an ggf. verknuepfte Fliessgewaesserabschnitte
                                            HRU_BFlMin_toReachesWeights[0] = HRU_BFlMin_toReachesWeights[0] + HRU_BFlMin_toHRUsWeights[iZirkelMin];
                                            HRU_BflMin.setObject("to_reach_weights", HRU_BFlMin_toReachesWeights);

                                            System.out.println("HRU " + (int)HRU_BflMin.getDouble("ID") + ": Unterbrechung der Fliessbeziehung zu HRU " + HRU_BFlMin_toHRUsID + ". Umleitung in das Fliessgewaessersegment -" + HRU_BFlMin_toReachesID + "; Beitragende Flaeche: " + Math.round(HRU_BflMin.getDouble("BFLDouble") * HRU_BFlMin_toHRUsWeights[iZirkelMin] / 1000.) / 1000. + " qkm");
                                        } else {

                                            //Uebergabe des Wassers der zirkelausloesenden Flaeche an die anderen Fliessbeziehungen der gleichen HRU
                                            for (int s = 0; s < HRU_BFlMin_toHRUs.length; s++) {
                                                
                                                if (HRU_BFlMin_toHRUs[s] != null && s != iZirkelMin) {
                                                    HRU_BFlMin_toHRUsWeights[s] = HRU_BFlMin_toHRUsWeights[s] + HRU_BFlMin_toHRUsWeights[iZirkelMin] / (teilerMin - 1);
                                                }
                                            }
                                            System.out.println("HRU " + (int)HRU_BflMin.getDouble("ID") + ": Unterbrechung der Fliessbeziehung zu HRU " + HRU_BFlMin_toHRUsID + ". Verteilung des Wassers auf alle anderen Fliessbeziehungen der HRU;" + " Beitragende Flaeche: " + Math.round(HRU_BflMin.getDouble("BFLDouble") * HRU_BFlMin_toHRUsWeights[iZirkelMin] / 1000.) / 1000. + " qkm");
                                        }
                                        HRU_BFlMin_toHRUs[iZirkelMin] = null;
                                        HRU_BflMin.setObject("to_poly", HRU_BFlMin_toHRUs);
                                        HRU_BFlMin_toHRUsWeights[iZirkelMin] = null;
                                        HRU_BflMin.setObject("to_poly_weights", HRU_BFlMin_toHRUsWeights);
                                    } else {
                                        unaufloesbar = true;
                                        System.out.println("Nicht aufloesbarer Zirkel! Fliessbeziehung von HRU " + (int)zielHRU.getDouble("ID") + " zu HRU " + (int)zielHRU_toHRU[i].getDouble("ID") + " kann nicht unterbrochen werden.");
                                        //JOptionPane.showMessageDialog(null, "Nicht aufloesbarer Zirkel! Fliessbeziehung von HRU " + (int)zielHRU.getDouble("ID") + " zu HRU " + (int)zielHRU_toHRU[i].getDouble("ID") + " kann nicht unterbrochen werden.");
                                        writer2.write("Nicht aufloesbarer Zirkel! Fliessbeziehung von HRU " + (int)zielHRU.getDouble("ID") + " zu HRU " + (int)zielHRU_toHRU[i].getDouble("ID") + " kann nicht unterbrochen werden.");
                                        writer2.newLine();
                                    }
                                    continue marke; //Die Sprungmarke ist deshalb notwendig, da eine Fliessbeziehung unterbrochen wurde und deshalb der Weg beim Backtracking nicht zurueckverfolgt werden koennte. Deshalb wird die Entitaet erneut von vorn abgearbeitet.
                                }

                                if (statusMap.get(untersuchteHRU) == 0) {
                                    statusMap.put(untersuchteHRU, new Integer(1));
                                    fromHRUMap.put(untersuchteHRU, zielHRU);
                                    fromIMap.put(untersuchteHRU, new Integer(i));
                                }
                            }
                        }

                        for (int i = 0; i < zielHRU_toHRU.length; i++) {
                            Attribute.Entity tempHRU = zielHRU_toHRU[i];

                            if (statusMap.get(tempHRU) != null && statusMap.get(tempHRU) == 1 && zielHRU == fromHRUMap.get(tempHRU)) {
                                zielHRU_neu = tempHRU;
                            }
                        }
                    }

                    //Backtracking
                    int freieRichtungen = 0;

                    for (int i = 0; i < zielHRU_toHRU.length; i++) {
                        Attribute.Entity tempHRU2 = zielHRU_toHRU[i];

                        if (statusMap.get(tempHRU2) != null && statusMap.get(tempHRU2) >= 0) {

                            if (statusMap.get(tempHRU2) == 1 && zielHRU == fromHRUMap.get(tempHRU2)) {
                                freieRichtungen += 1;
                            }
                        }
                    }

                    if (freieRichtungen == 0) {
                        statusMap.put(zielHRU, new Integer(-3));
                        zielHRU_neu = fromHRUMap.get(zielHRU);
                    }
                }
            }

            writer2.close();

            //Abbruch wegen nicht aufloesbaren Zirkels
            if (unaufloesbar == true) {
                System.out.println("Nicht aufloesbare Zirkel in HRU-Muster");
                JOptionPane.showMessageDialog(null, "Nicht aufloesbare Zirkel in HRU-Muster");
                System.exit(-1);
            } else {
                new File((getModel().getWorkspaceDirectory().getPath() + "/unaufloesbareZirkel.txt")).delete();
            }
        }/*/

        // Aufbau der Topologie
        geaendert = true;
        
        hruIterator = hrus.getEntities().iterator();
        while (hruIterator.hasNext()) {
            depthMap.put(hruIterator.next(), new Integer(0));
        }

        //put all collection elements (keys) and their maximum depth (values) into a HashMap
        while (geaendert) {
            geaendert = false;
            
            hruIterator = hrus.getEntities().iterator();
            while (hruIterator.hasNext()) {
                aktuelleHRU = hruIterator.next();
                eDepth = depthMap.get(aktuelleHRU);

                if ((asso.toString()).equals("to_poly")) {
                    Attribute.Entity[] aktuelleHRU_toHRUs = (Attribute.Entity[]) aktuelleHRU.getObject(asso);

                    if (aktuelleHRU_toHRUs.length > 0) {

                        for (int i = 0; i < aktuelleHRU_toHRUs.length; i++) {

                            if (aktuelleHRU_toHRUs[i] != null) {
                                fDepth = depthMap.get(aktuelleHRU_toHRUs[i]);

                                if (fDepth.intValue() <= eDepth.intValue()) {
                                    depthMap.put(aktuelleHRU_toHRUs[i], new Integer(eDepth.intValue() + 1));
                                    geaendert = true;
                                }
                            }
                        }
                    }
                }

                if ((asso.toString()).equals("to_reach")) {
                    Attribute.Entity aktuelleHRU_toReaches = (Attribute.Entity) aktuelleHRU.getObject(asso);

                    if (aktuelleHRU_toReaches.isEmpty()) {
                        aktuelleHRU_toReaches = null;
                    }

                    if (aktuelleHRU_toReaches != null) {
                        fDepth = depthMap.get(aktuelleHRU_toReaches);

                        if (fDepth.intValue() <= eDepth.intValue()) {
                            depthMap.put(aktuelleHRU_toReaches, new Integer(eDepth.intValue() + 1));
                            geaendert = true;
                        }
                    }
                }
            }
        }

        //find out which is the max depth of all entities
        int maxDepth = 0;

        hruIterator = hrus.getEntities().iterator();
        while (hruIterator.hasNext()) {
            aktuelleHRU = hruIterator.next();
            maxDepth = Math.max(maxDepth, depthMap.get(aktuelleHRU).intValue());
        }
        //create ArrayList of ArrayList objects, each element keeping the entities of one level
        //ArrayList<ArrayList<Attribute.Entity>> alList = new ArrayList<ArrayList<Attribute.Entity>>();
        ArrayList<ArrayList<Attribute.Entity>> alList = new ArrayList<ArrayList<Attribute.Entity>>();


        for (int i = 0; i <= maxDepth; i++) {
            alList.add(new ArrayList<Attribute.Entity>());
        }

        //fill the ArrayList objects within the ArrayList with entity objects
        hruIterator = hrus.getEntities().iterator();
        while (hruIterator.hasNext()) {
            aktuelleHRU = hruIterator.next();
            int depth = depthMap.get(aktuelleHRU).intValue();
            alList.get(depth).add(aktuelleHRU);
        }

        for (int i = 0; i <= maxDepth; i++) {

            hruIterator = alList.get(i).iterator();
            while (hruIterator.hasNext()) {
                aktuelleHRU = hruIterator.next();
                hruList.add(aktuelleHRU);
            }
        }
        hrus.setEntities(hruList);
    }
}