/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.source.indexing;

import com.sun.tools.javac.util.Context;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.processing.Processor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.api.java.queries.AnnotationProcessingQuery;
import org.netbeans.api.java.queries.CompilerOptionsQuery;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.java.queries.SourceLevelQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.java.source.parsing.CachingArchiveClassLoader;
import org.netbeans.modules.java.source.parsing.JavacParser;
import org.netbeans.modules.java.source.usages.LongHashMap;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.modules.parsing.impl.indexing.PathRegistry;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.BaseUtilities;
import org.openide.util.Pair;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jan Lahoda, Dusan Balek, Tomas Zezula
 */
public class APTUtils implements ChangeListener, PropertyChangeListener {

    private static final Logger LOG = Logger.getLogger(APTUtils.class.getName());
    private static final String PROCESSOR_PATH = "processorPath"; //NOI18N
    private static final String BOOT_PATH = "bootPath"; //NOI18N
    private static final String COMPILE_PATH = "compilePath";   //NOI18N
    private static final String APT_ENABLED = "aptEnabled"; //NOI18N
    private static final String APT_DIRTY = "aptDirty"; //NOI18N
    private static final String ANNOTATION_PROCESSORS = "annotationProcessors"; //NOI18N
    private static final String SOURCE_LEVEL_ROOT = "sourceLevel"; //NOI18N
    private static final String JRE_PROFILE = "jreProfile";        //NOI18N
    private static final String COMPILER_OPTIONS = "compilerOptions";   //NOI18N
    private static final int PATH_FLAG_EXISTS = 1;
    private static final Map<URL, APTUtils> knownSourceRootsMap = new HashMap<URL, APTUtils>();
    private static final Map<FileObject, Reference<APTUtils>> auxiliarySourceRootsMap = new WeakHashMap<FileObject, Reference<APTUtils>>();
    private static final Lookup HARDCODED_PROCESSORS = Lookups.forPath("Editors/text/x-java/AnnotationProcessors");
    private static final boolean DISABLE_CLASSLOADER_CACHE = Boolean.getBoolean("java.source.aptutils.disable.classloader.cache");
    private static final int SLIDING_WINDOW = 1000; //1s
    private static final RequestProcessor RP = new RequestProcessor(APTUtils.class);
    private final FileObject root;
    private volatile ClassPath bootPath;
    private volatile ClassPath compilePath;
    private final AtomicReference<ClassPath> processorPath;
    private final AnnotationProcessingQuery.Result aptOptions;
    private final SourceLevelQuery.Result sourceLevel;
    private final CompilerOptionsQuery.Result compilerOptions;
    private final RequestProcessor.Task slidingRefresh;
    private final UsedRoots usedRoots;
    private volatile ClassLoaderRef classLoaderCache;

    private APTUtils(@NonNull final FileObject root) {
        this.root = root;
        bootPath = ClassPath.getClassPath(root, ClassPath.BOOT);
        compilePath = ClassPath.getClassPath(root, ClassPath.COMPILE);
        processorPath = new AtomicReference<>(ClassPath.getClassPath(root, JavaClassPathConstants.PROCESSOR_PATH));
        aptOptions = AnnotationProcessingQuery.getAnnotationProcessingOptions(root);
        sourceLevel = SourceLevelQuery.getSourceLevel2(root);
        compilerOptions = CompilerOptionsQuery.getOptions(root);
        this.slidingRefresh = RP.create(() -> {
            IndexingManager.getDefault().refreshIndex(
                    root.toURL(),
                    Collections.<URL>emptyList(),
                    false);
        });
        usedRoots = new UsedRoots(root.toURL());
    }

    @CheckForNull
    public static APTUtils get(final FileObject root) {
        if (root == null) {
            return null;
        }
        final URL rootUrl = root.toURL();
        if (knownSourceRootsMap.containsKey(rootUrl)) {
            APTUtils utils = knownSourceRootsMap.get(rootUrl);

            if (utils == null) {
                knownSourceRootsMap.put(rootUrl, utils = create(root));
            }

            return utils;
        }

        Reference<APTUtils> utilsRef = auxiliarySourceRootsMap.get(root);
        APTUtils utils = utilsRef != null ? utilsRef.get() : null;
        if (utils != null) {
            return utils;
        }

        if (isAptBuildGeneratedFolder(root)) {
            return null;
        }

        auxiliarySourceRootsMap.put(root, new WeakReference<APTUtils>(utils = create(root)));
        return utils;
    }

