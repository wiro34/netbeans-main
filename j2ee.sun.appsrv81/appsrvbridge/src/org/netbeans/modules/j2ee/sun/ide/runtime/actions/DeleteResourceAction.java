/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.j2ee.sun.ide.runtime.actions;

import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.NodeAction;
import org.openide.util.NbBundle;
import org.openide.util.HelpCtx;

import org.netbeans.modules.j2ee.sun.bridge.apis.AppserverMgmtNode;
import org.netbeans.modules.j2ee.sun.bridge.apis.Removable;
import org.netbeans.modules.j2ee.sun.ide.runtime.nodes.ConnectionPoolNode;
import org.netbeans.modules.j2ee.sun.util.NodeTypes;
import org.netbeans.modules.j2ee.sun.util.GUIUtils;

import org.netbeans.modules.j2ee.sun.bridge.apis.RefreshCookie;

/**
 *
 *
 */
public class DeleteResourceAction extends NodeAction {
    
    
    /**
     *
     *
     */
    protected void performAction(Node[] activatedNodes) {
        if (activatedNodes==null){
            return;
        }
        
        for (int i=0;i<activatedNodes.length;i++){
            Node node = activatedNodes[i];
            Lookup lookup = node.getLookup();
            
            Object obj2 = lookup.lookup(AppserverMgmtNode.class);
            String nodeType = null;
            
            if(obj2 instanceof AppserverMgmtNode) {
                AppserverMgmtNode appMgmtNode = (AppserverMgmtNode) obj2;
                nodeType = appMgmtNode.getNodeType();
            }
            
            try {
            /*
             check if jdbc connection pool resource and get dependent reources
             to delete
             */
                if(NodeTypes.CONNECTION_POOL.equals(nodeType)) {
                    Object proceed =
                            GUIUtils.showWarning(
                            getLocalizedString("dependent_resources_notify"));
                    if(proceed == NotifyDescriptor.OK_OPTION){
                        deleteDependentResources(node);
                        removeResource(node);
                    }
                } else {
                    removeResource(node);
                }
                
                //refresh parent node of that which was deleted
                Node parentNode = node.getParentNode();
                RefreshCookie refreshAction =
                        (RefreshCookie)parentNode.getCookie(RefreshCookie.class);
                if (refreshAction != null){
                    refreshAction.refresh();
                }
            } catch(java.lang.RuntimeException rex) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL,rex);
            }
        }
    }
    
    
    /**
     * Removes the resource given the netbeans node.
     *
     * @param node The netbeans node.
     */
    private void removeResource(final Node node) {
        Lookup lookup = node.getLookup();
        Object obj = lookup.lookup(Removable.class);
        if(obj instanceof Removable) {
            Removable removeableObj = (Removable)obj;
            removeableObj.remove();
        }
    }
    
    
    /**
     *
     *
     */
    private void deleteDependentResources(final Node node) {
        Lookup lookup = node.getLookup();
        Object obj = lookup.lookup(ConnectionPoolNode.class);
        if(obj instanceof ConnectionPoolNode) {
            ConnectionPoolNode connPoolNode = (ConnectionPoolNode) obj;
            connPoolNode.removeDependentJDBCResources();
        }
        refreshJDBCResourcesFolder(node);
    }
    
    /**
     *
     *
     */
    private void refreshJDBCResourcesFolder(final Node node){
        try {
            Node parentNode = node.getParentNode();
            org.openide.nodes.Children ch = parentNode.getParentNode().getChildren();
            Node[] resourceNodes = ch.getNodes(true);
            if(resourceNodes.length > 0){
                Node jdbcMgmtNode = resourceNodes[0];
                RefreshCookie refreshAction = (RefreshCookie)jdbcMgmtNode.getCookie(RefreshCookie.class);
                if (refreshAction != null) {
                    refreshAction.refresh();
                }
            }
        }catch(Exception ex){
            //Failed to refresh peer container node containing JDBC resources
            //Manual refresh of UI will show updated list
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL,ex);
        }
    }
    
    /**
     *
     *
     */
    protected boolean enable(Node[] nodes) {
        return ((nodes != null) && (nodes.length >= 1)) ? true : false;
    }
    
    
    /**
     *
     *
     */
    protected boolean asynchronous() {
        return false;
    }
    
    
    /**
     *
     *
     */
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    /**
     *
     */
    public String getName() {
        return NbBundle.getMessage(DeleteResourceAction.class,
                "LBL_DeleteResourceAction");
    }
    
    
    /**
     *
     *
     */
    private static String getLocalizedString(final String bundleStrProp) {
        return NbBundle.getMessage(DeleteResourceAction.class, bundleStrProp);
    }
    
}
