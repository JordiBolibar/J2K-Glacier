/*
 * JAMSTree.java
 * Created on 20. April 2006, 11:59
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
package jams.explorer.tree;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

public class JAMSTree extends JTree {

    private Insets autoscrollInsets = new Insets(20, 20, 20, 20); // insets
    private Enumeration<TreePath> expandedPaths;

    public JAMSTree() {
        setAutoscrolls(true);
        setRootVisible(true);
        setShowsRootHandles(false);//to show the root icon
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); //set single selection for the Tree
        this.setSelectionRow(0);
        setCellRenderer(new JAMSTreeRenderer());
        setDragEnabled(true);
    }

    public void autoscroll(Point cursorLocation) {
        Insets insets = getAutoscrollInsets();
        Rectangle outer = getVisibleRect();
        Rectangle inner = new Rectangle(outer.x + insets.left, outer.y + insets.top, outer.width - (insets.left + insets.right), outer.height - (insets.top + insets.bottom));
        if (!inner.contains(cursorLocation)) {
            Rectangle scrollRect = new Rectangle(cursorLocation.x - insets.left, cursorLocation.y - insets.top, insets.left + insets.right, insets.top + insets.bottom);
            scrollRectToVisible(scrollRect);
        }
    }

    public boolean isPathEditable(TreePath path) {
        return false;
    }

    public Insets getAutoscrollInsets() {
        return (autoscrollInsets);
    }

    class JAMSTreeRenderer extends DefaultTreeCellRenderer {

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            TreeNode node = (TreeNode) value;

            if (node instanceof DSTreeNode) {
                DSTreeNode jNode = (DSTreeNode) node;
                setIcon(DSTreeNode.NODE_ICON[jNode.getType()]);
            }
            return this;
        }
    }

    public void expandAll() {
        int row = 0;
        while (row < this.getRowCount()) {
            this.expandRow(row);
            row++;
        }
    }

    public void collapseAll() {
        int row = this.getRowCount() - 1;
        while (row > 0) {
            this.collapseRow(row);
            row--;
        }
    }
    
    public void saveExpandedState(TreePath sourcePath) {
        expandedPaths = getExpandedDescendants(sourcePath);
    }

    public void restoreExpandedState() {
        if (expandedPaths != null) {
            while (expandedPaths.hasMoreElements()) {
                TreePath tmpPath = (TreePath) (expandedPaths.nextElement());
                expandPath(new TreePath(((DefaultMutableTreeNode) tmpPath.getLastPathComponent()).getPath()));
            }
        }
    }    
}