    @CheckForNull
    public static APTUtils getIfExist(@NullAllowed final FileObject root) {
        if (root == null) {
            return null;
        }
        final URL rootUrl = root.toURL();
        APTUtils res = knownSourceRootsMap.get(rootUrl);
        if (res != null) {
            return res;
        }
        final Reference<APTUtils> utilsRef = auxiliarySourceRootsMap.get(root);
        res = utilsRef != null ? utilsRef.get() : null;
        return res;
    }

    public static void sourceRootRegistered(FileObject root, URL rootURL) {
        if (   root == null
            || knownSourceRootsMap.containsKey(rootURL)
            //XXX hack unknown roots are also scanned (and consequently registered), but never sourceRootUnregistered(rootsRemoved):
            || PathRegistry.getDefault().getUnknownRoots().contains(rootURL)) {
            return;
        }

        Reference<APTUtils> utilsRef = auxiliarySourceRootsMap.remove(root);
        APTUtils utils = utilsRef != null ? utilsRef.get() : null;

        knownSourceRootsMap.put(rootURL, utils);
    }

    public static void sourceRootUnregistered(Iterable<? extends URL> roots) {
        for (URL root : roots) {
            knownSourceRootsMap.remove(root);
        }
        //XXX hack make sure we are not holding APTUtils for any unknown roots
        //just in case something goes wrong:
        for (URL unknown : PathRegistry.getDefault().getUnknownRoots()) {
            knownSourceRootsMap.remove(unknown);
        }
        final Project[] projects = OpenProjects.getDefault().getOpenProjects();
        if (projects.length == 0 && !knownSourceRootsMap.isEmpty()) {
            LOG.log(
                Level.WARNING,
                "Non removed known roots: {0}",  //NOI18N
                knownSourceRootsMap.keySet());
            knownSourceRootsMap.clear();
        }
    }

    @NonNull
    private static APTUtils create(FileObject root) {
        final APTUtils utils = new APTUtils(root);
        utils.listen();
        return utils;
    }

    private static boolean isAptBuildGeneratedFolder(@NonNull final FileObject root) {
        final ClassPath scp = ClassPath.getClassPath(root, ClassPath.SOURCE);
        if (scp != null) {
            for (FileObject srcRoot : scp.getRoots()) {
                if (root.toURL().equals(
                        AnnotationProcessingQuery.getAnnotationProcessingOptions(srcRoot).sourceOutputDirectory())) {
                   return true;
                }
            }
        }
        return false;
    }

    public boolean aptEnabledOnScan() {
        return aptOptions.annotationProcessingEnabled().contains(AnnotationProcessingQuery.Trigger.ON_SCAN);
    }

    public boolean aptEnabledInEditor() {
        return aptOptions.annotationProcessingEnabled().contains(AnnotationProcessingQuery.Trigger.IN_EDITOR);
    }

    @CheckForNull
    public URL sourceOutputDirectory() {
        return aptOptions.sourceOutputDirectory();
    }

    public Collection<? extends Processor> resolveProcessors(boolean onScan) {
        ClassPath pp = validatePaths();
        ClassLoader cl;
        final ClassLoaderRef cache = classLoaderCache;
        if (cache == null || (cl=cache.get(root)) == null) {
            if (pp == null) {
                pp = ClassPath.EMPTY;
            }
            cl = CachingArchiveClassLoader.forClassPath(
                    pp,
                    new BypassOpenIDEUtilClassLoader(Context.class.getClassLoader()),
                    usedRoots);
            classLoaderCache = !DISABLE_CLASSLOADER_CACHE ? new ClassLoaderRef(cl, root) : null;
        }
        Collection<Processor> result = lookupProcessors(cl, onScan);
        return result;
    }

