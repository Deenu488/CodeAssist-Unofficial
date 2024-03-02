package com.tyron.code.language.kotlin;

import android.content.res.AssetManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.tyron.builder.BuildModule;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.Module;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.language.CompletionItemWrapper;
import com.tyron.code.ui.project.ProjectManager;
import com.tyron.code.util.TaskExecutor;
import com.tyron.completion.model.CompletionItem;
import com.tyron.completion.model.DrawableKind;
import com.tyron.editor.Editor;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

public class KotlinLanguage implements Language {

  private final Editor mEditor;
  private final KotlinAnalyzer mAnalyzer;

  public KotlinLanguage(Editor editor) {
    mEditor = editor;
    AssetManager assetManager = ApplicationLoader.applicationContext.getAssets();
    mAnalyzer = KotlinAnalyzer.create(editor);
  }

  @NonNull
  @Override
  public AnalyzeManager getAnalyzeManager() {
    return mAnalyzer;
  }

  @Override
  public int getInterruptionLevel() {
    return INTERRUPTION_LEVEL_SLIGHT;
  }

  @Override
  public void requireAutoComplete(
      @NonNull ContentReference content,
      @NonNull CharPosition position,
      @NonNull CompletionPublisher publisher,
      @NonNull Bundle extraArguments)
      throws CompletionCancelledException {

    char c = content.charAt(position.getIndex() - 1);
    if (!isAutoCompleteChar(c)) {
      return;
    }
    String prefix = CompletionHelper.computePrefix(content, position, this::isAutoCompleteChar);

  /*  CompletableFuture<String> future =
        TaskExecutor.executeAsyncProvideError(
            () -> {
              Project currentProject = ProjectManager.getInstance().getCurrentProject();
              if (currentProject != null) {
                Module module = currentProject.getModule(mEditor.getCurrentFile());
                if (module instanceof AndroidModule) {
                  File libraries = new File(module.getBuildDirectory(), "libraries");
                  List<String> jars = listFiles(libraries.toPath(), ".jar");
                  jars.add(BuildModule.getAndroidJar().getAbsolutePath());
                  jars.add(BuildModule.getLambdaStubs().getAbsolutePath());

                  jars.forEach(
                      jar -> {
                        try {
                          ZipFile zipFile = new ZipFile(jar);
                          Enumeration entries = zipFile.entries();

                          while (entries.hasMoreElements()) {
                            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
                            String filePath = zipEntry.getName();

                            if (filePath.endsWith(".class")) {

                              File classFile = new File(filePath);
                              String classFileName = classFile.getName().replace(".class", "");

                              String className = filePath.replace("/", ".");

                              if (classFileName.startsWith(prefix)) {

                                publisher.addItem(
                                    new CompletionItemWrapper(
                                        CompletionItem.create(
                                            classFileName,
                                            className,
                                            classFileName,
                                            DrawableKind.Class)));
                              }
                            }
                          }
                          zipFile.close();
                        } catch (Exception e) {

                        }
                      });
                }
              }

              return null;
            },
            (result, throwable) -> {});*/

    //  publisher.addItem(new CompletionItemWrapper(new CompletionItem (prefix + " " +
    // position.getLine() + position.getColumn())));

      KotlinAutoCompleteProvider provider = new KotlinAutoCompleteProvider(mEditor);
    CompletionList list =
        provider.getCompletionList(prefix, position.getLine(), position.getColumn());
    if (list != null) {
      for (CompletionItem item : list.items) {
        CompletionItemWrapper wrapper = new CompletionItemWrapper(item);
        publisher.addItem(wrapper);
              }
    }

  }

  public boolean isAutoCompleteChar(char p1) {
    return p1 == '.' || MyCharacter.isJavaIdentifierPart(p1);
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
    String text = content.getLine(line).substring(0, column);
    return getIndentAdvance(text);
  }

  public int getIndentAdvance(String p1) {
    KotlinLexer lexer = new KotlinLexer(CharStreams.fromString(p1));
    Token token;
    int advance = 0;
    while ((token = lexer.nextToken()) != null) {
      if (token.getType() == KotlinLexer.EOF) {
        break;
      }
      if (token.getType() == KotlinLexer.LCURL) {
        advance++;
        /*case RBRACE:
        advance--;
        break;*/
      }
    }
    advance = Math.max(0, advance);
    return advance * 4;
  }

  @Override
  public boolean useTab() {
    return true;
  }

  @Override
  public CharSequence format(CharSequence text) {
    return text;
  }

  @Override
  public SymbolPairMatch getSymbolPairs() {
    return new SymbolPairMatch.DefaultSymbolPairs();
  }

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return handlers;
  }

  @Override
  public void destroy() {}

  private final NewlineHandler[] handlers = new NewlineHandler[] {new BraceHandler()};

  class BraceHandler implements NewlineHandler {

    @Override
    public boolean matchesRequirement(String beforeText, String afterText) {
      return beforeText.endsWith("{") && afterText.startsWith("}");
    }

    @Override
    public NewlineHandleResult handleNewline(String beforeText, String afterText, int tabSize) {
      int count = TextUtils.countLeadingSpaceCount(beforeText, tabSize);
      int advanceBefore = getIndentAdvance(beforeText);
      int advanceAfter = getIndentAdvance(afterText);
      String text;
      StringBuilder sb =
          new StringBuilder("\n")
              .append(TextUtils.createIndent(count + advanceBefore, tabSize, useTab()))
              .append('\n')
              .append(text = TextUtils.createIndent(count + advanceAfter, tabSize, useTab()));
      int shiftLeft = text.length() + 1;
      return new NewlineHandleResult(sb, shiftLeft);
    }
  }

  private List<String> listFiles(Path directory, String extension) throws IOException {
    return Files.walk(directory)
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(extension))
        .map(Path::toString)
        .collect(Collectors.toList());
  }
}