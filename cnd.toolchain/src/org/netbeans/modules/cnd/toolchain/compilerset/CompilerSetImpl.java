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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.cnd.toolchain.compilerset;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.cnd.api.toolchain.Tool;
import org.netbeans.modules.cnd.api.toolchain.CompilerFlavor;
import org.netbeans.modules.cnd.spi.toolchain.CompilerProvider;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.api.toolchain.ToolKind;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.AlternativePath;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.BaseFolder;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.CMakeDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.CompilerDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.DebuggerDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.LinkerDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.MakeDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.PredefinedMacro;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.QMakeDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.ScannerDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.ScannerPattern;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.ToolchainDescriptor;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.cnd.utils.FSPath;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.remote.spi.FileSystemProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * A container for information about a set of related compilers, typically from a vendor or
 * re-distributor.
 */
public final class CompilerSetImpl extends CompilerSet {

    private final CompilerFlavor flavor;
    private String name;
    private final String displayName;
    private boolean autoGenerated;
    private boolean isDefault;
    private final String directory;
    private  String commandDirectory;
    private final ArrayList<Tool> tools = new ArrayList<>();
    private final CompilerProvider compilerProvider;
    private Map<ToolKind,String> pathSearch;
    private boolean isSunStudioDefault;
    private Charset charset;
    private String modifyBuildPath;
    private String modifyRunPath;

    @Override
    public boolean isAutoGenerated() {
        return autoGenerated;
    }

