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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2010 Sun
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

package org.netbeans.modules.db.explorer.dlg;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ConnectionNamePanel extends javax.swing.JPanel {
    public final static String PROP_CONNECTION_NAME = "ConnectionName";

    /**
     * Creates a new form SchemaPanel
     * 
     * @param dbcon instance of DatabaseConnection object
     */
    public ConnectionNamePanel(ConnectionDialogMediator mediator, String connectionName) {
        initComponents();
        this.inputConnectionNameTextField.setText(connectionName);
        this.inputConnectionNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent de) {
                firePropertyChange(PROP_CONNECTION_NAME, null, getConntionName());
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                firePropertyChange(PROP_CONNECTION_NAME, null, getConntionName());
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                firePropertyChange(PROP_CONNECTION_NAME, null, getConntionName());
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        commentTextArea = new javax.swing.JTextArea();
        inputConnectionNameLabel = new javax.swing.JLabel();
        inputConnectionNameTextField = new javax.swing.JTextField();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));

        setMinimumSize(new java.awt.Dimension(174, 163));
        setPreferredSize(new java.awt.Dimension(174, 163));
        setLayout(new java.awt.GridBagLayout());

        commentTextArea.setEditable(false);
        commentTextArea.setFont(javax.swing.UIManager.getFont("Label.font"));
        commentTextArea.setLineWrap(true);
        commentTextArea.setText(org.openide.util.NbBundle.getMessage(ConnectionNamePanel.class, "MSG_ConnectionNamePanelComment")); // NOI18N
        commentTextArea.setWrapStyleWord(true);
        commentTextArea.setDisabledTextColor(javax.swing.UIManager.getColor("Label.foreground"));
        commentTextArea.setEnabled(false);
        commentTextArea.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 11);
        add(commentTextArea, gridBagConstraints);

        inputConnectionNameLabel.setLabelFor(inputConnectionNameTextField);
        org.openide.awt.Mnemonics.setLocalizedText(inputConnectionNameLabel, org.openide.util.NbBundle.getMessage(ConnectionNamePanel.class, "ConnectionNameDialogText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 5, 12);
        add(inputConnectionNameLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 5, 12);
        add(inputConnectionNameTextField, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weighty = 1.0;
        add(filler1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1.0;
        add(filler2, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea commentTextArea;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JLabel inputConnectionNameLabel;
    private javax.swing.JTextField inputConnectionNameTextField;
    // End of variables declaration//GEN-END:variables

    public String getConntionName() {
        return this.inputConnectionNameTextField.getText();
    }
    
    public void setConnectionName(String connectionName) {
        this.inputConnectionNameTextField.setText(connectionName);
    }
}