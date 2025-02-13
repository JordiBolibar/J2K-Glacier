package jamsui.juice.gui.tree;

import jams.JAMSException;
import jams.JAMSLogging;
import jams.meta.ComponentDescriptor;
import jams.meta.ComponentField;
import jams.meta.ContextDescriptor;
import java.awt.*;
import java.util.Collections;
import java.util.logging.Level;
import javax.swing.tree.*;
import java.awt.dnd.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import jams.model.JAMSContext;
import jamsui.juice.gui.ContextReplaceDlg;
import jamsui.juice.JUICE;
import java.util.logging.Logger;

public class DefaultTreeTransferHandler extends AbstractTreeTransferHandler {

    private ContextReplaceDlg dlg = new ContextReplaceDlg(JUICE.getJuiceFrame());

    public DefaultTreeTransferHandler(JAMSTree tree, int action) {
        super(tree, action, true);
        JAMSLogging.registerLogger(JAMSLogging.LogOption.CollectAndShow, 
                Logger.getLogger(this.getClass().getName()));
    }

    public boolean canPerformAction(JAMSTree target, JAMSNode draggedNode, int action, Point location) {

        JAMSNode targetRoot = (JAMSNode) target.getModel().getRoot();

        //nothing can be moved to a tree with lib root
        if (targetRoot.getType() == JAMSNode.LIBRARY_TYPE) {
            return false;
        }

        //package and library nodes can't be moved
        if (draggedNode.getType() == JAMSNode.PACKAGE_TYPE || draggedNode.getType() == JAMSNode.LIBRARY_TYPE || draggedNode.getType() == JAMSNode.ARCHIVE_TYPE) {
            return false;
        }

        TreePath pathTarget = target.getPathForLocation(location.x, location.y);
        if (pathTarget == null) {
            target.setSelectionPath(null);
            return (false);
        }

        /*        if (((JAMSNode)pathTarget.getLastPathComponent()).isLeaf()) { // or ((JAMSNode)pathTarget.getLastPathComponent()).getChildCount()==0
        target.setSelectionPath(null);
        return(false);
        }
         */

        if (((JAMSNode) pathTarget.getLastPathComponent()).getType() == JAMSNode.COMPONENT_TYPE) { // or ((JAMSNode)pathTarget.getLastPathComponent()).getChildCount()==0
//            target.setSelectionPath(null);
//            return(false);
        }
        if (action == DnDConstants.ACTION_COPY) {
            target.setSelectionPath(pathTarget);
            return (true);
        } else if (action == DnDConstants.ACTION_MOVE) {
            JAMSNode parentNode = (JAMSNode) pathTarget.getLastPathComponent();
            if (draggedNode.isRoot() || (parentNode == draggedNode.getParent()) || (draggedNode.isNodeDescendant(parentNode))) {
                target.setSelectionPath(null);
                return (false);
            } else {
                target.setSelectionPath(pathTarget);
                return (true);
            }
        } else {
            target.setSelectionPath(null);
            return (false);
        }
    }

    public boolean executeDrop(JAMSTree target, JAMSNode draggedNode, JAMSNode newParentNode, Vector expandedStates, int action) {

        int position = 0;

        if (newParentNode.getType() == JAMSNode.COMPONENT_TYPE) {
            JAMSNode siblingNode = newParentNode;
            newParentNode = (JAMSNode) newParentNode.getParent();
            position = newParentNode.getIndex(siblingNode);
            if (draggedNode.getParent().getIndex(draggedNode) > position) {
                position++;
            }
            if (draggedNode.getParent() != newParentNode) {
//                position++;
            }
        } else {
            position = newParentNode.getChildCount();
        }

        if (action == DnDConstants.ACTION_COPY) {

            JAMSNode newNode = (JAMSNode) JAMSTree.makeDeepCopy(draggedNode, target);
            newNode.setType(draggedNode.getType());

            if (target instanceof ModelTree) {
                if (!fixPendingContexts(newNode, newParentNode)) {
                    return false;
                }
            }

            target.expandPath(new TreePath(newParentNode.getPath()));
            ((DefaultTreeModel) target.getModel()).insertNodeInto(newNode, newParentNode, position);
            TreePath treePath = new TreePath(newNode.getPath());
            int i = 0;

            for (Enumeration enumeration = newNode.depthFirstEnumeration(); enumeration.hasMoreElements(); i++) {
                JAMSNode element = (JAMSNode) enumeration.nextElement();
                TreePath path = new TreePath(element.getPath());
                if (((Boolean) expandedStates.get(i)).booleanValue()) {
                    target.expandPath(path);
                }
            }

            target.scrollPathToVisible(treePath);
            target.setSelectionPath(treePath);

            return true;
        }
        if (action == DnDConstants.ACTION_MOVE) {

            if (target instanceof ModelTree) {
                if (!fixPendingContexts(draggedNode, newParentNode)) {
                    return false;
                }
            }

            target.saveExpandedState(new TreePath(target.getModel().getRoot()));

            draggedNode.removeFromParent();
            target.expandPath(new TreePath(newParentNode.getPath()));

            ((DefaultTreeModel) target.getModel()).insertNodeInto(draggedNode, newParentNode, position);

            TreePath treePath = new TreePath(draggedNode.getPath());

            int i = 0;
            for (Enumeration enumeration = draggedNode.depthFirstEnumeration(); enumeration.hasMoreElements(); i++) {
                JAMSNode element = (JAMSNode) enumeration.nextElement();
                TreePath path = new TreePath(element.getPath());
                if (((Boolean) expandedStates.get(i)).booleanValue()) {
                    target.expandPath(path);
                }
            }

            target.scrollPathToVisible(treePath);
            target.setSelectionPath(treePath);

            TreePath newtreePath = new TreePath(draggedNode.getPath());
            target.scrollPathToVisible(newtreePath);
            target.setSelectionPath(newtreePath);


            return true;
        }
        return false;
    }

