package com.tyron.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;

public class IOUtils {

  public static void writeAndClose(String content, File file) {
    try {
      FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