    public void setAutoGenerated(boolean autoGenerated) {
        this.autoGenerated = autoGenerated;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean isUrlPointer(){
        if (getDirectory() == null || getDirectory().length() == 0){
            return flavor.getToolchainDescriptor().getUpdateCenterUrl() != null && flavor.getToolchainDescriptor().getModuleID() != null;
        }
        return false;
    }

    void setAsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void unsetDefault() {
        this.isDefault = false;  // to set to true use CompilerSetManager.setDefault()
    }

    public static CompilerSetImpl restore(ExecutionEnvironment env, CompilerFlavor flavor, String directory, String name, String displayName,
            String setBuildPath, String setRunPath) {
        return new CompilerSetImpl(env, flavor, directory, name, displayName, setBuildPath, setRunPath);
    }

    public static CompilerSetImpl create(CompilerFlavor flavor, ExecutionEnvironment env, String directory) {
        String setBuildPath;
        String setRunPath;
        if (env.isLocal() && Utilities.isWindows()) {
            setBuildPath = CompilerSetPreferences.DEFAULT_BUILD_PATH_WINDOWS;
            setRunPath = CompilerSetPreferences.DEFAULT_RUN_PATH_WINDOWS;
        } else {
            setBuildPath = CompilerSetPreferences.DEFAULT_BUILD_PATH_UNIX;
            setRunPath = CompilerSetPreferences.DEFAULT_RUN_PATH_UNIX;
        }
        String productName = foundProductName(flavor, env, directory);
        if (productName == null || productName.isEmpty()) {
            String name = createName(flavor, null);
            String displayName = extractDisplayName(flavor, null);
            return new CompilerSetImpl(env, flavor, directory, name, displayName, setBuildPath, setRunPath);
        } else {
            String displayName = extractDisplayName(flavor, productName);
            String name = createName(flavor, displayName);
            if (flavor instanceof CompilerFlavorImpl) {
                CompilerFlavorImpl.putProductName((CompilerFlavorImpl) flavor, displayName);
            }
            return new CompilerSetImpl(env, flavor, directory, name, displayName, setBuildPath, setRunPath);
        }
    }
    
    public CompilerSetImpl createCopy(ExecutionEnvironment env) {
        return createCopy(env, this.flavor, getDirectory(), name, displayName, isAutoGenerated(), true, modifyBuildPath, modifyRunPath);
    }

    /**
     * if null is passed as param value => "this" object value is used instead
     * @param flavor
     * @param directory
     * @param name
     * @param autoGenerated
     * @param def
     * @return
     */
    public CompilerSetImpl createCopy(ExecutionEnvironment env, CompilerFlavor flavor, String directory, String name, String displayName,
            boolean autoGenerated, boolean keepToolFlavor, String setBuildPath, String setRunPath) {
        flavor = (flavor == null) ? this.flavor : flavor;
        directory = (directory == null) ? getDirectory() : directory;
        name = (name == null) ? this.name : name;
        displayName = (displayName == null) ? this.displayName : displayName;
        setBuildPath = (setBuildPath == null) ? modifyBuildPath : setBuildPath;
        setRunPath = (setRunPath == null) ? modifyRunPath : setRunPath;
        if (setBuildPath == null) {
            if (env.isLocal() && Utilities.isWindows()) {
                setBuildPath = CompilerSetPreferences.DEFAULT_BUILD_PATH_WINDOWS;
            } else {
                setBuildPath = CompilerSetPreferences.DEFAULT_BUILD_PATH_UNIX;
            }
        }
        if (setRunPath == null) {
            if (env.isLocal() && Utilities.isWindows()) {
                setRunPath = CompilerSetPreferences.DEFAULT_RUN_PATH_WINDOWS;
            } else {
                setRunPath = CompilerSetPreferences.DEFAULT_RUN_PATH_UNIX;
            }
        }
        CompilerSetImpl copy = CompilerSetImpl.restore(env, flavor, directory, name, displayName, setBuildPath, setRunPath);
        copy.setAutoGenerated(autoGenerated);
        copy.setEncoding(getEncoding());

        for (Tool tool : getTools()) {
            CompilerFlavor toolFlavor;
            if (keepToolFlavor) {
                toolFlavor = tool.getFlavor();
            } else {
                toolFlavor = flavor;
            }
            copy.addTool(tool.createCopy(toolFlavor));
        }

        return copy;
    }

    public static String foundProductName(CompilerFlavor compilerFlavor, ExecutionEnvironment ee, String path){
        if (compilerFlavor.isSunStudioCompiler()) {
            FSPath fs = new FSPath(FileSystemProvider.getFileSystem(ee), path);
            FileObject fo = fs.getFileObject();
            if (fo != null && fo.isValid()) {
                FileObject productName = fo.getFileObject("../LEGAL/ProductName"); //NOI18N
                if (productName != null && productName.isValid()) {
                    try {
                        List<String> asLines = productName.asLines();
                        if (asLines != null && asLines.size() > 0) {
                            return asLines.get(0).trim();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace(System.err);
                    }
                }
            }
        }
        return null;
    }
    
    public static String createName(CompilerFlavor compilerFlavor, String displayName) {
        if (displayName != null && !displayName.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            for(int i = 0; i < displayName.length(); i++) {
                char c = displayName.charAt(i);
                if (c == ' ') {
                    if (i+1 < displayName.length() && Character.isDigit(displayName.charAt(i+1))) {
                        buf.append('_');
                    } if (i-1 >= 0 && Character.isDigit(displayName.charAt(i-1))) {
                        buf.append('_');
                    }
                } else {
                    buf.append(c);
                }
            }
            return buf.toString();
        }
        return compilerFlavor.getToolchainDescriptor().getName();
    }

    public static String extractDisplayName(CompilerFlavor flavor, String productName) {
        String displayName = flavor.getToolchainDescriptor().getDisplayName();
        int start = displayName.indexOf('{');
        int end = displayName.indexOf('}');
        if (start >= 0 && end > start) {
            if (productName != null) {
                return displayName.substring(0, start)+productName+displayName.substring(end+1);
            } else {
                return displayName.substring(0, start)+displayName.substring(start+1, end)+displayName.substring(end+1);
            }
        }
        //if (productName == null) {
            return displayName;
        //} else {
        //    return productName;
        //}
    }
    
    /** Creates a new instance of CompilerSet */
    private CompilerSetImpl(ExecutionEnvironment env, CompilerFlavor flavor, String directory, String name, String displayName,
            String setBuildPath, String setRunPath) {
        this.directory = directory == null ? "" : directory; // NOI18N

        compilerProvider = CompilerProvider.getInstance();

        //displayName = mapNameToDisplayName(flavor);
        if (displayName == null || displayName.isEmpty()) {
            this.displayName = extractDisplayName(flavor, null);
        } else {
            this.displayName = displayName;
        }
        if (name != null && !name.isEmpty()) {
            this.name = name;
        } else {
            this.name = flavor.toString();
        }
        this.flavor = flavor;
        this.autoGenerated = true;
        this.isDefault = false;
        if (env.isLocal() && Utilities.isWindows()) {
            this.modifyBuildPath = setBuildPath == null ? CompilerSetPreferences.DEFAULT_BUILD_PATH_WINDOWS : setBuildPath;
            this.modifyRunPath = setRunPath == null ? CompilerSetPreferences.DEFAULT_RUN_PATH_WINDOWS : setRunPath;
        } else {
            this.modifyBuildPath = setBuildPath == null ? CompilerSetPreferences.DEFAULT_BUILD_PATH_UNIX : setBuildPath;
            this.modifyRunPath = setRunPath == null ? CompilerSetPreferences.DEFAULT_RUN_PATH_UNIX: setRunPath;
        }
    }
    
    private CompilerSetImpl(int platform) {
        this.directory = ""; // NOI18N
        this.name = None;
        this.flavor = CompilerFlavorImpl.getUnknown(platform);
        this.displayName = NbBundle.getMessage(CompilerSetImpl.class, "LBL_EmptyCompilerSetDisplayName"); // NOI18N

        compilerProvider = CompilerProvider.getInstance();
        this.autoGenerated = true;
        this.isDefault = false;
        this.modifyBuildPath = CompilerSetPreferences.DEFAULT_TRIVIAL_PATH;
        this.modifyRunPath = CompilerSetPreferences.DEFAULT_TRIVIAL_PATH;
    }

    /**
     * If no compilers are found an empty compiler set is created so we don't have an empty list.
     * Too many places in CND expect a non-empty list and throw NPEs if it is empty!
     */
    protected static CompilerSet createEmptyCompilerSet(int platform) {
        return new CompilerSetImpl(platform);
    }

    @Override
    public CompilerFlavor getCompilerFlavor() {
        return flavor;
    }

    @Override
    public String getDirectory() {
        return directory;
    }
    
    @Override
    public String getModifyBuildPath() {
        return modifyBuildPath;
    }

    @Override
    public String getModifyRunPath() {
        return modifyRunPath;
    }

    public void setModifyBuildPath(String modifyBuildPath) {
        this.modifyBuildPath = modifyBuildPath;
    }

    public void setModifyRunPath(String modifyRunPath) {
        this.modifyRunPath = modifyRunPath;
    }

    @Override
    public String getCommandFolder() {
        synchronized(this) {
            if (commandDirectory == null) {
                commandDirectory = ToolUtils.getCommandFolder(this);
                if (commandDirectory == null) {
                    commandDirectory = "";
                }
            }
        }
        if (commandDirectory.isEmpty()) {
            return null;
        }
        return commandDirectory;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDisplayName() {
        // TODO: this thing is never used although it's being set to informative values by personality
        return displayName;
    }

    public Tool addTool(ExecutionEnvironment env, String name, String path, ToolKind kind, CompilerFlavor aFlavor) {
        if (findTool(kind) != null) {
            return null;
        }
        if (aFlavor == null) {
            aFlavor = getCompilerFlavor();
        }
        Tool tool = compilerProvider.createCompiler(env, aFlavor, kind, name, kind.getDisplayName(), path);
        if (!tools.contains(tool)) {
            tools.add(tool);
        }
        APIAccessor.get().setCompilerSet(tool, this);
        return tool;
    }

    /*package-local*/ void addTool(Tool tool) {
        tools.add(tool);
        APIAccessor.get().setCompilerSet(tool, this);
    }

    /*package-local*/ Tool addNewTool(ExecutionEnvironment env, String name, String path, ToolKind kind, CompilerFlavor aFlavor) {
        if (aFlavor == null) {
            aFlavor = getCompilerFlavor();
        }
        Tool tool = compilerProvider.createCompiler(env, aFlavor, kind, name, kind.getDisplayName(), path);
        tools.add(tool);
        APIAccessor.get().setCompilerSet(tool, this);
        return tool;
    }

    /**
     * Get the first tool of its kind.
     *
     * @param kind The type of tool to get
     * @return The Tool or null
     */
    @Override
    public Tool getTool(ToolKind kind) {
        for (Tool tool : tools) {
            if (tool.getKind() == kind) {
                return tool;
            }
        }
        CndUtils.assertFalse(true, "Should not be here, cuz we should create empty tools in CompilerSetManager"); //NOI18N
        //TODO: remove this code, empty tools should be created in CompilerSetManager
        Tool t;
        // Fixup: all tools should go here ....
        t = compilerProvider.createCompiler(ExecutionEnvironmentFactory.getLocal(),
                getCompilerFlavor(), kind, "", kind.getDisplayName(), ""); // NOI18N
        APIAccessor.get().setCompilerSet(t, this);
        synchronized( tools ) { // synchronize this only unpredictable tools modification
            tools.add(t);
        }
        return t;
    }


    /**
     * Get the first tool of its kind.
     *
     * @param kind The type of tool to get
     * @return The Tool or null
     */
    @Override
    public Tool findTool(ToolKind kind) {
        for (Tool tool : tools) {
            if (tool.getKind() == kind) {
                return tool;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tool> getTools() {
        synchronized (tools) {
            return (List<Tool>)tools.clone();
        }
    }

    /*package-local*/ void addPathCandidate(ToolKind tool, String path) {
        if (pathSearch == null){
            pathSearch = new HashMap<>();
        }
        pathSearch.put(tool, path);
    }

    /*package-local*/String getPathCandidate(ToolKind tool){
        if (pathSearch == null){
            return null;
        }
        return pathSearch.get(tool);
    }

    /*package-local*/void setSunStudioDefault(boolean isSunStudioDefault){
        this.isSunStudioDefault = isSunStudioDefault;
    }

    /*package-local*/boolean isSunStudioDefault(){
        return isSunStudioDefault;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Charset getEncoding() {
        if (charset == null) {
            if (flavor != null && flavor.isSunStudioCompiler()) {
                charset = Charset.defaultCharset();
            }
            if (charset == null) {
                charset = Charset.forName("UTF-8"); //NOI18N
                if (charset == null) {
                    charset = Charset.defaultCharset();
                }
            }
        }
        return charset;
    }

    public void setEncoding(Charset charset) {
        this.charset = charset;
    }

    private static class UnknownCompilerDescriptor implements CompilerDescriptor {
        private final CompilerDescriptor proxy;
        
        UnknownCompilerDescriptor(CompilerDescriptor proxy){
            this.proxy = proxy;
        }

        @Override
        public String getPathPattern() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getPathPattern();
        }

        @Override
        public String getExistFolder() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getExistFolder();
        }

        @Override
        public String getIncludeFlags() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getIncludeFlags();
        }

        @Override
        public String getUserIncludeFlag() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getUserIncludeFlag();
        }

        @Override
        public String getUserFileFlag() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getUserFileFlag();
        }

        @Override
        public String getIncludeParser() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getIncludeParser();
        }

        @Override
        public String getRemoveIncludePathPrefix() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getRemoveIncludePathPrefix();
        }

        @Override
        public String getRemoveIncludeOutputPrefix() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getRemoveIncludeOutputPrefix();
        }

