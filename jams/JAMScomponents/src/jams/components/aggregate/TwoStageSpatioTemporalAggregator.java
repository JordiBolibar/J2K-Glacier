/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jams.components.aggregate;

import jams.JAMS;
import jams.aggregators.Aggregator;
import jams.aggregators.Aggregator.AggregationMode;
import jams.aggregators.CompoundTemporalAggregator;
import jams.aggregators.DoubleIteratorAggregator;
import jams.aggregators.MultiTemporalAggregator;
import jams.aggregators.TemporalAggregator;
import jams.aggregators.TemporalAggregator.AggregationTimePeriod;
import jams.data.Attribute;
import jams.model.JAMSComponentDescription;
import jams.model.JAMSVarDescription;
import java.text.MessageFormat;

/**
 *
 * @author christian
 */
@JAMSComponentDescription(
        title = "SpatioTemporalAggregator",
        author = "Christian Fischer",
        description = "Aggregates timeseries values to a given time period of day, month, year or dekade")

public class TwoStageSpatioTemporalAggregator extends SpatioTemporalAggregator {

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "aggregationMode: sum; avg; min; max; ind;")
    public Attribute.String[] outerAggregationMode;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "The reference time period for aggregation, e.g. yearly mean of months mean, possible values are: hourly, daily, monthly, seasonal, halfyear, hydhalfyear, yearly, decadly")
    public Attribute.String outerTimeUnit;

    private AggregationTimePeriod outerTimeUnitID = AggregationTimePeriod.YEARLY;
    private AggregationMode outerAggregationModeID[] = null;
        
    protected AggregationMode getOuterAggregationModeID(int i) {
        return outerAggregationModeID[i];
    }

    protected AggregationTimePeriod getOuterTimeUnitID() {
        return outerTimeUnitID;
    }
    
    @Override
    protected boolean checkConfiguration() {
        super.checkConfiguration();
        //check for consistency        
        if (outerAggregationMode != null && outerAggregationMode.length != n) {
            getModel().getRuntime().sendInfoMsg("Number of values in parameter \"outerAggregationMode\" does not match the number of attributes");
            return false;
        }

        if (outerTimeUnitID == null) {
            getModel().getRuntime().sendErrorMsg(MessageFormat.format(JAMS.i18n("Unknown time unit:" + outerTimeUnit.getValue() + ".\nPossible values are daily, monthly, yearly and decadly."), getInstanceName()));
        }
        return true;
    }
    
    @Override
    public void init() {
        super.init();
                
        outerTimeUnitID = AggregationTimePeriod.fromString(outerTimeUnit.getValue());
        if (outerTimeUnitID == null) {
            getModel().getRuntime().sendErrorMsg(MessageFormat.format(JAMS.i18n("Unknown time unit:" + outerTimeUnit.getValue() + ".\nPossible values are daily, monthly, yearly and decadly."), getInstanceName()));
        }
        outerAggregationModeID = new AggregationMode[n];

        for (int i = 0; i < n; i++) {
            outerAggregationModeID[i] = AggregationMode.fromAbbreviation(outerAggregationMode[i].getValue());
        }
        
        //create aggreagators        
        for (int i = 0; i < getNumberOfAttributes(); i++) {
            if (!isEnabled(i)) {
                continue;
            }

            Aggregator.AggregationMode outerMode = getOuterAggregationModeID(i);
            TemporalAggregator.AggregationTimePeriod outerTimeUnitID = getOuterTimeUnitID();

            TemporalAggregator<Iterable<Double>> innerAggregator = this.aggregators[i];
            innerAggregator.removeConsumers();

            TemporalAggregator<Iterable<Double>> outerAggregator;
            if (outerMode != AggregationMode.INDEPENDENT) {
                outerAggregator = new CompoundTemporalAggregator<Iterable<Double>>( 
                        DoubleIteratorAggregator.create(outerMode, N),
                        innerAggregator,
                        outerTimeUnitID,
                        customTimePeriods);
            } else {
                outerAggregator = new MultiTemporalAggregator(
                        innerAggregator,
                        outerTimeUnitID,
                        customTimePeriods);
            }
            outerAggregator.addConsumer(new DataConsumer(outputAttributeNames[i].getValue()));
            outerAggregator.init();
            aggregators[i] = outerAggregator;
        }        
    }
}
