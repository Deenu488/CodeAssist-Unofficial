package com.tyron.builder.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class ModuleParser {

  private final File root;

  public ModuleParser(File root) {
    this.root = root;
  }

  public String parse() throws IOException, JSONException {
    File module = new File(root, "module.json");
    if (!module.exists()) {
      return "Unknown";
    }

    String contents = FileUtils.readFileToString(module, StandardCharsets.UTF_8);
    JSONObject jsonObject = new JSONObject(contents);
    return jsonObject.getString("type");
  }
}
