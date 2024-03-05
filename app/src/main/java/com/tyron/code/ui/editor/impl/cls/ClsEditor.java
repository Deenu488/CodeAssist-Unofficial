package com.tyron.code.ui.editor.impl.cls;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.tyron.fileeditor.api.FileEditor;
import java.io.File;
import java.util.Objects;

public class ClsEditor implements FileEditor {

  private final File mFile;
  private final ClsEditorProvider mProvider;
  private ClsEditorFragment mFragment;

  public ClsEditor(@NonNull File file, ClsEditorProvider provider) {
    mFile = file;
    mProvider = provider;
    mFragment = createFragment(file);
  }

  protected ClsEditorFragment createFragment(@NonNull File file) {
    return ClsEditorFragment.newInstance(file);
  }

  @Override
  public Fragment getFragment() {
    return mFragment;
  }

  @Override
  public View getPreferredFocusedView() {
    return mFragment.getView();
  }

  @NonNull
  @Override
  public String getName() {
    return "Class Editor";
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public File getFile() {
    return mFile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClsEditor that = (ClsEditor) o;
    return Objects.equals(mFile, that.mFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mFile);
  }
}
