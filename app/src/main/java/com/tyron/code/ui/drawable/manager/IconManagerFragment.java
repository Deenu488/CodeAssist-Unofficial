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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.tyron.common.util.Decompress;
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
import com.tyron.code.ui.drawable.adapter.IconManagerAdapter;
import com.tyron.code.ui.drawable.EditVectorDialogFragment;
import com.tyron.code.ui.drawable.Icons;
import android.widget.Toast;
import android.widget.TextView;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.SearchView;

public class IconManagerFragment extends Fragment {

    public static final String TAG = IconManagerFragment.class.getSimpleName();
 
    private RecyclerView mRecyclerView;
    private IconManagerAdapter mAdapter;
    private List<Icons> Icons = new ArrayList<>();
    public static IconManagerFragment newInstance( ) {
        IconManagerFragment fragment = new IconManagerFragment();

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
        View view = inflater.inflate(R.layout.icon_manager_fragment, container, false);
        view.setClickable(true);
    
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->  getParentFragmentManager().popBackStack());
	
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(menu -> getParentFragmentManager().popBackStackImmediate());
        toolbar.addMenuProvider(new MenuProvider() {
				@Override
				public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {												
					menuInflater.inflate(R.menu.icon_manager_menu, menu); 	
					MenuItem searchViewItem = menu.findItem(R.id.search_bar);
					SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchViewItem);

					searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
							@Override
							public boolean onQueryTextSubmit(String query) {						    
								mAdapter.getFilter().filter(query);
								return false;
							}

							@Override
							public boolean onQueryTextChange(String newText) {						
								mAdapter.getFilter().filter(newText);
								return false;
							}
						});
				}

				@Override
				public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
					return false;
				}
			});
        mAdapter = new IconManagerAdapter();
        mAdapter.setOnIconSelectedListener(this::showDialog);
        mRecyclerView = view.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        mRecyclerView.setAdapter(mAdapter);
     
        if (!new File(getPackageDirectory() + "/Icons/").exists()) {
            showConfirmationDialog();
        } else {
            loadIcons();
		}
    } 
    private void loadIcons() {
       
        toggleLoading(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            String path = requireContext().getExternalFilesDir("/Icons").getAbsolutePath();
        
            File iconDir = new File(path);
            if (iconDir.exists()) {

            } else {
                iconDir.mkdirs();
            }
            File[] files = iconDir.listFiles(File::isFile);
       
            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                for (File file : files) {

                    if (file.exists()) {
                        Icons icon = new Icons(new File(file.getAbsolutePath()
                                                               .replaceAll("%20", " ")));
                        if (icon.getRootFile().getName().endsWith(".svg")) {
                            Icons.add(icon);
                        }  
                        if (icon.getRootFile().getName().endsWith(".xml")) {
                            Icons.add(icon);
                        }  
                    }
                }
            }
            

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    toggleLoading(false);
                    ProgressManager.getInstance().runLater(() -> {
                        mAdapter.submitList(Icons);
                        toggleNullProject(Icons);
                    }, 300);
                });
            }
        });
    }

    private void toggleNullProject(List<Icons> icons) {
        ProgressManager.getInstance().runLater(() -> {
            if (getActivity() == null || isDetached()) {
                return;
            }
            View view = getView();
            if (view == null) {
                return;
            }

            View recycler = view.findViewById(R.id.recyclerView);
            View empty = view.findViewById(R.id.empty_icons);

            TransitionManager.beginDelayedTransition(
                (ViewGroup) recycler.getParent(), new MaterialFade());
            if (icons.size() == 0) {
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
            View empty_icons = view.findViewById(R.id.empty_icons);
            empty_icons.setVisibility(View.GONE);

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
    private void showDialog(Icons icons) {
     
        Bundle bundle = new Bundle();
        bundle.putString("iconPath", icons.getRootFile().getAbsolutePath());
       
        FragmentActivity activity = (FragmentActivity) (requireContext());
        FragmentManager fm = activity.getSupportFragmentManager();

        if (fm.findFragmentByTag(EditVectorDialogFragment.TAG) == null) {

            EditVectorDialogFragment fragment = new EditVectorDialogFragment();

            fragment.setArguments(bundle);

            fragment.show(fm, EditVectorDialogFragment.TAG);
            }
            }
  
    private String getPackageDirectory() {
        return requireContext().getExternalFilesDir(null).getAbsolutePath();
    }
    
    private void showConfirmationDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.extract_icons);
        builder.setMessage(R.string.confirm_extract);
        builder.setPositiveButton(R.string.extract, (d, w) -> {
            showLoading(true);
            ProgressManager.getInstance().runNonCancelableAsync(() ->startExtractingIcons());
       
            });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    private void startExtractingIcons() {
        Decompress.unzipFromAssets(requireContext(), "Icons.zip", getPackageDirectory());

        ProgressManager.getInstance().runLater(() -> {
          
            showLoading(false);
              loadIcons(); 
      
        });
        
	}
    private void showLoading(boolean show) {
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
            View empty_icons = view.findViewById(R.id.empty_icons);
            empty_icons.setVisibility(View.GONE);
            TextView empty_label = view.findViewById(R.id.empty_label);
            
            TransitionManager.beginDelayedTransition((ViewGroup) recycler.getParent(),
                                                     new MaterialFade());
            if (show) {
                recycler.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
                empty_label.setVisibility(View.VISIBLE);
                empty_label.setText(R.string.extracting_icons);
            } else {
                recycler.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
                empty_label.setVisibility(View.GONE);
            }
        }, 300);
    }
    
}
