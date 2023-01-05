package com.tyron.code.ui.drawable.manager;

import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentActivity;
import com.tyron.code.R;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.transition.MaterialFade;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.tyron.completion.progress.ProgressManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tyron.common.util.AndroidUtilities;
import com.google.android.material.textfield.TextInputLayout;
import org.apache.commons.io.FileUtils;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.code.util.UiUtilsKt;
import androidx.recyclerview.widget.RecyclerView;
import android.content.SharedPreferences;
import java.io.File;
import com.tyron.code.ApplicationLoader;
import androidx.recyclerview.widget.GridLayoutManager;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import com.tyron.code.ui.drawable.adapter.DrawableManagerAdapter;
import com.tyron.code.ui.drawable.Drawables;
import com.tyron.code.ui.project.ProjectManager;

public class DrawableManagerFragment extends Fragment {
    
    public static final String TAG = DrawableManagerFragment.class.getSimpleName();
 
    private RecyclerView mRecyclerView;
    private DrawableManagerAdapter mAdapter;
    
    public static DrawableManagerFragment newInstance( ) { 
    DrawableManagerFragment fragment = new DrawableManagerFragment();   
    return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.drawable_manager_fragment, container, false);
        view.setClickable(true);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->  getParentFragmentManager().popBackStack());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(menu -> getParentFragmentManager().popBackStackImmediate());
      
        mAdapter = new DrawableManagerAdapter();
      //  mAdapter.setOnDrawableSelectedListener(this::showDialog);
        mRecyclerView = view.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        mRecyclerView.setAdapter(mAdapter);
        loadDrwables();
        }
        
    private void loadDrwables() {
        toggleLoading(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            String path =  ProjectManager.getInstance().getCurrentProject().getRootFile().getAbsolutePath() + "/app/src/main/res/drawable/";
            

            File drawableDir = new File(path);
            if (drawableDir.exists()) {

            } else {
                drawableDir.mkdirs();
            }
            File[] files = drawableDir.listFiles(File::isFile);

            List<Drawables> Drawables = new ArrayList<>();
            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                for (File file : files) {

                    if (file.exists()) {
                        Drawables drawable = new Drawables(new File(file.getAbsolutePath()
                                                        .replaceAll("%20", " ")));
                        if (drawable.getRootFile().getName().endsWith(".png")) {
                            Drawables.add(drawable);
                        } 
                        if (drawable.getRootFile().getName().endsWith(".webp")) {
                            Drawables.add(drawable);
                        }  
                        if (drawable.getRootFile().getName().endsWith(".xml")) {
                            Drawables.add(drawable);
                        }  
                        if (drawable.getRootFile().getName().endsWith(".jpg")) {
                            Drawables.add(drawable);
                        }  
                    }
                }
            }


            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    toggleLoading(false);
                    ProgressManager.getInstance().runLater(() -> {
                        mAdapter.submitList(Drawables);
                        toggleNullProject(Drawables);
                    }, 300);
                });
            }
        });
    }

    private void toggleNullProject(List<Drawables> drawables) {
        ProgressManager.getInstance().runLater(() -> {
            if (getActivity() == null || isDetached()) {
                return;
            }
            View view = getView();
            if (view == null) {
                return;
            }

            View recycler = view.findViewById(R.id.recyclerView);
            View empty = view.findViewById(R.id.empty_drawables);

            TransitionManager.beginDelayedTransition(
                (ViewGroup) recycler.getParent(), new MaterialFade());
            if (drawables.size() == 0) {
                recycler.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
            } else {
                recycler.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
            }
        }, 300);
    }

    private void toggleLoading(boolean show) {
        ProgressManager.getInstance().runLater(() -> {
            if (getActivity() == null || isDetached()) {
                return;
            }
            View view = getView();
            if (view == null) {
                return;
            }
            View recycler = view.findViewById(R.id.recyclerView);
            View empty = view.findViewById(R.id.empty_container);
            View empty_drawables = view.findViewById(R.id.empty_drawables);
            empty_drawables.setVisibility(View.GONE);

            TransitionManager.beginDelayedTransition((ViewGroup) recycler.getParent(),
                                                     new MaterialFade());
            if (show) {
                recycler.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
            } else {
                recycler.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
            }
        }, 300);
    }
    }
