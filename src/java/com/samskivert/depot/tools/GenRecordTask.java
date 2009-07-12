//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and Pär Winzell
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.depot.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.ClassUtil;
import com.samskivert.util.GenUtil;
import com.samskivert.util.StringUtil;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Transient;
import com.samskivert.depot.impl.DepotUtil;

/**
 * An ant task that updates the column constants for a persistent record.
 */
public class GenRecordTask extends Task
{
    /**
     * Adds a nested fileset element which enumerates record source files.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    /**
     * Configures that classpath that we'll use to load record classes.
     */
    public void setClasspathref (Reference pathref)
    {
        _cloader = ClasspathUtils.getClassLoaderForPath(getProject(), pathref);
    }

    @Override
    public void execute () throws BuildException
    {
        if (_cloader == null) {
            String errmsg = "This task requires a 'classpathref' attribute " +
                "to be set to the project's classpath.";
            throw new BuildException(errmsg);
        }

        // resolve the PersistentRecord class using our classloader
        try {
            _prclass = _cloader.loadClass(PersistentRecord.class.getName());
        } catch (Exception e) {
            throw new BuildException("Can't resolve InvocationListener", e);
        }

        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (int f = 0; f < srcFiles.length; f++) {
                processRecord(new File(fromDir, srcFiles[f]));
            }
        }
    }

    /**
     * Processes a distributed object source file.
     */
    protected void processRecord (File source)
    {
        // System.err.println("Processing " + source + "...");

        // load up the file and determine it's package and classname
        String name = null;
        try {
            name = readClassName(source);
        } catch (Exception e) {
            System.err.println("Failed to parse " + source + ": " + e.getMessage());
            return;
        }

        try {
            processRecord(source, _cloader.loadClass(name));
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Failed to load " + name + ".\n" +
                               "Missing class: " + cnfe.getMessage());
            System.err.println("Be sure to set the 'classpathref' attribute to a classpath\n" +
                               "that contains your projects invocation service classes.");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    /** Processes a resolved persistent record class instance. */
    protected void processRecord (File source, Class<?> rclass)
    {
        // make sure we extend persistent record
        if (!_prclass.isAssignableFrom(rclass)) {
            // System.err.println("Skipping " + rclass.getName() + "...");
            return;
        }
        boolean isAbstract = Modifier.isAbstract(rclass.getModifiers());

        // determine our primary key fields for getKey() generation (if we're not an abstract)
        List<Field> kflist = Lists.newArrayList();
        if (!isAbstract) {
            // determine which fields make up our primary key; we'd just use Class.getFields() but
            // that returns things in a random order whereas ClassUtil returns fields in
            // declaration order starting from the top-most class and going down the line
            for (Field field : ClassUtil.getFields(rclass)) {
                if (hasAnnotation(field, Id.class)) {
                    kflist.add(field);
                    continue;
                }
            }
        }

        // determine which fields we need to generate constants for
        List<Field> flist = Lists.newArrayList();
        for (Field field : rclass.getFields()) {
            if (isPersistentField(field)) {
                flist.add(field);
            }
        }
        Set<Field> declared = Sets.newHashSet();
        for (Field field : rclass.getDeclaredFields()) {
            if (isPersistentField(field)) {
                declared.add(field);
            }
        }

        // slurp our source file into newline separated strings
        String[] lines = null;
        try {
            BufferedReader bin = new BufferedReader(new FileReader(source));
            List<String> llist = Lists.newArrayList();
            String line = null;
            while ((line = bin.readLine()) != null) {
                llist.add(line);
            }
            lines = llist.toArray(new String[llist.size()]);
            bin.close();
        } catch (IOException ioe) {
            System.err.println("Error reading '" + source + "': " + ioe);
            return;
        }

        // now determine where to insert our static field declarations
        int bstart = -1, bend = -1;
        int nstart = -1, nend = -1;
        int mstart = -1, mend = -1;
        for (int ii = 0; ii < lines.length; ii++) {
            String line = lines[ii].trim();

            // look for the start of the class body
            if (NAME_PATTERN.matcher(line).find()) {
                if (line.endsWith("{")) {
                    bstart = ii+1;
                } else {
                    // search down a few lines for the open brace
                    for (int oo = 1; oo < 10; oo++) {
                        if (get(lines, ii+oo).trim().endsWith("{")) {
                            bstart = ii+oo+1;
                            break;
                        }
                    }
                }

            // track the last } on a line by itself and we'll call that the end of the class body
            } else if (line.equals("}")) {
                bend = ii;

            // look for our field and method markers
            } else if (line.equals(FIELDS_START)) {
                nstart = ii;
            } else if (line.equals(FIELDS_END)) {
                nend = ii+1;
            } else if (line.equals(METHODS_START)) {
                mstart = ii;
            } else if (line.equals(METHODS_END)) {
                mend = ii+1;
            }
        }

        // sanity check the markers
        if (check(source, "fields start", nstart, "fields end", nend) ||
            check(source, "fields end", nend, "fields start", nstart) ||
            check(source, "methods start", mstart, "methods end", mend) ||
            check(source, "methods end", mend, "methods start", mstart)) {
            return;
        }

        // we have no previous markers then stuff the fields at the top of the class body and the
        // methods at the bottom
        if (nstart == -1) {
            nstart = bstart;
            nend = bstart;
        }
        if (mstart == -1) {
            mstart = bend;
            mend = bend;
        }

        // get the unqualified class name
        String rname = DepotUtil.justClassName(rclass);

        // generate our fields section
        StringBuilder fsection = new StringBuilder();

        // add our prototype declaration
        Map<String, String> subs = Maps.newHashMap();
        subs.put("record", rname);
        fsection.append(mergeTemplate(PROTO_TMPL, subs));

        // add our ColumnExp constants
        for (int ii = 0; ii < flist.size(); ii++) {
            Field f = flist.get(ii);
            String fname = f.getName();

            // create our substitution mappings
            Map<String, String> fsubs = Maps.newHashMap(subs);
            fsubs.put("field", fname);
            fsubs.put("capfield", StringUtil.unStudlyName(fname).toUpperCase());

            // now generate our bits
            fsection.append(mergeTemplate(COL_TMPL, fsubs));
        }

        // generate our methods section
        StringBuilder msection = new StringBuilder();

        // add a getKey() method, if applicable
        if (kflist.size() > 0) {
            StringBuilder argList = new StringBuilder();
            StringBuilder argNameList = new StringBuilder();
            StringBuilder fieldNameList = new StringBuilder();
            for (Field keyField : kflist) {
                if (argList.length() > 0) {
                    argList.append(", ");
                    argNameList.append(", ");
                    fieldNameList.append(", ");
                }
                String name = keyField.getName();
                argList.append(GenUtil.simpleName(keyField)).append(" ").append(name);
                argNameList.append(name);
                fieldNameList.append(StringUtil.unStudlyName(name));
            }

            subs.put("argList", argList.toString());
            subs.put("argNameList", argNameList.toString());
            subs.put("fieldNameList", fieldNameList.toString());

            // generate our bits and append them as appropriate to the string buffers
            msection.append(mergeTemplate(KEY_TMPL, subs));
        }

        // now bolt everything back together into a class declaration
        try {
            BufferedWriter bout = new BufferedWriter(new FileWriter(source));
            for (int ii = 0; ii < nstart; ii++) {
                writeln(bout, lines[ii]);
            }

            if (fsection.length() > 0) {
                String prev = get(lines, nstart-1);
                if (!StringUtil.isBlank(prev) && !prev.equals("{")) {
                    bout.newLine();
                }
                writeln(bout, "    " + FIELDS_START);
                bout.write(fsection.toString());
                writeln(bout, "    " + FIELDS_END);
                if (!StringUtil.isBlank(get(lines, nend))) {
                    bout.newLine();
                }
            }
            for (int ii = nend; ii < mstart; ii++) {
                writeln(bout, lines[ii]);
            }

            if (msection.length() > 0) {
                if (!StringUtil.isBlank(get(lines, mstart-1))) {
                    bout.newLine();
                }
                writeln(bout, "    " + METHODS_START);
                bout.write(msection.toString());
                writeln(bout, "    " + METHODS_END);
                String next = get(lines, mend);
                if (!StringUtil.isBlank(next) && !next.equals("}")) {
                    bout.newLine();
                }
            }
            for (int ii = mend; ii < lines.length; ii++) {
                writeln(bout, lines[ii]);
            }

            bout.close();
        } catch (IOException ioe) {
            System.err.println("Error writing to '" + source + "': " + ioe);
        }
    }

    /**
     * Returns true if the supplied field is part of a persistent record (is a public, non-static,
     * non-transient field).
     */
    protected boolean isPersistentField (Field field)
    {
        int mods = field.getModifiers();
        return Modifier.isPublic(mods) && !Modifier.isStatic(mods) &&
            !Modifier.isTransient(mods) && !hasAnnotation(field, Transient.class);
    }

    /**
     * Safely gets the <code>index</code>th line, returning the empty string if we exceed the
     * length of the array.
     */
    protected String get (String[] lines, int index)
    {
        return (index < lines.length) ? lines[index] : "";
    }

    /** Helper function for sanity checking marker existence. */
    protected boolean check (File source, String mname, int mline, String fname, int fline)
    {
        if (mline == -1 && fline != -1) {
            System.err.println("Found " + fname + " marker (at line " + (fline+1) + ") but no " +
                               mname + " marker in '" + source + "'.");
            return true;
        }
        return false;
    }

    /** Helper function for writing a string and a newline to a writer. */
    protected void writeln (BufferedWriter bout, String line)
        throws IOException
    {
        bout.write(line);
        bout.newLine();
    }

    /** Helper function for generating our boilerplate code. */
    protected String mergeTemplate (String tmpl, Map<String, String> subs)
    {
        try {
            String text = StreamUtil.toString(
                getClass().getClassLoader().getResourceAsStream(tmpl), "UTF-8");
            for (Map.Entry<String, String> entry : subs.entrySet()) {
                text = text.replaceAll("@"+entry.getKey()+"@", entry.getValue());
            }
            return text;
        } catch (Exception e) {
            throw new BuildException("Failed processing template [tmpl=" + tmpl + "]", e);
        }
    }

    protected static boolean hasAnnotation (Field field, Class<?> annotation)
    {
        // iterate becase getAnnotation() fails if we're dealing with multiple classloaders
        for (Annotation a : field.getAnnotations()) {
            if (annotation.getName().equals(a.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads in the supplied source file and locates the package and class or interface name and
     * returns a fully qualified class name.
     */
    protected static String readClassName (File source)
        throws IOException
    {
        // load up the file and determine it's package and classname
        String pkgname = null, name = null;
        BufferedReader bin = new BufferedReader(new FileReader(source));
        String line;
        while ((line = bin.readLine()) != null) {
            Matcher pm = PACKAGE_PATTERN.matcher(line);
            if (pm.find()) {
                pkgname = pm.group(1);
            }
            Matcher nm = NAME_PATTERN.matcher(line);
            if (nm.find()) {
                name = nm.group(2);
                break;
            }
        }
        bin.close();

        // make sure we found something
        if (name == null) {
            throw new IOException("Unable to locate class or interface name in " + source + ".");
        }

        // prepend the package name to get a name we can Class.forName()
        if (pkgname != null) {
            name = pkgname + "." + name;
        }

        return name;
    }

    /** A list of filesets that contain tile images. */
    protected List<FileSet> _filesets = Lists.newArrayList();

    /** Used to do our own classpath business. */
    protected ClassLoader _cloader;

    /** {@link PersistentRecord} resolved with the proper classloader so that we can compare it to
     * loaded derived classes. */
    protected Class<?> _prclass;

    /** Specifies the path to the name code template. */
    protected static final String PROTO_TMPL = "com/samskivert/depot/tools/record_proto.tmpl";

    /** Specifies the path to the column code template. */
    protected static final String COL_TMPL = "com/samskivert/depot/tools/record_column.tmpl";

    /** Specifies the path to the key code template. */
    protected static final String KEY_TMPL = "com/samskivert/depot/tools/record_key.tmpl";

    // markers
    protected static final String MARKER = "// AUTO-GENERATED: ";
    protected static final String FIELDS_START = MARKER + "FIELDS START";
    protected static final String FIELDS_END = MARKER + "FIELDS END";
    protected static final String METHODS_START = MARKER + "METHODS START";
    protected static final String METHODS_END = MARKER + "METHODS END";

    /** A regular expression for matching the package declaration. */
    protected static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+(\\S+)\\W");

    /** A regular expression for matching the class or interface declaration. */
    protected static final Pattern NAME_PATTERN = Pattern.compile(
        "^\\s*public\\s+(?:abstract\\s+)?(interface|class)\\s+(\\w+)(\\W|$)");
}
