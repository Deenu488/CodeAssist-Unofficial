package com.tyron.code.ui.editor.log;

import static io.github.rosemoe.sora2.text.EditorUtil.getDefaultColorScheme;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tyron.builder.log.LogViewModel;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.R;
import com.tyron.code.ui.editor.impl.FileEditorManagerImpl;
import com.tyron.code.ui.editor.impl.text.rosemoe.CodeEditorFragment;
import com.tyron.code.ui.editor.impl.text.rosemoe.CodeEditorView;
import com.tyron.code.ui.editor.scheme.CompiledEditorScheme;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.ui.theme.ThemeRepository;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.editor.Caret;
import com.tyron.fileeditor.api.FileEditorManager;
import io.github.rosemoe.sora.langs.textmate.theme.TextMateColorScheme;
import io.github.rosemoe.sora2.text.EditorUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;
import javax.tools.Diagnostic;

public class AppLogFragment extends Fragment implements ProjectManager.OnProjectOpenListener {

  /** Only used in IDE Logs * */
  private Handler mHandler;

  public static AppLogFragment newInstance(int id) {
    AppLogFragment fragment = new AppLogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("id", id);
    fragment.setArguments(bundle);
    return fragment;
  }

  private CodeEditorView mEditor;
  private FloatingActionButton copyText, errorsFab;
  private View mRoot;
  private int id;
  private MainViewModel mMainViewModel;
  private LogViewModel mModel;
  private OnDiagnosticClickListener mListener;
  List<DiagnosticWrapper> diags = new ArrayList<>();
  List<ErrorItem> errors = new ArrayList<>();

  public AppLogFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    id = requireArguments().getInt("id");

    mModel = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mRoot = inflater.inflate(R.layout.app_log_fragment, container, false);
    return mRoot;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mEditor = view.findViewById(R.id.output_text);
    copyText = view.findViewById(R.id.copy_text);
    errorsFab = view.findViewById(R.id.errors_fab);

