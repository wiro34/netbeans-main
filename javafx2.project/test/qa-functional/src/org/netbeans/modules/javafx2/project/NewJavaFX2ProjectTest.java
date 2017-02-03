/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javafx2.project;

import junit.framework.Test;
import org.netbeans.jellytools.JellyTestCase;

/**
 *
 * @author stezeb
 */
public class NewJavaFX2ProjectTest extends JellyTestCase {

    /* Test project names */
    private final String PLAINAPP_NAME = "FXApp";
    private final String PRELOADER_NAME = "FXPreloader";
    private final String FXMLAPP_NAME = "FXMLApp";
    private final String SWINGAPP_NAME = "FXSwingApp";

    /**
     * Constructor required by JUnit
     */
    public NewJavaFX2ProjectTest(String testName) {
        super(testName);
    }

    /**
     * Creates suite from particular test cases.
     */
    public static Test suite() {
        return createModuleTest(NewJavaFX2ProjectTest.class,
                "createJavaFX2Application",
                "createJavaFX2Preloader",
                "createJavaFX2FXMLApp",
                "createJavaFX2SwingApp",
                "closeJavaFX2Application",
                "closeJavaFX2Preloader",
                "closeJavaFX2FXMLApp",
                "closeJavaFX2SwingApp");
    }

    @Override
    public void setUp() {
        System.out.println("########  " + getName() + "  #######");
    }

    public void createJavaFX2Application() {
        TestUtils.createJavaFX2Project(TestUtils.JAVAFX_PROJECT_TYPE_PLAIN,
                PLAINAPP_NAME, getWorkDirPath());
    }

    public void closeJavaFX2Application() {
        TestUtils.closeJavaFX2Project(PLAINAPP_NAME);
    }

    public void createJavaFX2Preloader() {
        TestUtils.createJavaFX2Project(TestUtils.JAVAFX_PROJECT_TYPE_PRELOADER,
                PRELOADER_NAME, getWorkDirPath());
    }

    public void closeJavaFX2Preloader() {
        TestUtils.closeJavaFX2Project(PRELOADER_NAME);
    }

    public void createJavaFX2FXMLApp() {
        TestUtils.createJavaFX2Project(TestUtils.JAVAFX_PROJECT_TYPE_FXMLAPP,
                FXMLAPP_NAME, getWorkDirPath());
    }

    public void closeJavaFX2FXMLApp() {
        TestUtils.closeJavaFX2Project(FXMLAPP_NAME);
    }

    public void createJavaFX2SwingApp() {
        TestUtils.createJavaFX2Project(TestUtils.JAVAFX_PROJECT_TYPE_SWINGAPP,
                SWINGAPP_NAME, getWorkDirPath());
    }

    public void closeJavaFX2SwingApp() {
        TestUtils.closeJavaFX2Project(SWINGAPP_NAME);
    }
    
}