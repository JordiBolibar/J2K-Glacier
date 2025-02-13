/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jams.aggregators;

import jams.data.Attribute;
import jams.data.Attribute.Calendar;
import jams.data.Attribute.TimeInterval;
import jams.data.DefaultDataFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;

/**
 *
 * @author christian
 */
public abstract class TemporalAggregator<T>{
    private Calendar time;
    
    AggregationTimePeriod timePeriod;
    ArrayList<Consumer> consumers = new ArrayList<Consumer>();    
    Collection<TimeInterval> customTimePeriods;
    public enum AggregationTimePeriod{
        HOURLY, 
        DAILY, 
        YEARLY, 
        SEASONAL, 
        MONTHLY, 
        DECADLY, 
        HALFYEAR, 
        HYDHALFYEAR, 
        CUSTOM;
        
        public static AggregationTimePeriod fromString(String s){
            for (AggregationTimePeriod iter : AggregationTimePeriod.values()){
                if (s.compareToIgnoreCase(iter.name()) == 0)
                    return iter;
            }
            return null;
        }
    };
    
    public interface Consumer<T>{
        public void consume(Calendar c, T v);
    }
            
    protected TemporalAggregator(TemporalAggregator<T> copy){
        this.time = copy.time;
        this.timePeriod = copy.timePeriod;
        this.consumers = (ArrayList)copy.consumers.clone();
        if (copy.customTimePeriods != null){
            this.customTimePeriods = new ArrayList<TimeInterval>();
            customTimePeriods.addAll(copy.customTimePeriods);
        }else{
            customTimePeriods = null;
        }
    }
          
    public TemporalAggregator(AggregationTimePeriod timePeriod){
        this(timePeriod, null);
    }
    
    public TemporalAggregator(AggregationTimePeriod timePeriod, Collection<TimeInterval> customTimePeriods){
        this.timePeriod = timePeriod;
        
        this.customTimePeriods = customTimePeriods;
        if (timePeriod == AggregationTimePeriod.CUSTOM){
            for (TimeInterval ti : customTimePeriods) {
                ti.getStart().removeUnsignificantComponents(ti.getTimeUnit());
                ti.getEnd().removeUnsignificantComponents(ti.getTimeUnit());
            }
            //check for overlapping
            for (TimeInterval ti1 : customTimePeriods) {
                for (TimeInterval ti2 : customTimePeriods) {
                    if (ti1 == ti2)
                        continue;
                    if (( ti1.getStart().getTimeInMillis() < ti2.getEnd().getTimeInMillis() && 
                          ti1.getStart().getTimeInMillis() > ti2.getStart().getTimeInMillis() ) || 
                        ( ti1.getEnd().getTimeInMillis() < ti2.getEnd().getTimeInMillis() && 
                          ti1.getEnd().getTimeInMillis() > ti2.getStart().getTimeInMillis() )  ){
                        throw new IllegalArgumentException("Time-Intervals " + ti1 + " and " + ti2 + " do overlap");
                    }                    
                }
            }
        }
    }
    
    public void addConsumer(Consumer<T> consumer){
        this.consumers.add(consumer);
    }
    public void removeConsumer(Consumer<T> consumer){
        this.consumers.remove(consumer);
    }
    public void removeConsumers(){
        this.consumers.clear();
    }
            
    protected void consume(Calendar time, T v) {
        //is there anything to consume?
        if (time == null){
            return;
        }
        Calendar roundedTime = roundToTimePeriod(time, timePeriod);
        if (roundedTime == null)
            return;

        for (Consumer c : consumers) {
            c.consume(roundedTime, v);
        }
    }
    
    public abstract TemporalAggregator<T> copy();
    
    public abstract void init();
    
    public abstract void aggregate(Calendar timeStep, T next);
    
    public boolean isNextTimeStep(Calendar timeStep){
        Calendar newTime = roundToTimePeriod(timeStep, timePeriod);        
        //outside of time intervals, so skip it anyway
        if (newTime == null){ 
            return true;
        }
        if (time == null){
            time = newTime;
            return false;
        }
        //this is interesting .. comparing calendars in that way, 
        //avoids internal cloning
        return newTime.getTimeInMillis()>(time.getTimeInMillis());
    }
    
    public void setTimeStep(Calendar timeStep){
        time = roundToTimePeriod(timeStep, timePeriod);
        if (time == null)
            time = timeStep.clone();
    }   
    
    public void finish(){
        time = null;
    }
    