    public Map<? extends String, ? extends String> processorOptions() {
        return aptOptions.processorOptions();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (verifyAttributes(root, true)) {
            slidingRefresh.schedule(SLIDING_WINDOW);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (ClassPath.PROP_ROOTS.equals(evt.getPropertyName())) {
            classLoaderCache = null;
            if (verifyProcessorPath(root, usedRoots)) {
                slidingRefresh.schedule(SLIDING_WINDOW);
            }
        }
    }

    private void listen() {
        listenOnProcessorPath(processorPath.get(), this);
        aptOptions.addChangeListener(WeakListeners.change(this, aptOptions));
        if (sourceLevel.supportsChanges()) {
            sourceLevel.addChangeListener(WeakListeners.change(this, sourceLevel));
        }
        compilerOptions.addChangeListener(WeakListeners.change(this,compilerOptions));
    }
    
    private static void listenOnProcessorPath(
            @NullAllowed final ClassPath cp,
            @NonNull final APTUtils target) {
        if (cp != null) {
            cp.addPropertyChangeListener(WeakListeners.propertyChange(target, cp));
            cp.getRoots();//so that the ClassPath starts listening on the filesystem
        }
    }

    @CheckForNull
    private ClassPath validatePaths() {
        ClassPath pp = processorPath.get();
        if (pp != null) {
            return pp;
        }
        pp = ClassPath.getClassPath(root, JavaClassPathConstants.PROCESSOR_PATH);
        if (pp != null && processorPath.compareAndSet(null, pp)) {
            bootPath = ClassPath.getClassPath(root, ClassPath.BOOT);
            compilePath = ClassPath.getClassPath(root, ClassPath.COMPILE);
            listenOnProcessorPath(pp, this);
            classLoaderCache = null;
        }
        return pp;
    }

    private Collection<Processor> lookupProcessors(ClassLoader cl, boolean onScan) {
        Iterable<? extends String> processorNames = aptOptions.annotationProcessorsToRun();
        if (processorNames == null) {
            processorNames = getProcessorNames(cl);
        }
        List<Processor> result = new LinkedList<Processor>();
        for (String name : processorNames) {
            try {
                Class<?> clazz = Class.forName(name, true, cl);
                Object instance = clazz.newInstance();
                if (instance instanceof Processor) {
                    result.add((Processor) instance);
                }
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {
                LOG.log(Level.FINE, null, t);
            }
        }
        if (!onScan)
            result.addAll(HARDCODED_PROCESSORS.lookupAll(Processor.class));
        return result;
    }

    @NonNull
    private Iterable<? extends String> getProcessorNames(@NonNull final ClassLoader cl) {
        try {
            return CachingArchiveClassLoader.readAction(() -> {
                Collection<String> result = new LinkedList<>();
                Enumeration<URL> resources = cl.getResources("META-INF/services/" + Processor.class.getName()); //NOI18N
                while (resources.hasMoreElements()) {
                    BufferedReader ins = null;
                    try {
                        final URLConnection uc = resources.nextElement().openConnection();
                        uc.setUseCaches(false);
                        ins = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8")); //NOI18N
                        String line;
                        while ((line = ins.readLine()) != null) {
                            int hash = line.indexOf('#');
                            line = hash != (-1) ? line.substring(0, hash) : line;
                            line = line.trim();
                            if (line.length() > 0) {
                                result.add(line);
                            }
                        }
                    } catch (IOException ex) {
                        LOG.log(Level.FINE, null, ex);
                    } finally {
                        if (ins != null) {
                            ins.close();
                        }
                    }
                }
                return result;
            });
        }  catch (Exception ex) {
            LOG.log(Level.FINE, null, ex);
            return Collections.emptySet();
        }
    }

    boolean verifyAttributes(FileObject fo, boolean checkOnly) {
        if (fo == null)
            return false;
        boolean vote = false;
        try {
            final ClassPath pp = validatePaths();
            final URL url = fo.toURL();
            String val = JavaIndex.getAttribute(url, APT_DIRTY, null);
            if (Boolean.parseBoolean(val)) {
                JavaIndex.LOG.fine("forcing reindex due to processors dirty"); //NOI18N
                vote = true;
                if (checkOnly) {
                    return vote;
                }
                JavaIndex.setAttribute(url, APT_DIRTY, null);
            }
            if (JavaIndex.ensureAttributeValue(url, SOURCE_LEVEL_ROOT, sourceLevel.getSourceLevel(), checkOnly)) {
                JavaIndex.LOG.fine("forcing reindex due to source level change"); //NOI18N
                vote = true;
                if (checkOnly) {
                    return vote;
                }
            }            
            if (JavaIndex.ensureAttributeValue(url, JRE_PROFILE, sourceLevel.getProfile().getName(), checkOnly)) {
                JavaIndex.LOG.fine("forcing reindex due to jre profile change"); //NOI18N
                vote = true;
                if (checkOnly) {
                    return vote;
                }
            }
            final String cmpOptsStr = JavacParser.validateCompilerOptions(compilerOptions.getArguments())
                    .stream()
                    .collect(Collectors.joining(" "));  //NOI18N
            if (JavaIndex.ensureAttributeValue(url, COMPILER_OPTIONS, cmpOptsStr, checkOnly)) {
                JavaIndex.LOG.fine("forcing reindex due to compiler options change"); //NOI18N
                vote = true;
                if (checkOnly) {
                    return vote;
                }
            }
            if (JavaIndex.ensureAttributeValue(url, BOOT_PATH, pathToString(bootPath), checkOnly)) {
                JavaIndex.LOG.fine("forcing reindex due to boot path change"); //NOI18N
                vote = true;
                if (checkOnly) {
                    return vote;
                }
            }
            if (JavaIndex.ensureAttributeValue(url, COMPILE_PATH, pathToString(compilePath), checkOnly)) {
                JavaIndex.LOG.fine("forcing reindex due to compile path change"); //NOI18N
                vote = true;
                if (checkOnly) {
                    return vote;
                }
            }
            boolean apEnabledOnScan = aptOptions.annotationProcessingEnabled().contains(AnnotationProcessingQuery.Trigger.ON_SCAN);
            if (JavaIndex.ensureAttributeValue(url, APT_ENABLED, apEnabledOnScan ? Boolean.TRUE.toString() : null, checkOnly)) {
                JavaIndex.LOG.fine("forcing reindex due to change in annotation processing options"); //NOI18N
                vote = true;
                if (checkOnly) {
                    return vote;
                }
            }
            if (!apEnabledOnScan) {
                //no need to check further:
                return vote;
            }
            if (JavaIndex.ensureAttributeValue(url, PROCESSOR_PATH, pathToFlaggedString(pp, false), checkOnly)) {
                JavaIndex.LOG.fine("forcing reindex due to processor path change"); //NOI18N
                vote = true;
                if (checkOnly) {
                    return vote;
                }
            }
            if (JavaIndex.ensureAttributeValue(url, ANNOTATION_PROCESSORS, encodeToStirng(aptOptions.annotationProcessorsToRun()), checkOnly)) {
                JavaIndex.LOG.fine("forcing reindex due to change in annotation processors"); //NOI18N
                vote = true;
                if (checkOnly) {
                    return vote;
                }
            }
        } catch (IOException ioe) {
            Exceptions.printStackTrace(ioe);
        }
        return vote;
    }

    private boolean verifyProcessorPath(
        @NonNull final FileObject root,
        @NonNull final UsedRoots usedRoots) {
        try {
            final LongHashMap<? extends File> used = usedRoots.getRoots();
            final URL url = root.toURL();
            final ClassPath pp = validatePaths();
            final Set<File> currentExitingFiles = pp.entries().stream()
                    .map((e) -> e.getRoot())
                    .filter((fo) -> fo != null && fo.isValid())
                    .map((fo) -> FileUtil.isArchiveArtifact(fo) ? FileUtil.getArchiveFile(fo) : fo)
                    .filter((fo) -> fo != null && fo.isValid())
                    .map(FileUtil::toFile)
                    .filter((file) -> file != null)
                    .collect(Collectors.toSet());
            final Set<File> currentAllFiles = pp.entries().stream()
                    .map((e) -> FileUtil.archiveOrDirForURL(e.getURL()))
                    .filter((file) -> file != null)
                    .collect(Collectors.toSet());

            final Set<File> oldExistingFiles = new HashSet<>();
            final Set<File> oldAllFiles = new HashSet<>();
            final String raw = JavaIndex.getAttribute(url, PROCESSOR_PATH, ""); //NOI18N
            if (!raw.isEmpty()) {
                final String[] parts = raw.split(File.pathSeparator);
                for (int i=0; i < parts.length; i+=2) {
                    final File f = new File(parts[i]);
                    int flags = PATH_FLAG_EXISTS;
                    try {
                        if (i+1 < parts.length) {
                            flags = Integer.parseInt(parts[i+1]);
                        }
                    } catch (NumberFormatException nfe) {
                        //pass with PATH_FLAG_USED set
                    }
                    oldAllFiles.add(f);
                    if ((flags & PATH_FLAG_EXISTS) == PATH_FLAG_EXISTS) {
                        oldExistingFiles.add(f);
                    }
                }
            }
            Set<File> added = new HashSet<>(currentExitingFiles);
            added.removeAll(oldExistingFiles);
            Set<File> removed = new HashSet<>(oldExistingFiles);
            removed.removeAll(currentExitingFiles);
            LOG.log(Level.FINEST, "Added: {0}", added);     //NOI18N
            LOG.log(Level.FINEST, "Removed: {0}", removed); //NOI18N
            if (!added.isEmpty() || !removed.isEmpty()) {
                JavaIndex.setAttribute(url, PROCESSOR_PATH, pathToFlaggedString(pp, true));
            }
            boolean res = false;

            for (Iterator<File> it = removed.iterator(); it.hasNext();) {
                File f = it.next();
                if (!currentAllFiles.contains(f)) {
                    LOG.log(Level.FINEST, "Removed from path: {0}", f);  //NOI18N
                    it.remove();
                    usedRoots.remove(f);
                    res = true;
                }
            }
            final Predicate<File> pUsed = (f) -> used == null || used.containsKey(f);
            final Predicate<Pair<File,Long>> pModified = (p) -> used == null || p.second() == -1 || used.get(p.first()) != p.second();
            final Predicate<File> pNotProjDep = (f) -> {
                final URL furl = FileUtil.urlForArchiveOrDir(f);
                return furl == null ?
                        true :
                        !hasSourceCache(furl);
            };
            removed = removed.stream()
                    .filter(pUsed)
                    .filter(pNotProjDep)
                    .collect(Collectors.toSet());
            if (!removed.isEmpty()) {
                LOG.log(Level.FINEST, "Important deleted: {0}", removed);    //NOI18N
                res = true;
            }

            for (Iterator<File> it = added.iterator(); it.hasNext();) {
                final File f = it.next();
                if(!oldAllFiles.contains(f)) {
                    LOG.log(Level.FINEST, "Added to path: {0}", f);  //NOI18N
                    it.remove();
                    res = true;
                }
            }
            final Collection<Pair<File,Long>> times = added.stream()
                    .filter(pUsed)
                    .map((f) -> Pair.of(f, f.isFile() ? f.length() : -1))
                    .filter(pModified)
                    .collect(Collectors.toList());
            if (!times.isEmpty()) {
                LOG.log(Level.FINEST, "Important changed: {0}", times);    //NOI18N
                for (Pair<File,Long> p : times) {
                    usedRoots.update(p.first(), p.second());
                }
                res = true;
            }
            if (res) {
                JavaIndex.setAttribute(url, APT_DIRTY, Boolean.TRUE.toString());
                JavaIndex.LOG.fine("forcing reindex due to processor path change"); //NOI18N
            }
            return res;
        } catch (IOException ioe) {
            Exceptions.printStackTrace(ioe);
            return false;
        }
    }

    @NonNull
    private static String pathToString(@NullAllowed ClassPath cp) {
        final StringBuilder b = new StringBuilder();
        if (cp == null) {
            cp = ClassPath.EMPTY;
        }
        for (final ClassPath.Entry cpe : cp.entries()) {
            final FileObject fo = cpe.getRoot();
            if (fo != null) {
                final URL u = fo.toURL();
                append(b, u);
            } else if (hasSourceCache(cpe.getURL())) {
                final URL u = cpe.getURL();
                append(b,u);
            }
        }
        return b.toString();
    }

    /**
     * Translated classpath to String (ENTRY PATH_SEPARATOR FLAGS) (PATH_SEPARATOR ENTRY PATH_SEPARATOR FLAGS)*
     * @param cp the classpath to translate
     * @return the translated classpath
     */
    @NonNull
    private static String pathToFlaggedString(
            @NullAllowed ClassPath cp,
            final boolean checkArchiveFile) {
        final StringBuilder b = new StringBuilder();
        if (cp == null) {
            cp = ClassPath.EMPTY;
        }
        for (final ClassPath.Entry cpe : cp.entries()) {
            //Entry.getRoot() returns root which is valid for deleted jar
            //in ClassPath.PROP_ROOTS event
            final boolean exists = Optional.ofNullable(cpe.getRoot())
                    .map((fo) -> (checkArchiveFile && FileUtil.isArchiveArtifact(fo)) ? FileUtil.getArchiveFile(fo) : fo)
                    .map((fo) -> checkArchiveFile ? fo.isValid() : true)
                    .orElse(Boolean.FALSE);
            append(b,cpe.getURL())
                    .append(File.pathSeparatorChar)
                    .append(exists ? PATH_FLAG_EXISTS : 0);
        }
        return b.toString();
    }

    private static String encodeToStirng(Iterable<? extends String> strings) {
        if (strings == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (Iterator it = strings.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext())
                sb.append(',');
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    @NonNull
    private static StringBuilder append (
            @NonNull final StringBuilder builder,
            @NonNull final URL url) {
        final File f = FileUtil.archiveOrDirForURL(url);
        if (f != null) {
            if (builder.length() > 0) {
                builder.append(File.pathSeparatorChar);
            }
            builder.append(f.getAbsolutePath());
        } else {
            if (builder.length() > 0) {
                builder.append(File.pathSeparatorChar);
            }
            builder.append(url);
        }
        return builder;
    }

    private static boolean hasSourceCache(@NonNull final URL root) {
        return SourceForBinaryQuery.findSourceRoots2(root).preferSources();
    }

    //keep synchronized with libs.javacapi/manifest.mf and libs.javacimpl/manifest.mf
    //when adding new packages, double-check the quick path in loadClass below:
    private static final Iterable<? extends String> javacPackages = Arrays.asList("com.sun.javadoc.", "com.sun.source.", "javax.annotation.processing.", "javax.lang.model.", "javax.tools.", "com.sun.tools.javac.", "com.sun.tools.javadoc.", "com.sun.tools.classfile.", "com.sun.tools.hc.");
    private static final class BypassOpenIDEUtilClassLoader extends ClassLoader {
        private final ClassLoader contextCL;
        public BypassOpenIDEUtilClassLoader(ClassLoader contextCL) {
            super(getSystemClassLoader().getParent());
            this.contextCL = contextCL;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            //the 5-th letter of all interesting packages is either 's' or 'x'
            //using that to prevent (possibly expensive) loop through javacPackages:
            char f = name.length() > 4 ? name.charAt(4) : '\0';

            if (f == 'x' || f == 's') {
                for (String pack : javacPackages) {
                    if (name.startsWith(pack)) {
                        return contextCL.loadClass(name);
                    }
                }
            }
            
            return super.loadClass(name, resolve);
        }

        //getResource and getResources of module classloaders do not return resources from parent's META-INF, so no need to override them
    }

    private static final class ClassLoaderRef extends SoftReference<ClassLoader> implements Runnable {

        private final long timeStamp;
        private final String rootPath;

        public ClassLoaderRef(final ClassLoader cl, final FileObject root) {
            super(cl, BaseUtilities.activeReferenceQueue());
            this.timeStamp = getTimeStamp(root);
            this.rootPath = FileUtil.getFileDisplayName(root);
            LOG.log(Level.FINER, "ClassLoader for root {0} created.", new Object[]{rootPath});  //NOI18N
        }

        public ClassLoader get(final FileObject root) {
            final long curTimeStamp = getTimeStamp(root);
            return curTimeStamp == timeStamp ? get() : null;
        }

        @Override
        public void run() {
            LOG.log(Level.FINER, "ClassLoader for root {0} freed.", new Object[] {rootPath});   //NOI18N
        }

        private static long getTimeStamp(final FileObject root) {
            final FileObject archiveFile = FileUtil.getArchiveFile(root);
            return archiveFile != null ? root.lastModified().getTime() : -1L;
        }
    }

    private static final class UsedRoots implements Consumer<URL> {
        private static final String ATTR_USED_PROC = "apUsed";  //NOI18N
        private static final int DEFERRED_SAVE = 2_500;    //ms
        private static final LongHashMap<File> TOMBSTONE = new LongHashMap<>();
        private static final RequestProcessor SAVER = new RequestProcessor(
                UsedRoots.class.getName(),
                1,
                false,
                false);
        private final URL root;
        private final RequestProcessor.Task saveTask;
        //@GuardedBy("this")
        private LongHashMap<File> used;

        UsedRoots(@NonNull final URL root) {
            this.root = root;
            this.saveTask = SAVER.create(()->save());
        }

        @Override
        public void accept(@NonNull final URL url) {
            synchronized (this) {
                load();
                if (used == null || used == TOMBSTONE) {
                    used = new LongHashMap<>();
                }
                final File f = FileUtil.archiveOrDirForURL(url);
                if (f != null) {
                    final long size = f.isFile() ?
                            f.length() :
                            -1;
                    if (!used.containsKey(f)) {
                        used.put(f, size);
                        saveTask.schedule(DEFERRED_SAVE);
                    }
                }
            }
        }

        void update(
                @NonNull final File file,
                final long size) {
            synchronized (this) {
                load();
                if (used == null || used == TOMBSTONE) {
                    used = new LongHashMap<>();
                }
                if (used.put(file, size) != size) {
                    saveTask.schedule(DEFERRED_SAVE);
                }
            }
        }

        void remove(@NonNull final File file) {
            synchronized (this) {
                load();
                if (used == null || used == TOMBSTONE) {
                    return;
                }
                if (used.remove(file) != LongHashMap.NO_VALUE) {
                    saveTask.schedule(DEFERRED_SAVE);
                }
            }
        }

        void reset() {
            synchronized (this) {
                used = TOMBSTONE;
            }
        }

        @CheckForNull
        LongHashMap<? extends File> getRoots() {
            final LongHashMap<File> res = load();
            return res == TOMBSTONE ?
                    null :
                    res;
        }

        @NonNull
        private synchronized LongHashMap<File> load() {
            if (used == null) {
                try {
                    final String raw = JavaIndex.getAttribute(
                            root,
                            ATTR_USED_PROC,
                            null);
                    if (raw != null && !raw.isEmpty()) {
                        LongHashMap<File> s = new LongHashMap<>();
                        final String[] parts = raw.split(File.pathSeparator);
                        for (int i = 0; i < parts.length; i+=2) {
                            final File f = new File(parts[i]);
                            long size = -1;
                            if (i+1 < parts.length) {
                                try {
                                    size = Long.parseLong(parts[i+1]);
                                } catch (NumberFormatException e) {
                                    LOG.log(
                                            parts[i+1].isEmpty() ?
                                                Level.FINE :
                                                Level.WARNING,
                                            "Wrong size of {0}: {1}",   //NOI18N
                                            new Object[]{
                                                f.getAbsolutePath(),
                                                parts[i+1]
                                            });
                                }
                            }
                            s.put(f, size);
                        }
                        used = s;
                    } else {
                        used = TOMBSTONE;
                    }
                } catch (IOException ioe) {
                    Exceptions.printStackTrace(ioe);
                    used = TOMBSTONE;
                }
            }
            assert used != null;
            return used;
        }

        private void save() {
            final LongHashMap<File> toSave;
            synchronized (this) {
                toSave = used == TOMBSTONE ?
                        null :
                        new LongHashMap<File>(used);
            }
            final String val;
            if (toSave == null) {
                val = null;
            } else {
                final StringBuilder raw = new StringBuilder();
                for (LongHashMap.Entry<File> e : toSave.entrySet()) {
                    if (raw.length() != 0) {
                        raw.append(File.pathSeparatorChar);
                    }
                    raw.append(e.getKey().getAbsolutePath());
                    raw.append(File.pathSeparator);
                    raw.append(e.getValue());
                }
                val = raw.toString();
            }
            try {
                JavaIndex.setAttribute(
                        root,
                        ATTR_USED_PROC,
                        val);
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            } finally {
                synchronized (this) {
                    if (used == TOMBSTONE) {
                        used = null;
                    }
                }
            }
        }
    }
}