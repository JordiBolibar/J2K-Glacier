/*
 * ShapeEntityReader.java
 * Created on 22. Januar 2009, 21:32
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
package jams.components.io;

import com.vividsolutions.jts.algorithm.CentroidArea;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import jams.data.Attribute;
import jams.model.JAMSComponent;
import jams.model.JAMSComponentDescription;
import jams.model.JAMSVarDescription;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.AreaFunction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author Sven Kralisch <sven.kralisch at uni-jena.de>
 */
@JAMSComponentDescription (title = "ShapeEntityReader",
                           author = "Sven Kralisch",
                           description = "Reads a shape file and creates a " +
"list of JAMS entities containing an entity for each feature. An attribute " +
"name must be provided in order to identify the id field used in the shape file")
public class ShapeEntityReader extends JAMSComponent {

    @JAMSVarDescription (access = JAMSVarDescription.AccessType.READ,
                         description = "Name of the shape file")
    public Attribute.String shapeFileName;

    @JAMSVarDescription (access = JAMSVarDescription.AccessType.READ,
                         description = "Name of identifying attribute in shape file")
    public Attribute.String idName;

    @JAMSVarDescription (access = JAMSVarDescription.AccessType.READ,
                         description = "Name of the area attribute to be created",
                         defaultValue = "area")
    public Attribute.String areaAttribute;

    @JAMSVarDescription (access = JAMSVarDescription.AccessType.READ,
                         description = "Name of the x attribute to be created",
                         defaultValue = "x")
    public Attribute.String xAttribute;

    @JAMSVarDescription (access = JAMSVarDescription.AccessType.READ,
                         description = "Name of the y attribute to be created",
                         defaultValue = "y")
    public Attribute.String yAttribute;

    @JAMSVarDescription (access = JAMSVarDescription.AccessType.WRITE,
                         description = "Entity collection to be created")
    public Attribute.EntityCollection entities;

    @Override
    public void init() {
        System.out.println("ShapeEntityReader.init. shape-file name " + shapeFileName);
        System.out.println("ShapeEntityReader.init. shape-file id " + idName);
        String workSpaceDirectory = getModel().getWorkspaceDirectory().getPath();
        String fileName = shapeFileName.getValue();
        boolean isCompleteFileName = false;
        if (fileName.indexOf(":") > -1) {   // works only for windows
            isCompleteFileName = true;
        }
        if (!isCompleteFileName) {
            fileName = workSpaceDirectory + File.separator + fileName;
        }

        try {

            URL shapeUrl = (new java.io.File(fileName).toURI().toURL());
            System.out.println("ShapeEntityReader.init. try to get shape-file from " + shapeUrl.toString());
            ShapefileDataStore store = new ShapefileDataStore(shapeUrl);
            
            FeatureIterator<SimpleFeature> featureIterator = store.getFeatureSource().getFeatures().features();
            
            List<AttributeDescriptor> atts = store.getFeatureSource().getSchema().getAttributeDescriptors();
            
            int idAttributeIndex = -1;
            
            for (int i = 0; i < atts.size(); i++) {
                if (atts.get(i).getName().toString().equals(idName.getValue()) &&
                        ((atts.get(i).getType().getBinding() == Long.class) || (atts.get(i).getType().getBinding() == Integer.class))) {
                    idAttributeIndex = i;
                }
            }
            
            ArrayList<Attribute.Entity> entityList = new ArrayList<Attribute.Entity>();
            
            while (featureIterator.hasNext()) {
                SimpleFeature f = featureIterator.next();
                
                Attribute.Entity e = getModel().getRuntime().getDataFactory().createEntity();
                
                for (int i = 0; i < atts.size(); i++) {
                    e.setObject(atts.get(i).getName().toString(), getModel().getRuntime().getDataFactory().createInstance(f.getAttribute(i)));
                }
                Geometry geom = (Geometry) f.getDefaultGeometry();
                AreaFunction af = new AreaFunction();
                e.setDouble(areaAttribute.getValue(), af.getArea(geom));
                
                CentroidArea c2d = new CentroidArea();
                c2d.add(geom);
                Coordinate coord = c2d.getCentroid();
                e.setDouble(xAttribute.getValue(), coord.x);
                e.setDouble(yAttribute.getValue(), coord.y);
                //System.out.println("ShapeEntityReader.x/y: " + coord.x + "/" + coord.y);
                
                if (idAttributeIndex != -1) {
                    try {
                        long id = Long.parseLong(f.getAttribute(idAttributeIndex).toString());
                        e.setId(id);
                    } catch (NumberFormatException nfe) {
                        getModel().getRuntime().sendErrorMsg("Could not parse " + f.getAttribute(idAttributeIndex) + " as long value!");
                    }
                }
                entityList.add((Attribute.Entity) e);
            }

            entities.setEntities(entityList);
        } catch (IOException ex) {
            getModel().getRuntime().sendErrorMsg("An error occured while trying to load entities from " + new java.io.File(fileName).getAbsolutePath() + 
                    " (" + ex.getMessage() + ")");
        }
    }
}
