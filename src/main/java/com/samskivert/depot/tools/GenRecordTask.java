//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.tools;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

import com.google.common.collect.Lists;

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

        GenRecord genner = new GenRecord(_cloader) {
            protected void logInfo (String msg) {
                log(msg, Project.MSG_VERBOSE);
            }
            protected void logWarn (String msg, Exception e) {
                if (e == null) log(msg, Project.MSG_WARN);
                else log(msg, e, Project.MSG_WARN);
            }
            protected RuntimeException mkFail (String msg, Exception e) {
                return new BuildException(msg, e);
            }
        };

        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (String srcFile : srcFiles) {
                genner.processRecord(new File(fromDir, srcFile));
            }
        }
    }

    /** A list of filesets that contain tile images. */
    protected List<FileSet> _filesets = Lists.newArrayList();

    /** Used to do our own classpath business. */
    protected ClassLoader _cloader;
}
