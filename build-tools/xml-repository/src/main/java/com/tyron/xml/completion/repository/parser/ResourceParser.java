package com.tyron.xml.completion.repository.parser;

import com.tyron.xml.completion.repository.api.ResourceNamespace;
import com.tyron.xml.completion.repository.api.ResourceValue;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ResourceParser {

  default List<ResourceValue> parse(
      @NotNull File file, @NotNull ResourceNamespace namespace, @Nullable String libraryName)
      throws IOException {
    String contents = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    return parse(file, contents, namespace, libraryName);
  }

  List<ResourceValue> parse(
      @NotNull File file,
      @Nullable String contents,
      @NotNull ResourceNamespace namespace,
      @Nullable String libraryName)
      throws IOException;
}
