package jams.components.machineLearning;

import jams.model.*;
import jams.data.*;
import java.util.Random;

/**
 *
 * @author Christian(web)
 */
public class DataDisturber extends JAMSContext {
        
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "TimeSerie of Temp Data"
            )
            public Attribute.Integer startComponent;
    
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "TimeSerie of Temp Data"
            )
            public Attribute.Integer endComponent;
           
    @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.READ,
            description = "TimeSerie of Temp Data"
            )
            public Attribute.Entity Data;
            
     @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "TimeSerie of Temp Data"
            )
            public Attribute.Double NoiseAmpitude;
     
     @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "TimeSerie of Temp Data"
            )
            public Attribute.Double ErrorAmpitude;
     
     @JAMSVarDescription(
            access = JAMSVarDescription.AccessType.WRITE,
            description = "TimeSerie of Temp Data"
            )
            public Attribute.Boolean AllowNegative;
     
    public DataDisturber() {
	 
    }
     
    public void init() {  
     
    }
     
          
    public void run() {  
	double data[][] = null;
				
	try {
	     data = (double[][])Data.getObject("data");	     
	}
	catch(Exception e) {
	    System.out.println("Konnte InputData nicht finden" + e.toString());
            e.printStackTrace();
	}
			
	System.out.println("Optimiere Trainingsdaten!");
	
	int N = data.length;
	double A1 = this.NoiseAmpitude.getValue();
	double A2 = this.ErrorAmpitude.getValue();
	
	Random r = new Random();
	
	for (int j=0;j<N;j++) {
	    for (int k=this.startComponent.getValue()-1;k<=this.endComponent.getValue()-1;k++) {
		double z1 = A1*(2.0*r.nextDouble() - 1.0);
		double z2 = A2*(2.0*r.nextDouble() - 1.0);
		
		data[j][k] = data[j][k]*(1.0 + z1) + z2;
		
		if (!this.AllowNegative.getValue()) {
		    if (data[j][k] < 0.0) {
			data[j][k] = -data[j][k];
		    }
		}
	    }
	}	
    }
}