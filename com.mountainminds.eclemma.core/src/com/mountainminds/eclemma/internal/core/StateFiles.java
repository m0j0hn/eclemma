/*******************************************************************************
 * Copyright (c) 2006 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id$
 ******************************************************************************/
package com.mountainminds.eclemma.internal.core;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.mountainminds.eclemma.core.EclEmmaStatus;

/**
 * Internal utility to manage files in the plugin's state locations.
 * 
 * @author Marc R. Hoffmann
 * @version $Revision$
 */
public class StateFiles {

  private static final String LAUNCHDATA_FOLDER = ".launch/"; //$NON-NLS-1$

  private static final String INSTRDATA_FOLDER = ".instr/"; //$NON-NLS-1$

  private static final ReferenceQueue CLEANUPQUEUE = new ReferenceQueue();
  
  private static final Set CLEANUPFILES = new HashSet();
  

  private final IPath stateLocation;

  public StateFiles(IPath stateLocation) {
    this.stateLocation = stateLocation;
    this.stateLocation.toFile().mkdirs();
    getLaunchDataFolder().toFile().mkdirs();
  }

  public void deleteLaunchFiles() {
    File[] files = getLaunchDataFolder().toFile().listFiles();
    for (int i = 0; i < files.length; i++) {
      files[i].delete();
    }
  }

  public IPath getLaunchDataFolder() {
    return stateLocation.append(LAUNCHDATA_FOLDER);
  }

  public IPath getInstrDataFolder(IPath location) throws CoreException {
    IPath path = stateLocation.append(INSTRDATA_FOLDER).append(
        getInternalId(location.toString()));
    return path;
  }

  private static String getInternalId(String location) throws CoreException {
    StringBuffer sb = new StringBuffer();
    try {
      MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
      md.update(location.getBytes("UTF8")); //$NON-NLS-1$
      byte[] sig = md.digest();
      for (int i = 0; i < sig.length; i++) {
        sb.append(Character.forDigit((sig[i] >> 4) & 0xf, 0x10));
        sb.append(Character.forDigit(sig[i] & 0xf, 0x10));
      }
    } catch (NoSuchAlgorithmException e) {
      throw new CoreException(EclEmmaStatus.ID_CREATION_ERROR.getStatus(e));
    } catch (UnsupportedEncodingException e) {
      throw new CoreException(EclEmmaStatus.ID_CREATION_ERROR.getStatus(e));
    }
    return sb.toString();
  }
  
  /**
   * Registers the file the given path points to for deletion as soon as the
   * reference to the path objects gets garbage collected. The caller must
   * ensure to hold a reference to the given path object as long as the file
   * is required. The file is not required to (jet) actually exist.
   * 
   * @param file  path object that points to the file
   */
  public void registerForCleanup(IPath file) {
    cleanupObsoleteFiles();
    CLEANUPFILES.add(new CleanupFile(file));
  }
  
  private void cleanupObsoleteFiles() {
    while (true) {
      CleanupFile f = (CleanupFile) CLEANUPQUEUE.poll();
      if (f == null) {
        break;
      }
      CLEANUPFILES.remove(f);
      f.delete();
    }
  }

  private static class CleanupFile extends PhantomReference {

    private final File file;

    public CleanupFile(IPath path) {
      super(path, CLEANUPQUEUE);
      this.file = path.toFile();
    }

    public void delete() {
      file.delete();
      clear();
    }

  }

}
