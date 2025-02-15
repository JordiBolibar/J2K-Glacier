/*
 * XYPlot.java
 * Created on 12. October 2006, 22:06
 *
 * This file is part of JAMS
 * Copyright (C) 2005 FSU Jena
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
package jams.components.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import jams.model.JAMSGUIComponent;
import jams.model.JAMSVarDescription;
import jams.data.*;
import jams.model.JAMSComponentDescription;
import jams.model.VersionComments;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.jfree.data.xy.*;

/**
 *
 * @author S. Kralisch
 */
@JAMSComponentDescription(
        title = "SimpleXYPlot",
        author = "Sven Kralisch",
        description = "This component creates a graphical plot of x/y data, "
        + "e.g. evaptranspiration plotted against temperature.",
        date = "2018-09-14",
        version = "1.0_1"
)
@VersionComments(entries = {
    @VersionComments.Entry(version = "1.0_0", comment = "Initial version"),
    @VersionComments.Entry(version = "1.0_1", date = "2018-09-14", comment = "Some improvements")
})
public class SimpleXYPlot extends JAMSGUIComponent {

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "Title string for plot",
            defaultValue = "Plot Title"
    )
    public Attribute.StringArray plotTitle;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "Title string for x axis",
            defaultValue = "x axis"
    )
    public Attribute.String xAxisTitle;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "Title string for y axis",
            defaultValue = "y axis"
    )
    public Attribute.String yAxisTitle;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "Values on x axis"
    )
    public Attribute.Double xValue;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "Values on y axis"
    )
    public Attribute.Double[] yValue;

    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "Flag for disabling or enabling the plot",
            defaultValue = "true"
    )
    public Attribute.Boolean paint;

    transient private XYPlot plot;
    transient private XYSeries dataset[];
    transient private JPanel panel;
    transient JFreeChart chart;
    transient JButton saveButton;
    int i, graphCountLeft = 0, graphCountRight = 0;
    HashMap<Integer, Color> colorTable = new HashMap<Integer, Color>();

    public SimpleXYPlot() {
        colorTable.put(0, Color.red);
        colorTable.put(1, Color.blue);
        colorTable.put(2, Color.green);
        colorTable.put(3, Color.pink);
        colorTable.put(4, Color.magenta);
        colorTable.put(5, Color.cyan);
        colorTable.put(6, Color.yellow);
        colorTable.put(7, Color.green);
        colorTable.put(8, Color.gray);
        colorTable.put(9, Color.lightGray);
        colorTable.put(10, Color.black);

        panel = null;
    }

    private void initDataSets() {

        dataset = new XYSeries[yValue.length];

        for (int i = 0; i < dataset.length; i++) {
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            dataset[i] = new XYSeries(plotTitle.getValue()[i]);
            plot.setDataset(i, new XYSeriesCollection(dataset[i]));
            renderer.setSeriesLinesVisible(0, true);
            renderer.setSeriesShapesVisible(0, false);

            renderer.setSeriesPaint(0, colorTable.get(i));
            //renderer.set
            plot.setRenderer(i, renderer);
        }

    }

    private JPanel createPanel() {
        plot = new XYPlot();
        plot.setDomainAxis(new NumberAxis(xAxisTitle.getValue()));
        plot.setRangeAxis(new NumberAxis(yAxisTitle.getValue()));
//        initDataSets();
        chart = new JFreeChart(plot);

        ChartPanel chartPanel = new ChartPanel(chart, true);

        panel = new JPanel(new BorderLayout());
        panel.add(chartPanel, BorderLayout.CENTER);

        chart.getPlot().setBackgroundPaint(Color.white);
        chart.getXYPlot().setDomainGridlinePaint(Color.black);

        return panel;
    }

    public JPanel getPanel() {
        if (panel == null) {
            return createPanel();
        }
        return panel;
    }

    @Override
    public void init() {
        initDataSets();
    }

    @Override
    public void run() {
        if (dataset == null) {
            this.initDataSets();
        }
        if (this.paint == null || this.paint.getValue()) {
            for (int i = 0; i < dataset.length; i++) {
                dataset[i].add(this.xValue.getValue(), this.yValue[i].getValue());
            }
        }
    }

    @Override
    public void cleanup() {
//        saveButton.setEnabled(true);
    }

    private void readObject(ObjectInputStream objIn) throws IOException, ClassNotFoundException {
        objIn.defaultReadObject();

        createPanel();

        for (int i = 0; i < dataset.length; i++) {
            int n = objIn.readInt();
            for (int j = 0; j < n; j++) {
                XYDataItem item = dataset[i].getDataItem(j);
                double x = objIn.readDouble();
                double y = objIn.readDouble();
                dataset[i].add(x, y);
            }
        }
    }

    private void writeObject(ObjectOutputStream objOut) throws IOException {
        objOut.defaultWriteObject();

        for (int i = 0; i < dataset.length; i++) {
            int n = dataset[i].getItemCount();
            objOut.writeInt(n);
            for (int j = 0; j < n; j++) {
                XYDataItem item = dataset[i].getDataItem(j);
                objOut.writeDouble(item.getX().doubleValue());
                objOut.writeDouble(item.getY().doubleValue());
            }
        }
    }
}
