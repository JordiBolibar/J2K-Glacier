/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lm.view.changeLMArable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import lm.Componet.*;
import javax.swing.*;
import lm.Componet.Vector.LMArableVector;
import lm.Componet.Vector.LMTillVector;
import lm.view.Constraints;

/**
 *
 * @author Jens Wipprich ==> jens.wipprich (at) uni-jena.de
 */
public class lmArablePanel extends JPanel {

    private Constraints c;
    private ArrayList<lmArableViewVector> ViewList=new ArrayList();
    private ArrayList<JButton> AddSteps=new ArrayList();
    private JPanel content=new JPanel();


    public lmArablePanel(){
        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        content.setLayout(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        this.add(content);

    }

    public void set(ArrayList<LMArableVector> a, ArrayList TID, ArrayList FID, ArrayList year,ArrayList date){
        getViewList().clear();
        for(int i=0;i<a.size();i++){
            getViewList().add(new lmArableViewVector(i+1,a.get(i),TID,FID,year,date));
        }
        getAddSteps().clear();
        for(int i=0;i<=a.size();i++){
            AddSteps.add(new JButton(new ImageIcon("src/lm/resources/images/Plus.gif")));
            AddSteps.get(i).setPreferredSize(new Dimension(20,20));
        }
        createGUI();

    }

    public void createGUI(){
        content.removeAll();
        c=new Constraints(0,0);
        content.add(new JLabel("ID  "),c);

        c=new Constraints(1,0);
        content.add(new JLabel("  Date  "),c);

        c=new Constraints(2,0);
        content.add(new JLabel(" Year "),c);

        c=new Constraints(3,0,2,1);
        content.add(new JLabel("  TID  "),c);

        c=new Constraints(5,0,2,1);
        content.add(new JLabel("  FID  "),c);
        
        c=new Constraints(7,0);
        content.add(new JLabel("  FAMount  "),c);

        c=new Constraints(8,0);
        content.add(new JLabel("  PLANT  "),c);

        c=new Constraints(9,0);
        content.add(new JLabel("  HARVEST  "),c);

        c=new Constraints(10,0);
        content.add(new JLabel("  FRACHARV  "),c);
        int p=0;
        for(int i=0;i<getViewList().size();i++){
            c=new Constraints(0,p+2);
            c.anchor=GridBagConstraints.FIRST_LINE_START;
            content.add(ViewList.get(i).getID(),c);

            c=new Constraints(1,p+2);
            ViewList.get(i).getDateField().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getDateField(),c);
            if(p!=0){
                c=new Constraints(2,p+2);
                ViewList.get(i).getYearBox().setActionCommand(String.valueOf(i));
                content.add(ViewList.get(i).getYearBox(),c);
            }else{
                c=new Constraints(2,2);
                content.add(new JLabel("1"),c);
            }
            

            c=new Constraints(3,p+2);
            ViewList.get(i).getTID().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getTID(),c);
            
            c=new Constraints(4,p+2);
            ViewList.get(i).getTIDEdit().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getTIDEdit(),c);


            c=new Constraints(5,p+2);
            ViewList.get(i).getFID().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getFID(),c);

            c=new Constraints(6,p+2);
            ViewList.get(i).getFIDEdit().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getFIDEdit(),c);

            c=new Constraints(7,p+2);
            ViewList.get(i).getFAMountField().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getFAMountField(),c);

            c=new Constraints(8,p+2);
            ViewList.get(i).getPLANTBOX().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getPLANTBOX(),c);

            c=new Constraints(9,p+2);
            ViewList.get(i).getHARVESTBOX().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getHARVESTBOX(),c);

            c=new Constraints(10,p+2);
            ViewList.get(i).getFRACHARVField().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getFRACHARVField(),c);

            c= new Constraints(11, p+2);
            ViewList.get(i).getDelete().setActionCommand(String.valueOf(i));
            content.add(ViewList.get(i).getDelete(),c);

            
            p=p+2;
            
        }
        p=0;
        for(int i=0;i<AddSteps.size();i++){
                   c=new Constraints(11,p+1);
                   AddSteps.get(i).setActionCommand(String.valueOf(i));
                   content.add(AddSteps.get(i),c);
            p=p+2;
        }



        content.revalidate();
        content.repaint();
    }
    /**
     * @return the ViewList
     */
    public ArrayList<lmArableViewVector> getViewList() {
        return ViewList;
    }

    /**
     * @return the AddSteps
     */
    public ArrayList<JButton> getAddSteps() {
        return AddSteps;
    }


}
