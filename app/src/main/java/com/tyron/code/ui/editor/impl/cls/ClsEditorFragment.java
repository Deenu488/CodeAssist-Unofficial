package com.tyron.code.ui.editor.impl.cls;

import static io.github.rosemoe.sora2.text.EditorUtil.getDefaultColorScheme;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import com.blankj.utilcode.util.ThreadUtils;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.R;
import com.tyron.code.ui.editor.impl.text.rosemoe.CodeEditorView;
import com.tyron.code.ui.editor.scheme.CompiledEditorScheme;
import com.tyron.code.ui.theme.ThemeRepository;
import com.tyron.code.util.TaskExecutor;
import com.tyron.common.SharedPreferenceKeys;
import io.github.rosemoe.sora.langs.textmate.theme.TextMateColorScheme;
import io.github.rosemoe.sora2.text.EditorUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;

public class ClsEditorFragment extends Fragment {

  public static ClsEditorFragment newInstance(File file) {
    ClsEditorFragment fragment = new ClsEditorFragment();
    Bundle bundle = new Bundle();
    bundle.putString("file", file.getAbsolutePath());
    fragment.setArguments(bundle);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    CodeEditorView editor = new CodeEditorView(requireContext());

    editor.setEditable(false);
    editor.setColorScheme(new CompiledEditorScheme(requireContext()));
    String key =
        EditorUtil.isDarkMode(requireContext())
            ? ThemeRepository.DEFAULT_NIGHT
            : ThemeRepository.DEFAULT_LIGHT;
    TextMateColorScheme scheme = ThemeRepository.getColorScheme(key);
    if (scheme == null) {
      scheme = getDefaultColorScheme(requireContext());
      ThemeRepository.putColorScheme(key, scheme);
    }
    editor.setColorScheme(scheme);

    // editor.setBackgroundAnalysisEnabled(false);
    editor.setTypefaceText(
        ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono_regular));
    editor.setLigatureEnabled(true);
    editor.setHighlightCurrentBlock(true);
    editor.setEdgeEffectColor(Color.TRANSPARENT);
    editor.setInputType(
        EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            | EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
            | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

    SharedPreferences pref = ApplicationLoader.getDefaultPreferences();
    editor.setWordwrap(pref.getBoolean(SharedPreferenceKeys.EDITOR_WORDWRAP, false));
    editor.setTextSize(Integer.parseInt(pref.getString(SharedPreferenceKeys.FONT_SIZE, "12")));

    if (getArguments() != null) {
      String file = requireArguments().getString("file", "");
      File clsFile = new File(file);
      if (clsFile.exists()) {

        CompletableFuture<String> future =
            TaskExecutor.executeAsyncProvideError(
                () -> {
                  ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                  PrintStream printStream = new PrintStream(outputStream);

                  String[] args = new String[] {"-p", clsFile.getAbsolutePath()};
                  com.sun.tools.javap.JavapTask javapTask = new com.sun.tools.javap.JavapTask();
                  javapTask.handleOptions(args);
                  javapTask.setLog(printStream);
                  javapTask.run();

                  String result = outputStream.toString();
                  int indexOfNewLine = result.indexOf('\n');

                  if (indexOfNewLine != -1) {
                    result = result.substring(indexOfNewLine + 1);
                  }

                  return result;
                },
                (result, throwable) -> {});

        future.whenComplete(
            (result, error) -> {
              ThreadUtils.runOnUiThread(
                  () -> {
                    if (result != null) {
                      editor.setText(result);
                    }
                  });
            });
      }
    }

    return editor;
  }
}