        @Override
        public String getImportantFlags() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getImportantFlags();
        }

        @Override
        public String getMacroFlags() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getMacroFlags();
        }

        @Override
        public String getMacroParser() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getMacroParser();
        }

        @Override
        public List<PredefinedMacro> getPredefinedMacros() {
            if (proxy == null) {
                return Collections.emptyList();
            }
            return proxy.getPredefinedMacros();
        }

        @Override
        public String getUserMacroFlag() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getUserMacroFlag();
        }

        @Override
        public String[] getDevelopmentModeFlags() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getDevelopmentModeFlags();
        }

        @Override
        public String[] getWarningLevelFlags() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getWarningLevelFlags();
        }

        @Override
        public String[] getArchitectureFlags() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getArchitectureFlags();
        }

        @Override
        public String getStripFlag() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getStripFlag();
        }

        @Override
        public String[] getMultithreadingFlags() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getMultithreadingFlags();
        }

        @Override
        public String[] getStandardFlags() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getStandardFlags();
        }

        @Override
        public String[] getLanguageExtensionFlags() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getLanguageExtensionFlags();
        }

        @Override
        public String[] getCppStandardFlags() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getCppStandardFlags();
        }

        @Override
        public String[] getCStandardFlags() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getCStandardFlags();
        }

        @Override
        public String[] getLibraryFlags() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getLibraryFlags();
        }

        @Override
        public String getOutputObjectFileFlags() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getOutputObjectFileFlags();
        }

        @Override
        public String getDependencyGenerationFlags() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getDependencyGenerationFlags();
        }

        @Override
        public String getPrecompiledHeaderFlags() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getPrecompiledHeaderFlags();
        }

        @Override
        public String getPrecompiledHeaderSuffix() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getPrecompiledHeaderSuffix();
        }

        @Override
        public boolean getPrecompiledHeaderSuffixAppend() {
            if (proxy == null) {
                return false;
            }
            return proxy.getPrecompiledHeaderSuffixAppend();
        }

        @Override
        public String[] getNames() {
            if (proxy == null) {
                return new String[0];
            }
            return proxy.getNames();
        }

        @Override
        public String getVersionFlags() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getVersionFlags();
        }

        @Override
        public String getVersionPattern() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getVersionPattern();
        }

        @Override
        public String getFingerPrintFlags() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getFingerPrintFlags();
        }

        @Override
        public String getFingerPrintPattern() {
            if (proxy == null) {
                return "";//NOI18N
            }
            return proxy.getFingerPrintPattern();
        }

        @Override
        public boolean skipSearch() {
            if (proxy == null) {
                return true;
            }
            return proxy.skipSearch();
        }

        @Override
        public AlternativePath[] getAlternativePath() {
            if (proxy == null) {
                return new AlternativePath[0];
            }
            return proxy.getAlternativePath();
        }
    }

    public static class UnknownToolchainDescriptor implements ToolchainDescriptor {
        private static final CompilerDescriptor unknowDescriptor = new UnknownCompilerDescriptor(null);
        private final ToolchainDescriptor proxy;
        
        UnknownToolchainDescriptor(ToolchainDescriptor proxy) {
            this.proxy = proxy;
        }

        @Override
        public String getFileName() {
            if (proxy == null) {
                return ""; // NOI18N
            }
            return proxy.getFileName();
        }

        @Override
        public String getName() {
            return CompilerSet.UNKNOWN;
        }

        @Override
        public String getDisplayName() {
            return NbBundle.getMessage(UnknownToolchainDescriptor.class, "UnknownToolCollection"); // NOI18N
        }

        @Override
        public String[] getFamily() {
            if (proxy == null) {
                return new String[]{};
            }
            return proxy.getFamily();
        }

        @Override
        public String[] getPlatforms() {
            if (proxy == null) {
                return new String[]{};
            }
            return proxy.getPlatforms();
        }

        @Override
        public String getUpdateCenterUrl() {
            if (proxy == null) {
                return null;
            }
            return proxy.getUpdateCenterUrl();
        }

        @Override
        public String getUpdateCenterDisplayName() {
            if (proxy == null) {
                return null;
            }
            return proxy.getUpdateCenterDisplayName();
        }

        @Override
        public String getUpgradeUrl() {
            if (proxy == null) {
                return null;
            }
            return proxy.getUpgradeUrl();
        }

        @Override
        public String getModuleID() {
            if (proxy == null) {
                return null;
            }
            return proxy.getModuleID();
        }

        @Override
        public boolean isAbstract() {
            if (proxy == null) {
                return true;
            }
            return proxy.isAbstract();
        }

        @Override
        public boolean isAutoDetected() {
            if (proxy == null) {
                return true;
            }
            return proxy.isAutoDetected();
        }

        @Override
        public String[] getAliases() {
            if (proxy == null) {
                return new String[]{};
            }
            return proxy.getAliases();
        }

        @Override
        public String getSubstitute() {
            if (proxy == null) {
                return null;
            }
            return proxy.getSubstitute();
        }

        @Override
        public String getDriveLetterPrefix() {
            if (proxy == null) {
                return "/"; // NOI18N
            }
            return proxy.getDriveLetterPrefix();
        }

        @Override
        public List<BaseFolder> getBaseFolders() {
            if (proxy == null) {
                return Collections.<BaseFolder>emptyList();
            }
            return proxy.getBaseFolders();
        }

        @Override
        public List<BaseFolder> getCommandFolders() {
            if (proxy == null) {
                return Collections.<BaseFolder>emptyList();
            }
            return proxy.getCommandFolders();
        }

        @Override
        public String getQmakeSpec() {
            if (proxy == null) {
                return ""; // NOI18N
            }
            return proxy.getQmakeSpec();
        }

        @Override
        public CompilerDescriptor getC() {
            if (proxy == null) {
                return unknowDescriptor;
            }
            return proxy.getC();
        }

        @Override
        public CompilerDescriptor getCpp() {
            if (proxy == null) {
                return unknowDescriptor;
            }
            return proxy.getCpp();
        }

        @Override
        public CompilerDescriptor getFortran() {
            if (proxy == null) {
                return unknowDescriptor;
            }
            return proxy.getFortran();
        }

        @Override
        public CompilerDescriptor getAssembler() {
            if (proxy == null) {
                return unknowDescriptor;
            }
            return proxy.getAssembler();
        }

        @Override
        public ScannerDescriptor getScanner() {
            if (proxy == null) {
                return new ScannerDescriptor() {

                    @Override
                    public String getID() {
                        return ""; // NOI18N
                    }

                    @Override
                    public List<ScannerPattern> getPatterns() {
                        return Collections.emptyList();
                    }

                    @Override
                    public String getChangeDirectoryPattern() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getEnterDirectoryPattern() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getLeaveDirectoryPattern() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getMakeAllInDirectoryPattern() {
                        return ""; // NOI18N
                    }

                    @Override
                    public List<String> getStackHeaderPattern() {
                        return Collections.emptyList();
                    }

                    @Override
                    public List<String> getStackNextPattern() {
                        return Collections.emptyList();
                    }

                    @Override
                    public List<String> getFilterOutPatterns() {
                        return Collections.emptyList();
                    }
                };
            }
            return proxy.getScanner();
        }

        @Override
        public LinkerDescriptor getLinker() {
            if (proxy == null) {
                return new LinkerDescriptor(){

                    @Override
                    public String getLibraryPrefix() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getLibrarySearchFlag() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getDynamicLibrarySearchFlag() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getLibraryFlag() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getPICFlag() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getStaticLibraryFlag() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getDynamicLibraryFlag() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getDynamicLibraryBasicFlag() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getOutputFileFlag() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getStripFlag() {
                        return ""; // NOI18N
                    }

                    @Override
                    public String getPreferredCompiler() {
                        return ""; // NOI18N
                    }
                };
            }
            return proxy.getLinker();
        }

        @Override
        public MakeDescriptor getMake() {
            if (proxy == null) {
                return null;
            }
            return proxy.getMake();
        }

        @Override
        public Map<String, List<String>> getDefaultLocations() {
            if (proxy == null) {
                return Collections.<String, List<String>>emptyMap();
            }
            return proxy.getDefaultLocations();
        }

        @Override
        public DebuggerDescriptor getDebugger() {
            if (proxy == null) {
                return null;
            }
            return proxy.getDebugger();
        }

        @Override
        public String getMakefileWriter() {
            if (proxy == null) {
                return null;
            }
            return proxy.getMakefileWriter();
        }

        @Override
        public QMakeDescriptor getQMake() {
            if (proxy == null) {
                return new QMakeDescriptor() {

                    @Override
                    public String[] getNames() {
                        return new String[0];
                    }

                    @Override
                    public String getVersionFlags() {
                        return ""; //NOI18N
                    }

                    @Override
                    public String getVersionPattern() {
                        return ""; //NOI18N
                    }

                    @Override
                    public String getFingerPrintFlags() {
                        return ""; //NOI18N
                    }

                    @Override
                    public String getFingerPrintPattern() {
                        return ""; //NOI18N
                    }

                    @Override
                    public boolean skipSearch() {
                        return true;
                    }

                    @Override
                    public AlternativePath[] getAlternativePath() {
                        return new AlternativePath[0];
                    }
                };
            }
            return proxy.getQMake();
        }

        @Override
        public CMakeDescriptor getCMake() {
            if (proxy == null) {
                return new CMakeDescriptor() {

                    @Override
                    public String[] getNames() {
                        return new String[0];
                    }

                    @Override
                    public String getVersionFlags() {
                        return ""; //NOI18N
                    }

                    @Override
                    public String getVersionPattern() {
                        return ""; //NOI18N
                    }

                    @Override
                    public String getFingerPrintFlags() {
                        return ""; //NOI18N
                    }

                    @Override
                    public String getFingerPrintPattern() {
                        return ""; //NOI18N
                    }

                    @Override
                    public boolean skipSearch() {
                        return true;
                    }

                    @Override
                    public AlternativePath[] getAlternativePath() {
                        return new AlternativePath[0];
                    }
                };
            }
            return proxy.getCMake();
        }
    }
}
