package com.tyron.code.ui.ssh;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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

import com.tyron.code.util.UiUtilsKt;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.os.Bundle;
import java.util.concurrent.Executors;
import android.os.Build;
import android.os.Environment;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import com.tyron.code.ui.ssh.adapter.SshKeyManagerAdapter;

public class SshKeyManagerFragment extends Fragment {

    public static final String TAG = SshKeyManagerFragment.class.getSimpleName();
	private RecyclerView mRecyclerView;
    private SshKeyManagerAdapter mAdapter;
	
	
	public static SshKeyManagerFragment newInstance( ) {
             SshKeyManagerFragment fragment = new SshKeyManagerFragment();
     	
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
        View view = inflater.inflate(R.layout.ssh_key_manager_fragment, container, false);
        view.setClickable(true);
	
		Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
			getParentFragmentManager().popBackStack());
     

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(menu -> getParentFragmentManager().popBackStackImmediate());

        View fab = view.findViewById(R.id.fab_add_ssh_key);
        UiUtilsKt.addSystemWindowInsetToMargin(fab, false, false, false, true);
        ViewCompat.requestApplyInsets(fab);
		
		mAdapter = new SshKeyManagerAdapter();
		//      mAdapter.setOnProjectSelectedListener(this::openProject);
		// mAdapter.setOnProjectLongClickListener(this::inflateProjectMenus);
        mRecyclerView = view.findViewById(R.id.ssh_keys_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.setAdapter(mAdapter);

		loadSshKeys();


        
		
		}
		
		
	
	private void loadSshKeys() {
        toggleLoading(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            String path;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                path = requireContext().getExternalFilesDir("/.ssh").getAbsolutePath();
            } else {
                path = Environment.getExternalStorageDirectory()+ "/.ssh";
            }
            File sshKeysDir = new File(path);
			if (sshKeysDir.exists()) {

			} else {
				sshKeysDir.mkdirs();
			}
            File[] files = sshKeysDir.listFiles(File::isFile);

            List<SshKeys> sshKeys = new ArrayList<>();
            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                for (File file : files) {
                    File filter = new File(file, "");
                    if (filter.exists()) {
                        SshKeys sshkeys = new SshKeys(new File(file.getAbsolutePath()
															   .replaceAll("%20", " ")));
                       
                        sshKeys.add(sshkeys);
                       
                    }
                }
            }

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    toggleLoading(false);
                    ProgressManager.getInstance().runLater(() -> {
                        mAdapter.submitList(sshKeys);
                        toggleNullProject(sshKeys);
                    }, 300);
                });
            }
        });
    }

    private void toggleNullProject(List<SshKeys> sshkeys) {
        ProgressManager.getInstance().runLater(() -> {
            if (getActivity() == null || isDetached()) {
                return;
            }
            View view = getView();
            if (view == null) {
                return;
            }

            View recycler = view.findViewById(R.id.ssh_keys_recycler);
            View empty = view.findViewById(R.id.empty_ssh_keys);

            TransitionManager.beginDelayedTransition(
				(ViewGroup) recycler.getParent(), new MaterialFade());
            if (sshkeys.size() == 0) {
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
            View recycler = view.findViewById(R.id.ssh_keys_recycler);
            View empty = view.findViewById(R.id.empty_container);
            View empty_ssh_keys= view.findViewById(R.id.empty_ssh_keys);
            empty_ssh_keys.setVisibility(View.GONE);

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
