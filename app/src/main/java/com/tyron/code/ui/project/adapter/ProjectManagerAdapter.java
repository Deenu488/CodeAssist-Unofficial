package com.tyron.code.ui.project.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.Module;
import com.tyron.code.R;

import java.util.ArrayList;
import java.util.List;
import com.tyron.code.util.ProjectUtils;
import java.io.File;
import android.content.ClipboardManager;
import android.content.Context;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.concurrent.Executors;
import com.tyron.code.ApplicationLoader;
import android.graphics.drawable.BitmapDrawable;

public class ProjectManagerAdapter extends RecyclerView.Adapter<ProjectManagerAdapter.ViewHolder> {

    private static final int TYPE_EMPTY = -1;
    private static final int TYPE_ITEM = 1;

    public interface OnProjectSelectedListener {
        void onProjectSelect(Project project);
    }

    public interface OnProjectLongClickedListener {
        boolean onLongClicked(View view, Project project);
    }

    private final List<Project> mProjects = new ArrayList<>();
    private OnProjectLongClickedListener mLongClickListener;
    private OnProjectSelectedListener mListener;

    public ProjectManagerAdapter() {

    }

    public void setOnProjectSelectedListener(OnProjectSelectedListener listener) {
        mListener = listener;
    }

    public void setOnProjectLongClickListener(OnProjectLongClickedListener listener) {
        mLongClickListener = listener;
    }

    public void submitList(@NonNull List<Project> projects) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mProjects.size();
            }

            @Override
            public int getNewListSize() {
                return projects.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mProjects.get(oldItemPosition).equals(projects.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return mProjects.get(oldItemPosition).equals(projects.get(newItemPosition));
            }
        });
        mProjects.clear();
        mProjects.addAll(projects);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout root = new FrameLayout(parent.getContext());
        root.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        final ViewHolder holder;
        if (viewType == TYPE_EMPTY) {
            holder = new EmptyViewHolder(root);
        } else {
            holder = new ItemViewHolder(root);
        }

        root.setOnClickListener(v -> {
           if (mListener != null) {
               int position = holder.getBindingAdapterPosition();
               if (position != RecyclerView.NO_POSITION) {
                   mListener.onProjectSelect(mProjects.get(position));				   
               }
           }
        });
        root.setOnLongClickListener(v -> {
            if (mLongClickListener != null) {
                int position = holder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    return mLongClickListener.onLongClicked(v, mProjects.get(position));
                }
            }
            return false;
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_ITEM) {
            ((ItemViewHolder) holder).bind(mProjects.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mProjects.isEmpty()) {
            return TYPE_EMPTY;
        }

        return TYPE_ITEM;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    private static class ItemViewHolder extends ViewHolder {
		private Context context;
        public ShapeableImageView icon;
        public TextView title;
		public TextView pkg;
        public ItemViewHolder(FrameLayout view) {
            super(view);
            LayoutInflater.from(view.getContext())
                    .inflate(R.layout.project_item, view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
			pkg = view.findViewById(R.id.pkg);
        }

        public void bind(Project module) {
			String webp = module.getRootFile() + "/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png";
			String pn = module.getRootFile() + "/app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp";
			
			File  web= new File(webp);		
			File  png= new File(pn);
			
			if(web.exists()) {
				BitmapDrawable bitmapDrawable = new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), web.toString()); 
				icon.setImageDrawable(bitmapDrawable);
			} else	if (png.exists()) {	
			BitmapDrawable bitmapDrawable = new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), png.toString()); 
			icon.setImageDrawable(bitmapDrawable);
			} else {
			icon.setImageResource(R.drawable.ic_launcher);
			}
			
			title.setText(module.getRootFile().getName());
			String manifest = module.getRootFile() + "/app/src/main/AndroidManifest.xml";
		
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();	
						try {
					DocumentBuilder db = dbf.newDocumentBuilder();
					Document doc = db.parse(new File(manifest));
					doc.getDocumentElement().normalize();
					
					String packageName = doc.getDocumentElement().getAttribute("package");					
			
					if  (packageName.isEmpty()) {
					pkg.setText("package error");
							}else {
					pkg.setText(packageName);		
						}
						
					} catch (ParserConfigurationException | SAXException |   IOException e) {
						}
				}
		}
    private static class EmptyViewHolder extends ViewHolder {

        public final TextView text;

        public EmptyViewHolder(FrameLayout view) {
            super(view);

            text = new TextView(view.getContext());
            text.setTextSize(18);
            text.setText(R.string.project_manager_empty);
            view.addView(text, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        }
    }
}