    public AggregationTimePeriod getTimePeriod(){
        return this.timePeriod;
    }
    
    protected Calendar currentTimeStep(){
        return time;// == null ? null : time.clone();
    }
            
    protected TimeInterval getTotalTimePeriod(){
        if (customTimePeriods == null || customTimePeriods.isEmpty()){
            return null;
        }
        TimeInterval totalTimePeriod = DefaultDataFactory.getDataFactory().createTimeInterval();
        totalTimePeriod.getStart().set(5000, 1, 1, 0, 0, 0);
        totalTimePeriod.getEnd().set(1, 1, 1, 0, 0, 0);
                
        for (TimeInterval ti : customTimePeriods){
            if (totalTimePeriod.getStart().after(ti.getStart())){
                totalTimePeriod.setStart(ti.getStart().clone());
            }
            if (totalTimePeriod.getEnd().before(ti.getEnd())){
                totalTimePeriod.setEnd(ti.getEnd().clone());
            }
        }
        return totalTimePeriod;
    }    
    //cloning of calendar is expansive
    protected Attribute.Calendar roundToTimePeriod(Calendar in, AggregationTimePeriod timeUnitID){        
        Attribute.Calendar out = in.clone();

        switch (timeUnitID){
            case HOURLY: out.removeUnsignificantComponents(Attribute.Calendar.HOUR_OF_DAY); break;
            case DAILY: out.removeUnsignificantComponents(Attribute.Calendar.DAY_OF_MONTH); break;
            case MONTHLY: out.removeUnsignificantComponents(Attribute.Calendar.MONTH); break;
            case YEARLY: out.removeUnsignificantComponents(Attribute.Calendar.YEAR); break;
            case DECADLY: out.removeUnsignificantComponents(Attribute.Calendar.YEAR); 
                    int yearInDekade = (out.get(Attribute.Calendar.YEAR)-1) % 10;
                    out.set(out.get(Attribute.Calendar.YEAR)-yearInDekade, 0, 1, 12, 0, 0);
                    break;
            case SEASONAL: {out.removeUnsignificantComponents(Attribute.Calendar.MONTH); 
                    int month = out.get(Attribute.Calendar.MONTH);
                    int year  = out.get(Attribute.Calendar.YEAR);
                    //winter dez - feb (11,00,01)
                    if (month < 2){
                        month = 11;
                        year = year - 1;
                    //spring mar - may (02,03,04)
                    }else if (month < 5){
                        month = 2;
                    //spring jun,jul,aug (05,06,07)
                    }else if (month < 8){
                        month = 5;
                    //spring sep,okt,nov (08,09,10)
                    }else if (month < 11){
                        month = 8;
                    }else{
                        month = 12;
                    }
                    out.set(year, month, 1, 12, 0, 0);}
                    break;
            case HALFYEAR: {out.removeUnsignificantComponents(Attribute.Calendar.MONTH); 
                    int month = out.get(Attribute.Calendar.MONTH);
                    if (month < 6){
                        month = 0;
                    }else
                        month = 6;
                    out.set(out.get(Attribute.Calendar.YEAR), month, 1, 12, 0, 0);}
                break;
            case HYDHALFYEAR:
                {out.removeUnsignificantComponents(Attribute.Calendar.MONTH); 
                    int month = out.get(Attribute.Calendar.MONTH);
                    if (month >= 4 && month <= 9){
                        month = 4;
                        out.set(out.get(Attribute.Calendar.YEAR), month, 1, 12, 0, 0);
                    }else if (month < 4){
                        month = 10;
                        out.set(out.get(Attribute.Calendar.YEAR)-1, month, 1, 12, 0, 0);
                    }else{
                        month = 10;
                        out.set(out.get(Attribute.Calendar.YEAR), month, 1, 12, 0, 0);
                    }}
                break;
            case CUSTOM:{
                boolean isConsidered = false;                
                for (Attribute.TimeInterval ti : customTimePeriods){
                    //clone would be better but expensive
                    out.removeUnsignificantComponents(ti.getTimeUnit());
                    if (!(ti.getStart().getTimeInMillis()>out.getTimeInMillis()) && 
                        !(ti.getEnd().getTimeInMillis()  <out.getTimeInMillis()) ){
                        out.setValue(ti.getStart().toString());
                        isConsidered = true;
                        break;
                    }
                }
                if (!isConsidered){
                    return null;
                }
                break;
            }
        }
        return out;
    }
}
