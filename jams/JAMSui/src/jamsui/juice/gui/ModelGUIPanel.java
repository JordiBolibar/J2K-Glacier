/*
 * ModelGUIPanel.java
 * Created on 5. Januar 2007, 10:44
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
package jamsui.juice.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import jams.gui.tools.GUIHelper;
import jams.gui.input.InputComponent;
import jamsui.juice.*;
import jams.gui.input.InputComponentFactory;
import jams.JAMS;
import jams.meta.ModelDescriptor;
import jams.meta.ModelProperties;
import jams.meta.ModelProperties.Group;
import jams.meta.ModelProperties.ModelElement;
import jams.meta.ModelProperties.ModelProperty;

/**
 *
 * @author S. Kralisch
 */
public class ModelGUIPanel extends JPanel {

    private static ImageIcon UP_ICON = new ImageIcon(new ImageIcon(ClassLoader.getSystemResource("resources/images/arrowup.png")).getImage().getScaledInstance(9, 5, Image.SCALE_SMOOTH));
    private static ImageIcon DOWN_ICON = new ImageIcon(new ImageIcon(ClassLoader.getSystemResource("resources/images/arrowdown.png")).getImage().getScaledInstance(9, 5, Image.SCALE_SMOOTH));
    private static final Dimension BUTTON_DIMENSION = new Dimension(150, 20), PANEL_DIMENSION = new Dimension(300, 0);
    private JTabbedPane tabbedPane = new JTabbedPane();
    private HashMap<ModelProperty, InputComponent> inputMap = new HashMap<ModelProperty, InputComponent>();
    private ModelPropertyDlg propertyDlg = new ModelPropertyDlg(JUICE.getJuiceFrame());
    private ModelSubgroupDlg subgroupDlg = new ModelSubgroupDlg(JUICE.getJuiceFrame());
//    private GroupEditDlg groupEditDlg = new GroupEditDlg(JUICE.getJuiceFrame());
    private HashMap<ModelProperties.Group, JPanel> groupPanels;
    private HashMap<ModelProperties.Group, JScrollPane> groupPanes;
    private JPanel mainButtonPanel = new JPanel();
    private ModelView view;
    private Font titledBorderFont;

