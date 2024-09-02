package com.tyron.code.ui.settings;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.transition.MaterialSharedAxis;
import org.codeassist.unofficial.R;

public class BuildSettingsFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
    setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
    setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.build_preferences, rootKey);
  }
}