    errorsFab.setOnClickListener(
        v -> {
          if (errors != null) {
            errors.clear();
          }

          for (DiagnosticWrapper diagnostic : diags) {
            if (diagnostic != null) {

              if (diagnostic.getKind() != null && diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                String error = diagnostic.getMessage(Locale.getDefault());
                if (diagnostic.getSource() != null) {
                  String label = diagnostic.getSource().getName();
                  if (label != null) {
                    label = label + " : line:" + diagnostic.getLineNumber() + " : " + error;
                    errors.add(new ErrorItem(label, diagnostic.getSource(), diagnostic));
                  }
                }
              }
            }
          }

          if (errors != null && !errors.isEmpty()) {

            ArrayAdapter<ErrorItem> adapter =
                new ArrayAdapter<ErrorItem>(
                    requireContext(),
                    android.R.layout.select_dialog_item,
                    android.R.id.text1,
                    errors) {
                  @NonNull
                  @Override
                  public View getView(
                      int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);

                    TextView textView = view.findViewById(android.R.id.text1);                   
                    ErrorItem errorItem = getItem(position);

                    textView.setTextSize(16);
                    textView.setTextColor(0xffcf6679);
                    textView.setText(errorItem.getMessage());
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.ic_error, 0, 0, 0);
                    return view;
                  }
                };

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle(getString(R.string.errors_found, String.valueOf(errors.size())));
            builder.setAdapter(
                adapter,
                (dialog, which) -> {
                  ErrorItem selectedErrorItem = errors.get(which);

                  if (selectedErrorItem.getFile() != null) {
                    if (getContext() != null) {
                      FileEditorManager manager = FileEditorManagerImpl.getInstance();
                      manager.openFile(
                          requireContext(),
                          selectedErrorItem.getFile(),
                          it -> {
                            if (selectedErrorItem.getDiagnosticWrapper().getLineNumber() > 0
                                && selectedErrorItem.getDiagnosticWrapper().getColumnNumber() > 0) {
                              Bundle bundle = new Bundle(it.getFragment().getArguments());
                              bundle.putInt(
                                  CodeEditorFragment.KEY_LINE,
                                  (int) selectedErrorItem.getDiagnosticWrapper().getLineNumber());
                              bundle.putInt(
                                  CodeEditorFragment.KEY_COLUMN,
                                  (int) selectedErrorItem.getDiagnosticWrapper().getColumnNumber());
                              it.getFragment().setArguments(bundle);
                              manager.openFileEditor(it);
                            }
                          });
                    }
                  }
                });

            builder.show();
          } else {
            Toast.makeText(requireContext(), R.string.no_errors_found, Toast.LENGTH_SHORT).show();
          }
        });

    copyText.setOnClickListener(
        v -> {
          Caret caret = mEditor.getCaret();
          if (!(caret.getStartLine() == caret.getEndLine()
              && caret.getStartColumn() == caret.getEndColumn())) {

          } else {

            String content = mEditor.getText().toString().trim();
            if (content != null && !content.isEmpty()) {
              copyContent(content);
            }
          }
        });

    mEditor.setEditable(false);
    configureEditor(mEditor);

    if (mModel != null) {
      mModel.getLogs(id).observe(getViewLifecycleOwner(), this::process);
    }
  }

  private void copyContent(String content) {
    ClipboardManager clipboard =
        (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
    clipboard.setText(content);
    Toast toast = Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_LONG);
    toast.show();
  }

  private void configureEditor(@NonNull CodeEditorView editor) {

    editor.setEditable(false);
    editor.setColorScheme(new CompiledEditorScheme(requireContext()));

    /*   String schemeValue =
        ApplicationLoader.getDefaultPreferences().getString(SharedPreferenceKeys.SCHEME, null);
    if (schemeValue != null
        && new File(schemeValue).exists()
        && ThemeRepository.getColorScheme(schemeValue) != null) {
      TextMateColorScheme scheme = ThemeRepository.getColorScheme(schemeValue);
      if (scheme != null) {
        editor.setColorScheme(scheme);
      }
    }*/

    String key =
        EditorUtil.isDarkMode(requireContext())
            ? ThemeRepository.DEFAULT_NIGHT
            : ThemeRepository.DEFAULT_LIGHT;
    TextMateColorScheme scheme = ThemeRepository.getColorScheme(key);
    if (scheme == null) {
      scheme = getDefaultColorScheme(requireContext());
      ThemeRepository.putColorScheme(key, scheme);
    }
    mEditor.setColorScheme(scheme);

    // editor.setBackgroundAnalysisEnabled(false);
    editor.setTypefaceText(
        ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono_regular));
    // editor.setLigatureEnabled(true);
    // editor.setHighlightCurrentBlock(true);
    editor.setEdgeEffectColor(Color.TRANSPARENT);
    editor.setInputType(
        EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            | EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
            | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

    SharedPreferences pref = ApplicationLoader.getDefaultPreferences();
    // editor.setWordwrap(pref.getBoolean(SharedPreferenceKeys.EDITOR_WORDWRAP, false));
    editor.setWordwrap(true);
    editor.setTextSize(Integer.parseInt(pref.getString(SharedPreferenceKeys.FONT_SIZE, "10")));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
  }

  private void process(List<DiagnosticWrapper> texts) {
    final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
    if (handler != null) {
      handler.postDelayed(
          () -> {
            SpannableStringBuilder combinedText = new SpannableStringBuilder();

            if (texts != null) {
              // Create a copy of the list to avoid ConcurrentModificationException
              List<DiagnosticWrapper> diagnostics = new ArrayList<>(texts);
              this.diags = diagnostics;
              for (DiagnosticWrapper diagnostic : diagnostics) {
                if (diagnostic != null) {
                  if (diagnostic.getKind() != null) {
                    combinedText.append(diagnostic.getKind().name()).append(": ");
                    addDiagnosticSpan(combinedText, diagnostic);
                    combinedText.append(' ');
                  }

                  if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    combinedText.append(diagnostic.getMessage(Locale.getDefault()));
                  } else {
                    combinedText.append(diagnostic.getMessage(Locale.getDefault()));
                  }

                  if (diagnostic.getSource() != null) {
                    combinedText.append(' ');
                  }

                  combinedText.append("\n");
                }
              }
            }

            mEditor.setText(combinedText);
          },
          100);
    }
  }

  @Override
  public void onProjectOpen(Project project) {}

  @ColorInt
  private int getColor(Diagnostic.Kind kind) {
    switch (kind) {
      case ERROR:
        return 0xffcf6679;
      case MANDATORY_WARNING:
      case WARNING:
        return Color.YELLOW;
      case NOTE:
        return Color.CYAN;
      default:
        return 0xffFFFFFF;
    }
  }

  private void addDiagnosticSpan(SpannableStringBuilder sb, DiagnosticWrapper diagnostic) {
    if (diagnostic.getSource() == null || !diagnostic.getSource().exists()) {
      return;
    }

    if (diagnostic.getOnClickListener() != null) {
      ClickableSpan span =
          new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
              diagnostic.getOnClickListener().onClick(widget);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
              super.updateDrawState(ds);
              ds.setColor(getColor(diagnostic.getKind())); // set color
              ds.setUnderlineText(false); // underline the link text
            }
          };
      sb.append("[" + diagnostic.getExtra() + "]", span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      return;
    }

    ClickableSpan span =
        new ClickableSpan() {
          @Override
          public void onClick(@NonNull View view) {
            if (mListener != null) {
              mListener.onClick(diagnostic);
            }
          }
        };

    String label = diagnostic.getSource().getName();
    label = label + ":" + diagnostic.getLineNumber();
    sb.append("[" + label + "]", span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  public interface OnDiagnosticClickListener {
    void onClick(DiagnosticWrapper diagnostic);
  }

  public void setOnDiagnosticClickListener(OnDiagnosticClickListener listener) {
    mListener = listener;
  }

  class ErrorItem {
    private String message;
    private String path;
    private DiagnosticWrapper diagnostic;

    public ErrorItem(String message, String path, DiagnosticWrapper diagnostic) {
      this.message = message;
      this.path = path;
      this.diagnostic = diagnostic;
    }

    public ErrorItem(String message, File path, DiagnosticWrapper diagnostic) {
      this.message = message;
      this.path = path.getAbsolutePath();
      this.diagnostic = diagnostic;
    }

    public String getMessage() {
      return message;
    }

    private DiagnosticWrapper getDiagnosticWrapper() {
      return diagnostic;
    }

    public String getPath() {
      return path;
    }

    public File getFile() {
      return new File(path);
    }
  }
}