package jams.worldwind.ui.renderer;

import gov.nasa.worldwind.render.Material;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Ronny Berndt <ronny.berndt at uni-jena.de>
 */
public class MaterialClassCellRenderer extends JLabel implements TableCellRenderer {

    Material currentMaterial;
    
    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;

    /**
     *
     * @param isBordered
     */
    public MaterialClassCellRenderer(boolean isBordered) {
        this.isBordered = isBordered;
        setOpaque(true); //MUST do this for background to show up.
    }

    @Override
    public Component getTableCellRendererComponent(
                            JTable table, Object color,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) {
        //System.out.println("ROW: " + row + " COL: " + column + "OBJECT: " + color);
        this.currentMaterial = (Material)color;
        Color newColor = this.currentMaterial.getDiffuse();
        setBackground(newColor);
        if (isBordered) {
            if (isSelected) {
                if (selectedBorder == null) {
                    selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                              table.getSelectionBackground());
                }
                setBorder(selectedBorder);
            } else {
                if (unselectedBorder == null) {
                    unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                              table.getBackground());
                }
                setBorder(unselectedBorder);
            }
        }
        
        setToolTipText("RGB value: " + newColor.getRed() + ", "
                                     + newColor.getGreen() + ", "
                                     + newColor.getBlue());
        return this;
    }
}

