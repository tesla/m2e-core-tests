
package org.maven.ide.eclipse.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;


public class FileHelpers {

  public static void copyDir(File src, File dst) throws IOException {
    copyDir(src, dst, new FileFilter() {
      public boolean accept(File pathname) {
        return !".svn".equals(pathname.getName());
      }
    });
  }

  public static void copyDir(File src, File dst, FileFilter filter) throws IOException {
    copyDir(src, dst, filter, true);
  }

  private static void copyDir(File src, File dst, FileFilter filter, boolean deleteDst) throws IOException {
    if(deleteDst) {
      FileUtils.deleteDirectory(dst);
    }
    dst.mkdirs();
    File[] files = src.listFiles(filter);
    if(files != null) {
      for(int i = 0; i < files.length; i++ ) {
        File file = files[i];
        if(file.canRead()) {
          File dstChild = new File(dst, file.getName());
          if(file.isDirectory()) {
            copyDir(file, dstChild, filter, false);
          } else {
            copyFile(file, dstChild);
          }
        }
      }
    }
  }

  private static void copyFile(File src, File dst) throws IOException {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dst));

    byte[] buf = new byte[10240];
    int len;
    while((len = in.read(buf)) != -1) {
      out.write(buf, 0, len);
    }

    out.close();
    in.close();
  }

}