    /**
     * JPanel providing visual builder for defining a JAMS models GUI
     * 
     * @param view the ModelView object this ModelGUIPanel is associated to
     */
    public ModelGUIPanel(ModelView view) {

        this.view = view;
        this.tabbedPane.setTabPlacement(JTabbedPane.LEFT);

        //setBorder(BorderFactory.createTitledBorder("Model Parameters"));
        setLayout(new BorderLayout());

        this.add(tabbedPane, BorderLayout.CENTER);
        this.add(mainButtonPanel, BorderLayout.NORTH);

        // create some nice font for the border title
        titledBorderFont = (Font) UIManager.getDefaults().get("TitledBorder.font");
        int fontSize = titledBorderFont.getSize();
        if (titledBorderFont.getStyle() == Font.BOLD) {
            fontSize += 2;
        }
        titledBorderFont = new Font(titledBorderFont.getName(), Font.BOLD, fontSize);

        JButton addPropertyButton = new JButton(JAMS.i18n("Add_Property"));
        addPropertyButton.setPreferredSize(BUTTON_DIMENSION);
        addPropertyButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addProperty();
            }
        });

        JButton addSubgroupButton = new JButton(JAMS.i18n("Add_Subgroup"));
        addSubgroupButton.setPreferredSize(BUTTON_DIMENSION);
        addSubgroupButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addSubgroup();
            }
        });

        JButton addGroupButton = new JButton(JAMS.i18n("Add_Group"));
        addGroupButton.setPreferredSize(BUTTON_DIMENSION);
        addGroupButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addGroup();
            }
        });

        JButton moveupGroupButton = new JButton(JAMS.i18n("Group_up"));
        moveupGroupButton.setPreferredSize(BUTTON_DIMENSION);
        moveupGroupButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveupGroup();
            }
        });

        JButton movedownGroupButton = new JButton(JAMS.i18n("Group_down"));
        movedownGroupButton.setPreferredSize(BUTTON_DIMENSION);
        movedownGroupButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                movedownGroup();
            }
        });

        JButton editGroupButton = new JButton(JAMS.i18n("Edit_Group"));
        editGroupButton.setPreferredSize(BUTTON_DIMENSION);
        editGroupButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                editGroup();
            }
        });

        JButton delGroupButton = new JButton(JAMS.i18n("Remove_Group"));
        delGroupButton.setPreferredSize(BUTTON_DIMENSION);
        delGroupButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteGroup();
            }
        });

        GridBagLayout gblButton = new GridBagLayout();
        JPanel innerButtonPanel = new JPanel();
        innerButtonPanel.setLayout(gblButton);
        //innerButtonPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        GUIHelper.addGBComponent(innerButtonPanel, gblButton, addPropertyButton, 0, 0, 1, 1, 0, 0);
        GUIHelper.addGBComponent(innerButtonPanel, gblButton, addSubgroupButton, 0, 1, 1, 1, 0, 0);
        GUIHelper.addGBComponent(innerButtonPanel, gblButton, addGroupButton, 1, 0, 1, 1, 0, 0);
        GUIHelper.addGBComponent(innerButtonPanel, gblButton, editGroupButton, 1, 1, 1, 1, 0, 0);
        GUIHelper.addGBComponent(innerButtonPanel, gblButton, moveupGroupButton, 2, 0, 1, 1, 0, 0);
        GUIHelper.addGBComponent(innerButtonPanel, gblButton, movedownGroupButton, 2, 1, 1, 1, 0, 0);
        GUIHelper.addGBComponent(innerButtonPanel, gblButton, delGroupButton, 3, 0, 1, 1, 0, 0);

        mainButtonPanel.add(innerButtonPanel);
        this.setPreferredSize(PANEL_DIMENSION);

    }

    private void moveupGroup() {
        int index = tabbedPane.getSelectedIndex();
        if (index > 0) {

            ModelDescriptor md = view.getModelDescriptor();

            ModelProperties.Group group = md.getModelProperties().getGroup(index);
            md.getModelProperties().removeGroup(group);
            Component comp = tabbedPane.getComponentAt(index);
            tabbedPane.removeTabAt(index);

            index--;

            md.getModelProperties().insertGroup(group, index);
            tabbedPane.add(comp, index);
            tabbedPane.setTitleAt(index, group.getName());
            tabbedPane.setSelectedIndex(index);
        }
    }

    private void movedownGroup() {
        int index = tabbedPane.getSelectedIndex();
        if (index < tabbedPane.getTabCount() - 1) {

            ModelDescriptor md = view.getModelDescriptor();

            ModelProperties.Group group = md.getModelProperties().getGroup(index);
            md.getModelProperties().removeGroup(group);
            Component comp = tabbedPane.getComponentAt(index);
            tabbedPane.removeTabAt(index);

            index++;

            md.getModelProperties().insertGroup(group, index);
            tabbedPane.add(comp, index);
            tabbedPane.setTitleAt(index, group.getName());
            tabbedPane.setSelectedIndex(index);
        }
    }

    private void editGroup() {

        // get selected group

        int index = tabbedPane.getSelectedIndex();
        if (index < 0) {
            return;
        }

        ModelProperties mp = view.getModelDescriptor().getModelProperties();

        ModelProperties.Group group = mp.getGroup(index);

        // query new name

        String groupName = group.getName();
        String newGroupName = JOptionPane.showInputDialog(this, JAMS.i18n("Group_name:"), groupName);
        if ((newGroupName == null) || newGroupName.equals("") || newGroupName.equals(groupName)) {
            return;
        }

        //  try to set group's name

        if (!mp.setGroupName(group, newGroupName)) {
            GUIHelper.showErrorDlg(JUICE.getJuiceFrame(), JAMS.i18n("Group_name_already_in_use!"), JAMS.i18n("Error"));
            return;
        }

        // update tabbedpane title

        tabbedPane.setTitleAt(index, newGroupName);

    }

    private void addGroup() {

        // get name

        String groupName = JOptionPane.showInputDialog(this, JAMS.i18n("Group_name:"));

        if ((groupName == null) || groupName.equals("")) {
            return;
        }

        // add group

        ModelProperties mp = view.getModelDescriptor().getModelProperties();

        if (!mp.addGroup(groupName)) {
            GUIHelper.showErrorDlg(JUICE.getJuiceFrame(), JAMS.i18n("Group_name_already_in_use!"), JAMS.i18n("Error"));
            return;
        }

        ModelProperties.Group group = mp.getGroup(groupName);

        // create panels and scrollpanes

        JPanel contentPanel = new JPanel();
        JPanel scrollPanel = new JPanel();
        scrollPanel.add(contentPanel);

        JScrollPane scrollPane = new JScrollPane(scrollPanel);

        tabbedPane.addTab(groupName, scrollPane);
        tabbedPane.setSelectedComponent(scrollPane);

        // add group and panel to groupPanels map

        groupPanels.put(group, contentPanel);
        groupPanes.put(group, scrollPane);
    }

    // delete group of gui components 
    private void deleteGroup() {

        int index = tabbedPane.getSelectedIndex();
        if (index < 0) {
            return;
        }

        if (GUIHelper.showYesNoDlg(JUICE.getJuiceFrame(), JAMS.i18n("Really_delete_this_Group_and_all_of_its_properties?"), JAMS.i18n("Delete_Group")) != JOptionPane.YES_OPTION) {
            return;
        }

        // get group
        ModelProperties mp = view.getModelDescriptor().getModelProperties();
        ModelProperties.Group group = mp.getGroup(index);

        // remove group
        mp.removeGroup(group);

        // remove tabbedPane and entry from groupPanels
        tabbedPane.removeTabAt(index);
        groupPanels.remove(group);
    }

    // update group of gui components
    public void updateGroup(Group group) {

        if (group.isSubGroup()) {
            updateGroup(group.getGroup());
            return;
        }
//        updateProperties();

        GridBagLayout gbl = new GridBagLayout();

        JPanel contentPanel = groupPanels.get(group);

        contentPanel.removeAll();
        contentPanel.setLayout(gbl);

        int y = 1;

        ArrayList properties = group.getProperties();
        for (int j = 0; j < properties.size(); j++) {
            Object modelElement = properties.get(j);

            // <@todo> groups consists of subgroups and properties,
            //          subgroups consists of properties
            //          this could be recursive too
            if (modelElement instanceof ModelProperty) {
                ModelProperty property = (ModelProperty) modelElement;

                JPanel buttonPanel = createPropertyButtonPanel(contentPanel, gbl, property, y);
                GUIHelper.addGBComponent(contentPanel, gbl, buttonPanel, 3, y, 1, 1, 1, 1);
            }

            if (modelElement instanceof Group) {
                Group subgroup = (Group) modelElement;
                ArrayList subgroupProperties = subgroup.getProperties();

                int height = subgroupProperties.size() + 3;

                // create the subgroup panel
                JPanel subgroupPanel = new JPanel(gbl);

                // create and set the border
                subgroupPanel.setBorder(BorderFactory.createTitledBorder(null, subgroup.getCanonicalName(),
                        TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, titledBorderFont));

                // add the subgroup panel
                GUIHelper.addGBComponent(contentPanel, gbl, subgroupPanel,
                        0, y, 3, height,
                        6, 2, 6, 2,
                        1, 1);

                JPanel sgButtonPanel = createSubgroupButtonPanel(subgroup);
                GUIHelper.addGBComponent(contentPanel, gbl, sgButtonPanel, 3, y, 3, 1, 1, 1);
                int row = y + 1;
                for (int k = 0; k < subgroupProperties.size(); k++) {
                    Object subgroupElement = subgroupProperties.get(k);

                    if (subgroupElement instanceof ModelProperty) {
                        row++;

                        ModelProperty subgroupProperty = (ModelProperty) subgroupElement;
                        JPanel buttonPanel = createPropertyButtonPanel(subgroupPanel, gbl, subgroupProperty, row);
                        GUIHelper.addGBComponent(subgroupPanel, gbl, buttonPanel, 3, row, 3, 1, 1, 1);
                    }
                }
                row = row + 2;
                y = row;

            }
            y++;
        }

        contentPanel.updateUI();
    }

    // update panel, i.e. load list of editors and create controls
    public void updatePanel() {

        ArrayList<ModelProperties.Group> groups = view.getModelDescriptor().getModelProperties().getGroupList();

        groupPanels = new HashMap<ModelProperties.Group, JPanel>();
        groupPanes = new HashMap<ModelProperties.Group, JScrollPane>();

        tabbedPane.removeAll();

        JPanel contentPanel, scrollPanel;
        JScrollPane scrollPane;

        for (ModelProperties.Group group : groups) {

            contentPanel = new JPanel();

            scrollPanel = new JPanel();
            scrollPanel.add(contentPanel);
            scrollPane = new JScrollPane(scrollPanel);

            groupPanels.put(group, contentPanel);
            groupPanes.put(group, scrollPane);

            updateGroup(group);

            tabbedPane.addTab(group.getName(), scrollPane);
        }
    }

    /**
     * createPropertyButtonPanel
     * @param contentPanel - the content panel
     * @param gbl - the layout
     * @param property - the property
     * @param row - row number of actual row
     * @return buttonPanel
     */
    private JPanel createPropertyButtonPanel(JPanel contentPanel, GridBagLayout gbl, ModelProperty property, int row) {

        JPanel buttonPanel = new JPanel();
        InputComponent ic;

        // create a label with the property's name and some space in front of it
        JLabel nameLabel = new JLabel(property.name);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        GUIHelper.addGBComponent(contentPanel, gbl, nameLabel, 0, row, 1, 1, 0, 0);

        if (property.var != null) {
            ic = InputComponentFactory.createInputComponent(property.var.getType(), true);
        } else if (property.attribute != null) {
            ic = InputComponentFactory.createInputComponent(property.attribute.getType(), true);
        } else {
            ic = InputComponentFactory.createInputComponent(JUICE.JAMS_DATA_TYPES[0], true);
        }

        ic.setRange(property.lowerBound, property.upperBound);
        ic.setLength(property.length);
        ic.setHelpText(property.description);
        ic.getComponent().setEnabled(false);
//        ic.setValue(property.value);

//        inputMap.put(property, ic);
        GUIHelper.addGBComponent(contentPanel, gbl, (Component) ic, 1, row, 2, 1, 1, 1);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder());

        ElementButton downButton = new ElementButton(property);
        downButton.setToolTipText(JAMS.i18n("Move_down"));
        downButton.setIcon(DOWN_ICON);
        downButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ElementButton button = (ElementButton) e.getSource();
                moveDownElement(button.element);
            }
        });
        buttonPanel.add(downButton);

        ElementButton upButton = new ElementButton(property);
        upButton.setToolTipText(JAMS.i18n("Move_up"));
        upButton.setIcon(UP_ICON);
        upButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ElementButton button = (ElementButton) e.getSource();
                moveUpElement(button.element);
            }
        });
        buttonPanel.add(upButton);

        ElementButton delButton = new ElementButton(property);
        delButton.setToolTipText(JAMS.i18n("Delete"));
        delButton.setText("-");
        delButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ElementButton button = (ElementButton) e.getSource();
                deleteElement(button.element);
            }
        });
        buttonPanel.add(delButton);

        ElementButton editButton = new ElementButton(property);
        editButton.setToolTipText(JAMS.i18n("Edit"));
        editButton.setText("...");
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ElementButton button = (ElementButton) e.getSource();
                editProperty((ModelProperty) button.element);
            }
        });
        buttonPanel.add(editButton);

        return buttonPanel;
    }

    private JPanel createSubgroupButtonPanel(Group subgroup) {

        JPanel buttonPanel = new JPanel();

        buttonPanel.setBorder(BorderFactory.createEmptyBorder());

        ElementButton downButton = new ElementButton(subgroup);
        downButton.setToolTipText(JAMS.i18n("Move_down"));
        downButton.setIcon(DOWN_ICON);
        downButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ElementButton button = (ElementButton) e.getSource();
                moveDownElement(button.element);
            }
        });
        buttonPanel.add(downButton);

        ElementButton upButton = new ElementButton(subgroup);
        upButton.setToolTipText(JAMS.i18n("Move_up"));
        upButton.setIcon(UP_ICON);
        upButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ElementButton button = (ElementButton) e.getSource();
                moveUpElement(button.element);
            }
        });
        buttonPanel.add(upButton);

        ElementButton delButton = new ElementButton(subgroup);
        delButton.setToolTipText(JAMS.i18n("Delete"));
        delButton.setText("-");
        delButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ElementButton button = (ElementButton) e.getSource();
                deleteElement(button.element);
            }
        });
        buttonPanel.add(delButton);

        ElementButton editButton = new ElementButton(subgroup);
        editButton.setToolTipText(JAMS.i18n("Edit"));
        editButton.setText("...");
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ElementButton button = (ElementButton) e.getSource();
                editSubgroup((Group) button.element);
            }
        });
        buttonPanel.add(editButton);

        return buttonPanel;
    }

    private void moveDownElement(ModelElement element) {

        ArrayList<Object> list = element.getGroup().getProperties();
        int index = list.indexOf(element);
        if (index < list.size() - 1) {
            list.remove(index);
            list.add(index + 1, element);
        }

        Group mainGroup = element.getMainGroup();
        updateGroup(mainGroup);
        JPanel contentPanel = groupPanels.get(mainGroup);
        contentPanel.updateUI();
    }

    private void moveUpElement(ModelElement element) {

        ArrayList<Object> list = element.getGroup().getProperties();
        int index = list.indexOf(element);
        if (index > 0) {
            list.remove(index);
            list.add(index - 1, element);
        }

        Group mainGroup = element.getMainGroup();
        updateGroup(mainGroup);
        JPanel contentPanel = groupPanels.get(mainGroup);
        contentPanel.updateUI();
    }

    private void deleteElement(ModelElement element) {

        ArrayList list = element.getGroup().getProperties();

        // subgroup has to be empty
        if (element instanceof Group) {
            ArrayList test = ((Group) element).getProperties();
            if (test.size() > 0) {
                GUIHelper.showErrorDlg(JUICE.getJuiceFrame(), JAMS.i18n("Subgroup_needs_to_be_empty."), JAMS.i18n("Deletion_not_possible"));
                return;
            }
        }

        int result = GUIHelper.showYesNoDlg(JUICE.getJuiceFrame(), JAMS.i18n("Really_delete_this_property?"), JAMS.i18n("Delete_property"));
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        list.remove(element);

        Group mainGroup = element.getMainGroup();
        JPanel contentPanel = groupPanels.get(mainGroup);
        updateGroup(mainGroup);
        contentPanel.updateUI();
    }

    private void addProperty() {

        int index = tabbedPane.getSelectedIndex();
        if (index < 0) {
            return;
        }
        String groupName = tabbedPane.getTitleAt(index);

        ModelProperties mp = view.getModelDescriptor().getModelProperties();

        propertyDlg.update(mp.getAllGroupNames(), view.getModelDescriptor().getComponentDescriptors(), null, groupName);
        propertyDlg.setVisible(true);

        if (propertyDlg.getResult() == ModelPropertyDlg.OK_RESULT) {

            ModelProperties.ModelProperty property = mp.createProperty();

            String newGroupName = propertyDlg.getGroup();

            property.name = propertyDlg.getName();
            property.description = propertyDlg.getDescription();
            property.lowerBound = propertyDlg.getLowerBound();
            property.upperBound = propertyDlg.getUpperBound();
            property.length = propertyDlg.getLength();
            property.getHelpComponent().setHelpURL(propertyDlg.getHelpURL());
            property.getHelpComponent().setHelpText(propertyDlg.getHelpText());

            property.component = propertyDlg.getComponent();
            property.attribute = propertyDlg.getAttribute();
            property.var = propertyDlg.getVar();

            ModelProperties.Group group = mp.getGroup(newGroupName);
            mp.addProperty(group, property);
            if (group.isSubGroup()) {
                group = group.getGroup();
            }
            updateGroup(group);
            groupPanels.get(group).updateUI();
        }
    }

    private void addSubgroup() {
        int index = tabbedPane.getSelectedIndex();
        if (index < 0) {
            return;
        }
        String groupName = tabbedPane.getTitleAt(index);

        ModelProperties mp = view.getModelDescriptor().getModelProperties();

        subgroupDlg.update(mp.getGroupNames(), null, groupName);
        subgroupDlg.setVisible(true);

        if (subgroupDlg.getResult() == ModelSubgroupDlg.OK_RESULT) {

            groupName = subgroupDlg.getGroup();
            ModelProperties.Group group = mp.getGroup(groupName);

            String subgroubName = subgroupDlg.getName();

            ModelProperties.Group subgroup = mp.createSubgroup(group, subgroubName);
            subgroup.getHelpComponent().setHelpURL(subgroupDlg.getHelpURL());
            subgroup.getHelpComponent().setHelpText(subgroupDlg.getHelpText());

            updateGroup(group);
            groupPanels.get(group).updateUI();
        }
    }

    private void editProperty(ModelProperty property) {

        ModelProperties.Group group = property.getGroup();

        ModelProperties mp = view.getModelDescriptor().getModelProperties();

        propertyDlg.update(mp.getAllGroupNames(), view.getModelDescriptor().getComponentDescriptors(), property, group.getName());
        propertyDlg.setVisible(true);

        if (propertyDlg.getResult() == ModelPropertyDlg.OK_RESULT) {
            String newGroupName = propertyDlg.getGroup();

            property.name = propertyDlg.getName();
            property.description = propertyDlg.getDescription();
            property.lowerBound = propertyDlg.getLowerBound();
            property.upperBound = propertyDlg.getUpperBound();
            property.length = propertyDlg.getLength();
            property.getHelpComponent().setHelpURL(propertyDlg.getHelpURL());
            property.getHelpComponent().setHelpText(propertyDlg.getHelpText());
            property.component = propertyDlg.getComponent();
            property.attribute = propertyDlg.getAttribute();
            property.var = propertyDlg.getVar();

            ModelProperties.Group newGroup = mp.getGroup(newGroupName);

            if (!newGroup.equals(group)) {
                mp.removePropertyFromGroup(group, property);
                mp.addPropertyToGroup(newGroup, property);

                updateGroup(newGroup);
            }

            updateGroup(group);
        }
    }

    private void editSubgroup(Group subgroup) {

        ModelProperties.Group group = subgroup.getGroup();

        ModelProperties mp = view.getModelDescriptor().getModelProperties();

        subgroupDlg.update(mp.getGroupNames(), subgroup, group.getName());
        subgroupDlg.setVisible(true);

        if (subgroupDlg.getResult() == ModelSubgroupDlg.OK_RESULT) {
            String newGroupName = subgroupDlg.getGroup();

            subgroup.name = subgroupDlg.getName();
            subgroup.getHelpComponent().setHelpURL(subgroupDlg.getHelpURL());
            subgroup.getHelpComponent().setHelpText(subgroupDlg.getHelpText());

            ModelProperties.Group newGroup = mp.getGroup(newGroupName);

            if (!newGroup.equals(group)) {
                mp.removePropertyFromGroup(group, subgroup);
                mp.addPropertyToGroup(newGroup, subgroup);

                updateGroup(newGroup);
            }

            updateGroup(group);
        }
    }

//    public void updateProperties() {
//
//        // set values of properties to provided
//        for (ModelProperty property : inputMap.keySet()) {
//            InputComponent ic = inputMap.get(property);
//            property.value = ic.getValue();
//        }
//    }
    class ElementButton extends JButton {

        ModelElement element;

        public ElementButton(ModelElement element) {
            super();
            this.setBorder(null);
            this.setPreferredSize(new Dimension(20, 14));
            this.element = element;
        }
    }
}
