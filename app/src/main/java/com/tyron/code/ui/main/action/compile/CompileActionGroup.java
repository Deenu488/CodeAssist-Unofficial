package com.tyron.code.ui.main.action.compile;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;

import com.tyron.actions.ActionGroup;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.code.R;
import com.tyron.code.ui.main.CompileCallback;
import com.tyron.code.ui.main.MainFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tyron.builder.project.Project;
import com.tyron.builder.compiler.BuildType;

import java.util.ArrayList;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.deenu143.gradle.utils.GradleUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import java.io.File;
import java.util.List;
import com.tyron.code.ui.main.MainViewModel;
import com.tyron.code.ui.main.IndexCallback;

public class CompileActionGroup extends ActionGroup {

    public static final String ID = "compileActionGroup";

    @Override
    public void update(@NonNull AnActionEvent event) {	
		Project project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }
		Context context = event.getData(CommonDataKeys.CONTEXT);
        MainViewModel mainViewModel = event.getData(MainFragment.MAIN_VIEW_MODEL_KEY);
        Boolean indexing = mainViewModel.isIndexing().getValue();
        if (indexing == null) {
            indexing = false;
        }
        if (project != null && !project.isCompiling() && !indexing) {      
			event.getPresentation().setEnabled(true);
		} else {
			event.getPresentation().setEnabled(false);
		}

        event.getPresentation().setVisible(true);   
        event.getPresentation().setText(context.getString(R.string.menu_run));
        event.getPresentation().setIcon(ContextCompat.getDrawable(context, R.drawable.round_play_arrow_24));
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
		return new AnAction[]{};
    }

	@Override
	public void actionPerformed(@NonNull AnActionEvent event) {
		Context context = event.getData(CommonDataKeys.CONTEXT);
		CompileCallback callback = event.getData(MainFragment.COMPILE_CALLBACK_KEY);

		Project project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }

	    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
		bottomSheetDialog.setContentView(R.layout.layout_dialog_run_actions);

		LinearLayout aarActions = bottomSheetDialog.findViewById(R.id.android_library_actions);
		LinearLayout jarActions = bottomSheetDialog.findViewById(R.id.java_library_actions);
		LinearLayout appActions = bottomSheetDialog.findViewById(R.id.android_app_actions);
		ConstraintLayout projectStatus = bottomSheetDialog.findViewById(R.id.project_status);

		MaterialCardView buildRelease = bottomSheetDialog.findViewById(R.id.buildRelease);
		MaterialCardView buildDebug = bottomSheetDialog.findViewById(R.id.buildDebug);
		MaterialCardView buildBundle = bottomSheetDialog.findViewById(R.id.buildBundle);

		MaterialCardView buildAar = bottomSheetDialog.findViewById(R.id.buildAar);
		MaterialCardView buildJar = bottomSheetDialog.findViewById(R.id.buildJar);
		MaterialCardView buildRunJar = bottomSheetDialog.findViewById(R.id.buildRunJar);

		File gradleFile = new File(project.getRootFile(), "app/build.gradle");
		try {
			List<String> plugins = GradleUtils.parsePlugins(gradleFile);
			plugins.forEach(v -> {
				if (plugins.contains("java.library")) {
					jarActions.setVisibility(View.VISIBLE);
					aarActions.setVisibility(View.GONE);
					appActions.setVisibility(View.GONE);
					projectStatus.setVisibility(View.GONE);
				}
				if (plugins.contains("com.android.library")) {
					aarActions.setVisibility(View.VISIBLE);
					jarActions.setVisibility(View.GONE);
					appActions.setVisibility(View.GONE);
					projectStatus.setVisibility(View.GONE);

				}
				if (plugins.contains("com.android.application")) {
					appActions.setVisibility(View.VISIBLE);
					aarActions.setVisibility(View.GONE);
					jarActions.setVisibility(View.GONE);
					projectStatus.setVisibility(View.GONE);
				}
				if (plugins.contains("com.android.application") || plugins.contains("com.android.library")
					|| plugins.contains("java.library")) {

				} else {
					projectStatus.setVisibility(View.VISIBLE);
				}

			});

		} catch (Exception e) {
			projectStatus.setVisibility(View.VISIBLE);
		}

		bottomSheetDialog.show();

		buildRelease.setOnClickListener(v -> {
			callback.compile(BuildType.RELEASE);
			bottomSheetDialog.dismiss();
		});
		buildDebug.setOnClickListener(v -> {	
			callback.compile(BuildType.DEBUG);
			bottomSheetDialog.dismiss();
		});
		buildBundle.setOnClickListener(v -> {
			callback.compile(BuildType.AAB);
			bottomSheetDialog.dismiss();
		});
		buildAar.setOnClickListener(v -> {			
			bottomSheetDialog.dismiss();
		});
		buildJar.setOnClickListener(v -> {
			bottomSheetDialog.dismiss();
		});
		buildRunJar.setOnClickListener(v -> {
			bottomSheetDialog.dismiss();
		});	
	}
}