    private boolean fixPendingContexts(JAMSNode rootNode, JAMSNode parentNode) {

        JAMSNode node;
        ComponentDescriptor cd;
        HashSet<String> contexts = new HashSet<String>();
        HashMap<String, HashSet<ComponentDescriptor>> pendingContexts = new HashMap<String, HashSet<ComponentDescriptor>>();
        
        Enumeration nodeEnum = rootNode.breadthFirstEnumeration();

        while (nodeEnum.hasMoreElements()) {
            node = (JAMSNode) nodeEnum.nextElement();
            cd = (ComponentDescriptor) node.getUserObject();

            if (JAMSContext.class.isAssignableFrom(cd.getClazz())) {
                contexts.add(cd.getInstanceName());
            }

            // create list if ancestor nodes in the new (sub)tree
            HashSet<String> ancestors = new HashSet<String>();
            ancestors.add(parentNode.getUserObject().toString());
            JAMSNode ancestor = (JAMSNode) parentNode.getParent();
            while (ancestor != null) {
                ancestors.add(ancestor.getUserObject().toString());
                ancestor = (JAMSNode) ancestor.getParent();
            }
            
            for (ComponentField var : cd.getComponentFields().values()) {
                if (var.getContext() != null) {
                    String contextName = var.getContext().getInstanceName();

                    // check if context is part of new ancestors
                    if (ancestors.contains(contextName)) {
                        continue;
                    }

                    // check if context has been moved as well
                    if (!contexts.contains(contextName)) {
                        HashSet<ComponentDescriptor> components = pendingContexts.get(contextName);
                        if (components == null) {
                            components = new HashSet<ComponentDescriptor>();
                            pendingContexts.put(contextName, components);
                        }
                        components.add(cd);
                    //System.out.println(var.name + " references " + contextName);
                    }
                }
            }
        }

        //put new parent and all of its ancestors into vector for creating a select box
        Vector<String> ancestorNames = new Vector<String>();

        //put new parent and all of its ancestors into a hashmap for access by name
        HashMap<String, ComponentDescriptor> ancestors = new HashMap<String, ComponentDescriptor>();

        cd = (ComponentDescriptor) parentNode.getUserObject();
        ancestorNames.add(cd.toString());
        ancestors.put(cd.toString(), cd);

        JAMSNode ancestor = (JAMSNode) parentNode.getParent();
        while (ancestor != null) {
            cd = (ComponentDescriptor) ancestor.getUserObject();
            ancestorNames.add(cd.toString());
            ancestors.put(cd.toString(), cd);
            ancestor = (JAMSNode) ancestor.getParent();
        }
        cd = (ComponentDescriptor) rootNode.getUserObject();
        ancestorNames.add(cd.toString());
        ancestors.put(cd.toString(), cd);

        String ancestorNameArray[] = ancestorNames.toArray(new String[ancestorNames.size()]);

        //sort pending contexts
        ArrayList<String> pendingContextList = new ArrayList<String>(pendingContexts.keySet());
        Collections.sort(pendingContextList);

        //iterate over all pending contexts
        for (String oldContextName : pendingContextList) {
            HashSet<ComponentDescriptor> components = pendingContexts.get(oldContextName);

            //open a dialog for specification of new context and get new context
            if (dlg.show(oldContextName, ancestorNameArray, components) == ContextReplaceDlg.CANCEL_OPTION) {
                return false;
            }
            ContextDescriptor newContext = (ContextDescriptor) ancestors.get(dlg.getContext());

            //iterate over all components referencing pending contexts
            for (ComponentDescriptor component : components) {
                //iterate over all vars
                for (ComponentField var : component.getComponentFields().values()) {
                    if (var.getContext() != null) {
                        //again select vars that reference this pending context and connect to new (selected) context
                        if (var.getContext().getInstanceName().equals(oldContextName)) {
                            try {
                                //@TODO: proper handling
                                var.linkToAttribute(newContext, var.getAttribute());
                                //component.linkComponentAttribute(var.name, newContext, var.getAttribute());
                                //var.context = newContext;
                            } catch (JAMSException ex) {
                                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
