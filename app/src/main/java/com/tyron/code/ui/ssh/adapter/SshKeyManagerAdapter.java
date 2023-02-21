package com.tyron.code.ui.ssh.adapter;

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
import com.tyron.code.R;
import com.tyron.code.ui.ssh.SshKeys;
import java.util.ArrayList;
import java.util.List;

public class SshKeyManagerAdapter extends RecyclerView.Adapter<SshKeyManagerAdapter.ViewHolder> {

  private static final int TYPE_EMPTY = -1;
  private static final int TYPE_ITEM = 1;
  private final int limit = 2;

  public interface OnSshKeysSelectedListener {
    void onSshKeysSelect(SshKeys sshKeys);
  }

  public interface OnSshKeysLongClickedListener {
    boolean onLongClicked(View view, SshKeys sshKeys);
  }

  private final List<SshKeys> mSshKeys = new ArrayList<>();
  private OnSshKeysLongClickedListener mLongClickListener;
  private OnSshKeysSelectedListener mListener;

  public SshKeyManagerAdapter() {}

  public void OnSshKeysSelectedListener(OnSshKeysSelectedListener listener) {
    mListener = listener;
  }

  public void OnSshKeysLongClickedListener(OnSshKeysLongClickedListener listener) {
    mLongClickListener = listener;
  }

  public void submitList(@NonNull List<SshKeys> sshKeys) {
    DiffUtil.DiffResult diffResult =
        DiffUtil.calculateDiff(
            new DiffUtil.Callback() {
              @Override
              public int getOldListSize() {
                return mSshKeys.size();
              }

              @Override
              public int getNewListSize() {
                return sshKeys.size();
              }

              @Override
              public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mSshKeys.get(oldItemPosition).equals(sshKeys.get(newItemPosition));
              }

              @Override
              public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return mSshKeys.get(oldItemPosition).equals(sshKeys.get(newItemPosition));
              }
            });
    mSshKeys.clear();
    mSshKeys.addAll(sshKeys);
    diffResult.dispatchUpdatesTo(this);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    FrameLayout root = new FrameLayout(parent.getContext());
    root.setLayoutParams(
        new RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    final ViewHolder holder;
    if (viewType == TYPE_EMPTY) {
      holder = new EmptyViewHolder(root);
    } else {
      holder = new ItemViewHolder(root);
    }

    root.setOnClickListener(
        v -> {
          if (mListener != null) {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
              mListener.onSshKeysSelect((mSshKeys.get(position)));
            }
          }
        });
    root.setOnLongClickListener(
        v -> {
          if (mLongClickListener != null) {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
              return mLongClickListener.onLongClicked(v, mSshKeys.get(position));
            }
          }
          return false;
        });
    return holder;
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    if (holder.getItemViewType() == TYPE_ITEM) {
      ((ItemViewHolder) holder).bind(mSshKeys.get(position));
    }
  }

  @Override
  public int getItemCount() {

    if (mSshKeys.size() > limit) {
      return limit;
    }
    return mSshKeys.size();
  }

  @Override
  public int getItemViewType(int position) {
    if (mSshKeys.isEmpty()) {
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

    public ShapeableImageView icon;
    public TextView title;

    public ItemViewHolder(FrameLayout view) {
      super(view);
      LayoutInflater.from(view.getContext()).inflate(R.layout.ssh_key_item, view);
      icon = view.findViewById(R.id.icon);
      title = view.findViewById(R.id.title);
    }

    public void bind(SshKeys sshKeys) {
      if (sshKeys.getRootFile().getName().endsWith(".key")) {
        icon.setImageResource(R.drawable.ic_key);
      } else if (sshKeys.getRootFile().getName().endsWith(".pub")) {
        icon.setImageResource(R.drawable.ic_pub);
      }
      title.setText(sshKeys.getRootFile().getName());
    }
  }

  private static class EmptyViewHolder extends ViewHolder {

    public final TextView text;

    public EmptyViewHolder(FrameLayout view) {
      super(view);

      text = new TextView(view.getContext());
      text.setTextSize(18);
      text.setText(R.string.ssh_keys_manager_empty);
      view.addView(
          text,
          new FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.WRAP_CONTENT,
              FrameLayout.LayoutParams.WRAP_CONTENT,
              Gravity.CENTER));
    }
  }
}
