package com.tyron.code.ui.editor.impl.cls;

import androidx.annotation.NonNull;
import com.tyron.fileeditor.api.FileEditor;
import com.tyron.fileeditor.api.FileEditorProvider;
import java.io.File;

public class ClsEditorProvider implements FileEditorProvider {

  private static final String TYPE_ID = "class-editor";

  @Override
  public boolean accept(@NonNull File file) {
    if (file.isDirectory()) {
      return false;
    }
    String name = file.getName();
    if (name.endsWith(".class")) {
      return true;
    }

    return false;
  }

  @NonNull
  @Override
  public FileEditor createEditor(@NonNull File file) {
    return new ClsEditor(file, this);
  }

  @NonNull
  @Override
  public String getEditorTypeId() {
    return TYPE_ID;
  }
}
