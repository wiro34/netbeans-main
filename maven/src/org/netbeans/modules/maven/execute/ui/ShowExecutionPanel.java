/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.maven.execute.ui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import static org.netbeans.modules.maven.execute.ui.Bundle.*;
import javax.swing.tree.TreeSelectionModel;
import org.apache.maven.execution.ExecutionEvent;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.ModelUtils;
import org.netbeans.modules.maven.api.execute.RunConfig;
import org.netbeans.modules.maven.execute.cmd.ExecutionEventObject;
import org.netbeans.modules.maven.execute.cmd.ExecMojo;
import org.netbeans.modules.maven.execute.cmd.ExecProject;
import org.netbeans.modules.maven.spi.IconResources;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Pair;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author mkleint
 */
public class ShowExecutionPanel extends javax.swing.JPanel implements ExplorerManager.Provider {
        private static final @StaticResource String ERROR_ICON = "org/netbeans/modules/maven/execute/ui/error.png";
        private static final @StaticResource String LIFECYCLE_ICON = "org/netbeans/modules/maven/execute/ui/lifecycle.png";
        private static final @StaticResource String ICON_PHASE = "org/netbeans/modules/maven/execute/ui/phase.png"; 
        
    private final ExplorerManager manager;
    private boolean showPhases = false;
    private boolean showOnlyErrors = false;
    private Project prj;
    private RunConfig config;

    /**
     * Creates new form ShowExecutionPanel
     */
    public ShowExecutionPanel() {
        manager = new ExplorerManager();
        initComponents();
        ((BeanTreeView)btvExec).setRootVisible(false);
        ((BeanTreeView)btvExec).setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        btnPhaseToggle.setIcon(ImageUtilities.loadImageIcon(ICON_PHASE, true));
        btnPhaseToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                showPhases = btnPhaseToggle.isSelected();
                ExecutionEventObject.Tree item = manager.getRootContext().getLookup().lookup(ExecutionEventObject.Tree.class);
                manager.setRootContext(createNodeForExecutionEventTree(item));
                expandCollapseChildNodes(manager.getRootContext(), false, false);

            }
        });
        btnFailedToggle.setIcon(ImageUtilities.loadImageIcon(ERROR_ICON, true));
        btnFailedToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                showOnlyErrors = btnFailedToggle.isSelected();
                ExecutionEventObject.Tree item = manager.getRootContext().getLookup().lookup(ExecutionEventObject.Tree.class);
                manager.setRootContext(createNodeForExecutionEventTree(item));
                expandCollapseChildNodes(manager.getRootContext(), false, false);
                
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btvExec = new BeanTreeView();
        jToolBar1 = new javax.swing.JToolBar();
        btnPhaseToggle = new javax.swing.JToggleButton();
        btnFailedToggle = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnCollapse = new javax.swing.JButton();
        btnExpand = new javax.swing.JButton();

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        org.openide.awt.Mnemonics.setLocalizedText(btnPhaseToggle, org.openide.util.NbBundle.getMessage(ShowExecutionPanel.class, "ShowExecutionPanel.btnPhaseToggle.text")); // NOI18N
        btnPhaseToggle.setToolTipText(org.openide.util.NbBundle.getMessage(ShowExecutionPanel.class, "ShowExecutionPanel.btnPhaseToggle.toolTipText")); // NOI18N
        btnPhaseToggle.setFocusable(false);
        jToolBar1.add(btnPhaseToggle);

        org.openide.awt.Mnemonics.setLocalizedText(btnFailedToggle, org.openide.util.NbBundle.getMessage(ShowExecutionPanel.class, "ShowExecutionPanel.btnFailedToggle.text")); // NOI18N
        btnFailedToggle.setToolTipText(org.openide.util.NbBundle.getMessage(ShowExecutionPanel.class, "ShowExecutionPanel.btnFailedToggle.toolTipText")); // NOI18N
        btnFailedToggle.setFocusable(false);
        jToolBar1.add(btnFailedToggle);
        jToolBar1.add(jSeparator1);

        org.openide.awt.Mnemonics.setLocalizedText(btnCollapse, org.openide.util.NbBundle.getMessage(ShowExecutionPanel.class, "ShowExecutionPanel.btnCollapse.text")); // NOI18N
        btnCollapse.setToolTipText(org.openide.util.NbBundle.getMessage(ShowExecutionPanel.class, "ShowExecutionPanel.btnCollapse.toolTipText")); // NOI18N
        btnCollapse.setFocusable(false);
        btnCollapse.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnCollapse.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnCollapse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCollapseActionPerformed(evt);
            }
        });
        jToolBar1.add(btnCollapse);

        org.openide.awt.Mnemonics.setLocalizedText(btnExpand, org.openide.util.NbBundle.getMessage(ShowExecutionPanel.class, "ShowExecutionPanel.btnExpand.text")); // NOI18N
        btnExpand.setToolTipText(org.openide.util.NbBundle.getMessage(ShowExecutionPanel.class, "ShowExecutionPanel.btnExpand.toolTipText")); // NOI18N
        btnExpand.setFocusable(false);
        btnExpand.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnExpand.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnExpand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExpandActionPerformed(evt);
            }
        });
        jToolBar1.add(btnExpand);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btvExec)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 564, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(btvExec, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnCollapseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCollapseActionPerformed
        expandCollapseChildNodes(manager.getRootContext(), true, true);
    }//GEN-LAST:event_btnCollapseActionPerformed

    private void btnExpandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExpandActionPerformed
        expandCollapseChildNodes(manager.getRootContext(), false, true);
    }//GEN-LAST:event_btnExpandActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCollapse;
    private javax.swing.JButton btnExpand;
    private javax.swing.JToggleButton btnFailedToggle;
    private javax.swing.JToggleButton btnPhaseToggle;
    private javax.swing.JScrollPane btvExec;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables

    @Override
    public ExplorerManager getExplorerManager() {
        return manager;
    }
    
    public void setTreeToDisplay(ExecutionEventObject.Tree item, RunConfig config) {
        this.prj = config.getProject();
        this.config = config;
        manager.setRootContext(createNodeForExecutionEventTree(item));
        
        manager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                    Object oldO = evt.getOldValue();
                    if (oldO instanceof Node[]) {
                        
                    }
                    Object newO = evt.getNewValue();
                    if (newO instanceof Node[]) {
                        Node[] sel = (Node[]) newO;
                        if (sel.length > 0) {
                            ExecutionEventObject.Tree tree = sel[0].getLookup().lookup(ExecutionEventObject.Tree.class);
                            if (tree != null) {
                                tree.expandFold();
                                tree.getStartOffset().scrollTo();
                            }
                        }
                    }
                }
