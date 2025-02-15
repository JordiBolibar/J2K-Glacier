/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jams.components.calc;

import de.odysseus.el.util.SimpleContext;
import de.odysseus.el.util.SimpleResolver;
import jams.data.Attribute;
import jams.data.Attribute.Calendar;
import jams.model.JAMSComponent;
import jams.model.JAMSVarDescription;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

/**
 *
 * @author christian
 */
public class DoubleCalculator extends JAMSComponent {

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "vector data1")
    public Attribute.DoubleArray X1;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "vector data2")
    public Attribute.DoubleArray X2;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "scalar data1")
    public Attribute.Double[] s1;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "scalar data2")
    public Attribute.Double[] s2;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "scalar weight")
    public Attribute.Double w;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READWRITE,
            description = "scalar output")
    public Attribute.Double[] y;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "time object")
    public Attribute.Calendar time;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "time period")
    public Attribute.TimeInterval I;

    @JAMSVarDescription(access = JAMSVarDescription.AccessType.READ,
            description = "expression")
    public Attribute.String expr;

    transient ExpressionFactory factory = null;
    transient SimpleContext context = new SimpleContext(new SimpleResolver());
    transient ValueExpression valueExpr = null;

    transient ValueExpression X1Expr = null;
    transient ValueExpression X2Expr = null;
    transient ValueExpression s1Expr = null;
    transient ValueExpression s2Expr = null;
    transient ValueExpression wExpr = null;
    transient ValueExpression timeExpr = null;
    transient ValueExpression intervalExpr = null;
    transient ValueExpression yExpr = null;

    @Override
    public void init() {
        java.util.Properties properties = new java.util.Properties();
        properties.put("javax.el.cacheSize", "1000");
        properties.put("javax.el.methodInvocations", "false");
        properties.put("javax.el.nullProperties", "false");
        properties.put("javax.el.varArgs", "false");
        properties.put("javax.el.ignoreReturnType", "false");

        factory = new de.odysseus.el.ExpressionFactoryImpl(properties);

        context = DoubleFunctions.getContext();

        valueExpr = factory.createValueExpression(context, "${" + this.expr.getValue() + "}", double.class);
        wExpr = factory.createValueExpression(context, "${w}", double.class);

        X1Expr = factory.createValueExpression(context, "${X1}", double[].class);
        X2Expr = factory.createValueExpression(context, "${X2}", double[].class);

        s1Expr = factory.createValueExpression(context, "${s1}", double.class);
        s2Expr = factory.createValueExpression(context, "${s2}", double.class);

        yExpr = factory.createValueExpression(context, "${y}", double.class);

        timeExpr = factory.createValueExpression(context, "${time}", Calendar.class);
        intervalExpr = factory.createValueExpression(context, "${I}", Attribute.TimeInterval.class);
    }

    @Override
    public void run() {
        int n = y.length;

        if (this.w != null) {
            wExpr.setValue(context, w.getValue());
        }

        if (this.X1 != null) {
            X1Expr.setValue(context, X1.getValue());
        }

        if (this.X2 != null) {
            X2Expr.setValue(context, X2.getValue());
        }

        if (this.time != null && this.time.getValue() != null) {
            timeExpr.setValue(context, time.getValue());
        }

        if (this.I != null && this.I.getValue() != null) {
            intervalExpr.setValue(context, I);
        }

        for (int i = 0; i < n; i++) {
            if (s1 != null) {
                s1Expr.setValue(context, s1[i].getValue());
            }
            if (s2 != null) {
                s2Expr.setValue(context, s2[i].getValue());
            }

            yExpr.setValue(context, y[i].getValue());

            y[i].setValue((Double) valueExpr.getValue(context));
        }
    }

    private void readObject(ObjectInputStream objIn) throws IOException, ClassNotFoundException {
        objIn.defaultReadObject();

        init();
    }
}