//                    Node[] nds = manager.getSelectedNodes();
//                    if (nds.length > 0) {
//                        ExecutionEventObject.Tree tree = nds[0].getLookup().lookup(ExecutionEventObject.Tree.class);
//                        if (tree != null) {
//                            tree.getStartOffset().scrollTo();
//                        }
//                    }
//                }
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        expandCollapseChildNodes(manager.getRootContext(), false, false);
    }
    
    
    
    private Node createNodeForExecutionEventTree(ExecutionEventObject.Tree item) {
        ExecutionEventObject se = item.getStartEvent();
        if (se != null) {
            //TODO
            AbstractNode nd = new AbstractNode(createChildren(item.getChildrenNodes()), Lookups.fixed(item));
            switch (se.type) {
                case ProjectStarted :
                    return new ProjectNode(showPhases ? createPhasedChildren(item.getChildrenNodes()) : createChildren(item.getChildrenNodes()), Lookups.fixed(item));
                case MojoStarted :
                    return new MojoNode(createChildren(item.getChildrenNodes()), Lookups.fixed(item, config));
                case ForkStarted :
                case ForkedProjectStarted :
                default :
                    nd.setDisplayName(se.type.name());
                    nd.setName(se.type.name());
                    break;
            }
            
            return nd;
        }
        return new AbstractNode(createChildren(item.getChildrenNodes()), Lookups.fixed(item));
    }
    
    private Children createChildren(final List<ExecutionEventObject.Tree> childrenNodes) {
        if (childrenNodes.isEmpty()) {
            return Children.LEAF;
        }
        return Children.create(new ChildFactory<ExecutionEventObject.Tree>() {
            @Override
            protected boolean createKeys(List<ExecutionEventObject.Tree> toPopulate) {
                if (showOnlyErrors) {
                    for (ExecutionEventObject.Tree item : childrenNodes) {
                        ExecutionEventObject end = item.getEndEvent();
                        if (end != null) {
                            if (
                                ExecutionEvent.Type.ProjectFailed.equals(end.type) || 
                                ExecutionEvent.Type.MojoFailed.equals(end.type) ||
                                ExecutionEvent.Type.ForkedProjectFailed.equals(end.type) ||    
                                ExecutionEvent.Type.ForkFailed.equals(end.type))
                            {
                                toPopulate.add(item);
                            }
                        }
                    }
                } else {
                    toPopulate.addAll(childrenNodes);
                }

                return true;
            }
            
            @Override
            protected Node createNodeForKey(ExecutionEventObject.Tree key) {
                return createNodeForExecutionEventTree(key);
            }
            
        }, false);
    }
    
    private Children createPhasedChildren(final List<ExecutionEventObject.Tree> childrenNodes) {
        return Children.create(new ChildFactory<Pair<String, List<ExecutionEventObject.Tree>>>() {
            @Override
            protected boolean createKeys(List<Pair<String, List<ExecutionEventObject.Tree>>> toPopulate) {
                
                Map<String, Pair<String, List<ExecutionEventObject.Tree>>> phases = new HashMap<String, Pair<String, List<ExecutionEventObject.Tree>>>();
                for (ExecutionEventObject.Tree item : childrenNodes) {
                    ExecMojo mojo = (ExecMojo) item.getStartEvent();
                    String phaseString = mojo.phase != null ? mojo.phase : "<none>";
                    Pair<String, List<ExecutionEventObject.Tree>> phase = phases.get(phaseString);
                    if (phase == null) {
                        phase = Pair.<String, List<ExecutionEventObject.Tree>>of(phaseString, new ArrayList<ExecutionEventObject.Tree>());
                        phases.put(phaseString, phase);
                        toPopulate.add(phase);
                    }
                    phase.second().add(item);
                }
                return true;
            }
            
            @Override
            protected Node createNodeForKey(Pair<String, List<ExecutionEventObject.Tree>> key) {
                return createPhaseNode(key);
            }

            
        }, false);
    }    
    
    private Node createPhaseNode(Pair<String, List<ExecutionEventObject.Tree>> key) {
        Children childs;
        if (showOnlyErrors) {
            boolean atLeastOne = false;
            for (ExecutionEventObject.Tree ch : key.second()) {
                ExecutionEventObject end = ch.getEndEvent();
                if (end != null) {
                    if (ExecutionEvent.Type.ProjectFailed.equals(end.type)
                            || ExecutionEvent.Type.MojoFailed.equals(end.type)
                            || ExecutionEvent.Type.ForkedProjectFailed.equals(end.type)
                            || ExecutionEvent.Type.ForkFailed.equals(end.type)) {
                        atLeastOne = true;
                        break;
                    }
                }
            }
            if (atLeastOne) {
                childs = createChildren(key.second());
            } else {
                childs = Children.LEAF;
            }
        } else {
            childs = createChildren(key.second());
        }
        AbstractNode nd = new AbstractNode(childs, Lookup.EMPTY);
        nd.setName(key.first());
        nd.setDisplayName(key.first());
        nd.setIconBaseWithExtension(ICON_PHASE);
        return nd;
    }

    
    private void expandCollapseChildNodes(Node parent, boolean collapse, boolean recursive) {
        for (Node nd : parent.getChildren().getNodes(true)) {
            if (recursive) {
                expandCollapseChildNodes(nd, collapse, recursive);
            }
            if (collapse) {
                ((BeanTreeView)btvExec).collapseNode(nd);
            } else {
                ((BeanTreeView)btvExec).expandNode(nd);
            }
        }
    }
    
    private static class MojoNode extends AbstractNode {
        private final ExecMojo start;
        private final ExecMojo end;
        private final ExecutionEventObject.Tree tree;
        private final RunConfig config;

        public MojoNode(Children children, Lookup lookup) {
            super(children, lookup);
            this.tree = lookup.lookup(ExecutionEventObject.Tree.class);
            this.start = (ExecMojo) tree.getStartEvent();
            this.end = (ExecMojo) tree.getEndEvent();
            config = lookup.lookup(RunConfig.class);
            assert start != null && end != null;
            
            setIconBaseWithExtension(IconResources.MOJO_ICON);
            setDisplayName(start.goal);
        }


        @Override
        public String getHtmlDisplayName() {
            return "<html><b>" + start.goal + "</b>" + (end.getErrorMessage() != null ? " <font color='#a40000'>" + end.getErrorMessage() + "</font>" : "") +" <font color='!controlShadow'>" + start.plugin.groupId + ":" + start.plugin.artifactId + ":" + start.plugin.version + "</font></html>";
        }

        @Override
        public String getShortDescription() {
            return "<html>Goal: " + start.goal + "<br/>Phase: " + start.phase + "<br/>Execution Id: " + start.executionId + (end.getErrorMessage() != null ? "<br/>Error: <b>" + end.getErrorMessage() + "</b>": "") + "</html>";
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[] {
                new GotoOutputAction(tree),
                new GotoSourceAction(start),
                new GotoPluginSourceAction(start, config), 
                new DebugPluginSourceAction(start, config)
            };
        }
        
        
        

        @Override
        public Image getIcon(int type) {
            Image img =  super.getIcon(type);
            if (ExecutionEvent.Type.MojoFailed.equals(end.type)) {
                Image ann = ImageUtilities.loadImage(ERROR_ICON); //NOI18N
//                ann = ImageUtilities.addToolTipToImage(ann, "Mojo execution failed");
                return ImageUtilities.mergeImages(img, ann, 8, 0);//NOI18N
            }
            return img;
        }

    }
    private static class ProjectNode extends AbstractNode {
        private final ExecProject start;
        private final ExecProject end;

        public ProjectNode(Children children, Lookup lookup) {
            super(children, lookup);
            this.start = (ExecProject) lookup.lookup(ExecutionEventObject.Tree.class).getStartEvent();
            this.end = (ExecProject) lookup.lookup(ExecutionEventObject.Tree.class).getEndEvent();
            assert start != null && end != null;

            setIconBaseWithExtension(LIFECYCLE_ICON);
            setDisplayName(start.gav.artifactId);
        }


        @Override
        public String getHtmlDisplayName() {
            return "<html><font color='!controlShadow'>" + start.gav.groupId + " </font><b>" + start.gav.artifactId + "</b> " + start.gav.version + "</html>";
        }

        @Override
        public String getShortDescription() {
            return "<html>Project GAV: " + start.gav.getId() + (start.currentProjectLocation != null ? "<br/>Location: " + start.currentProjectLocation.getAbsolutePath() : "") + "</html>";
        }
        
        

        @Override
        public Image getIcon(int type) {
            Image img =  super.getIcon(type);
            if (ExecutionEvent.Type.ProjectFailed.equals(end.type)) {
                Image ann = ImageUtilities.loadImage(ERROR_ICON); //NOI18N
        //        ann = ImageUtilities.addToolTipToImage(ann, "Project build failed");
                return ImageUtilities.mergeImages(img, ann, 8, 0);//NOI18N
            }
            return img;
        }        

        @Override
        public Image getOpenedIcon(int type) {
            return this.getIcon(type); 
        }

    
    
    
     
    }

    private static class GotoOutputAction extends AbstractAction {
        private final ExecutionEventObject.Tree item;
        @NbBundle.Messages("ACT_GOTO_Output=Go to Build Output")
        public GotoOutputAction(ExecutionEventObject.Tree item) {
            putValue(NAME, ACT_GOTO_Output());
            this.item = item;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            item.expandFold();
            item.getStartOffset().scrollTo();
        }
    }

    private static class GotoSourceAction extends AbstractAction {
        private final ExecMojo mojo;

        @NbBundle.Messages("ACT_GOTO_Exec=Go to POM definition")
        public GotoSourceAction(ExecMojo start) {
            putValue(NAME, ACT_GOTO_Exec());
            setEnabled(start.getLocation() != null);
            this.mojo = start;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (mojo.getLocation() != null) {
                ModelUtils.openAtSource(mojo.getLocation());
            }
        }
    }
    
}