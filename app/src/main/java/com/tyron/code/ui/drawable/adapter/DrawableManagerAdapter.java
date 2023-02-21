package com.tyron.code.ui.drawable.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.tyron.code.ApplicationLoader;
import com.tyron.code.R;
import com.tyron.code.ui.drawable.Drawables;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DrawableManagerAdapter
    extends RecyclerView.Adapter<DrawableManagerAdapter.ViewHolder> {

  private static final int TYPE_EMPTY = -1;
  private static final int TYPE_ITEM = 1;

  public interface OnDrawableSelectedListener {
    void onDrwableSelect(Drawables drawables);
  }

  private final List<Drawables> mDrawables = new ArrayList<>();
  private OnDrawableSelectedListener mListener;

  public DrawableManagerAdapter() {}

  public void setOnDrawableSelectedListener(OnDrawableSelectedListener listener) {
    mListener = listener;
  }

  public void submitList(@NonNull List<Drawables> drawables) {
    DiffUtil.DiffResult diffResult =
        DiffUtil.calculateDiff(
            new DiffUtil.Callback() {
              @Override
              public int getOldListSize() {
                return mDrawables.size();
              }

              @Override
              public int getNewListSize() {
                return drawables.size();
              }

              @Override
              public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mDrawables.get(oldItemPosition).equals(drawables.get(newItemPosition));
              }

              @Override
              public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return mDrawables.get(oldItemPosition).equals(drawables.get(newItemPosition));
              }
            });
    mDrawables.clear();
    mDrawables.addAll(drawables);
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
              mListener.onDrwableSelect(mDrawables.get(position));
            }
          }
        });

    return holder;
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    if (holder.getItemViewType() == TYPE_ITEM) {
      ((ItemViewHolder) holder).bind(mDrawables.get(position));
    }
  }

  @Override
  public int getItemCount() {
    return mDrawables.size();
  }

  @Override
  public int getItemViewType(int position) {
    if (mDrawables.isEmpty()) {
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

    public ImageView drawable;
    public TextView title;
    private Context context;

    public ItemViewHolder(FrameLayout view) {
      super(view);
      LayoutInflater.from(view.getContext()).inflate(R.layout.drawable_manager_item, view);
      drawable = view.findViewById(R.id.drawable);
      title = view.findViewById(R.id.title);
    }

    public void bind(Drawables drawables) {
      String name = drawables.getRootFile().getName();
      Bitmap bitmap = BitmapFactory.decodeFile(drawables.getRootFile().getAbsolutePath());

      Drawable d = null;
      if (name.endsWith(".png")) {
        title.setText(name.replace(".png", ""));
        drawable.setImageBitmap(bitmap);
      } else if (name.endsWith(".webp")) {
        title.setText(name.replace(".webp", ""));
        drawable.setImageBitmap(bitmap);
      } else if (name.endsWith(".xml")) {
        title.setText(name.replace(".xml", ""));

        File icon_cache = ApplicationLoader.applicationContext.getExternalFilesDir("icon_cache");

        if (icon_cache.exists()) {

        } else {
          icon_cache.mkdirs();
        }

        File output =
            new File(
                icon_cache.getAbsolutePath(),
                drawables.getRootFile().getName().replace(".xml", ".svg"));
        Vector2Svg converter =
            new Vector2Svg(new File(drawables.getRootFile().getAbsolutePath()), output);
        if (!converter.createSvg()) {
          System.out.println("Error creating SVG from");
        } else {

          try {
            FileInputStream fileInputStream =
                new FileInputStream(new File(output.getAbsolutePath()));
            try {
              SVG svg = SVG.getFromInputStream(fileInputStream);
              d = new PictureDrawable(svg.renderToPicture());
              drawable.setImageDrawable(d);
              icon_cache.deleteOnExit();
            } catch (SVGParseException e) {
            }
          } catch (FileNotFoundException e) {
          }
        }

      } else if (name.endsWith(".jpg")) {
        title.setText(name.replace(".jpg", ""));
        drawable.setImageBitmap(bitmap);
      }
    }
  }

  private static class EmptyViewHolder extends ViewHolder {

    public final TextView text;

    public EmptyViewHolder(FrameLayout view) {
      super(view);

      text = new TextView(view.getContext());
      text.setTextSize(18);
      text.setText(R.string.drawable_manager_empty);
      view.addView(
          text,
          new FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.WRAP_CONTENT,
              FrameLayout.LayoutParams.WRAP_CONTENT,
              Gravity.CENTER));
    }
  }
}

class Vector2Svg {

  File source;
  File destination;

  public Vector2Svg(File source, File destination) {
    this.source = source;
    this.destination = destination;
  }

  public boolean createSvg() {
    try {
      AndroidVectorDrawable drawable = getDrawable();
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element svg = doc.createElement("svg");
      svg.setAttribute("viewBox", String.format("0 0 %.1f %.1f", drawable.width, drawable.height));
      for (Group group : drawable.groups) {
        Element g = doc.createElement("g");
        for (VectorPath path : group.paths) {
          Element child = doc.createElement("path");
          if (path.fillColor != null) {
            child.setAttribute("fill", path.fillColor);
          }
          child.setAttribute("d", path.pathData);
          g.appendChild(child);
        }
        svg.appendChild(g);
      }
      for (VectorPath path : drawable.paths) {
        Element child = doc.createElement("path");
        if (path.fillColor != null) {
          child.setAttribute("fill", path.fillColor);
        }
        child.setAttribute("d", path.pathData);
        svg.appendChild(child);
      }
      doc.appendChild(svg);
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(destination);
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.transform(source, result);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private AndroidVectorDrawable getDrawable()
      throws ParserConfigurationException, IOException, SAXException {
    Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
    xml.getDocumentElement().normalize();
    Node vector = xml.getElementsByTagName("vector").item(0);
    NamedNodeMap attributes = vector.getAttributes();
    NodeList children = vector.getChildNodes();

    double width = 0;
    double height = 0;
    for (int i = 0; i < attributes.getLength(); i++) {
      if (attributes.item(i).getNodeName().equals("android:viewportHeight")) {
        height = Double.parseDouble(attributes.item(i).getNodeValue());
      } else if (attributes.item(i).getNodeName().equals("android:viewportWidth")) {
        width = Double.parseDouble(attributes.item(i).getNodeValue());
      }
    }

    List<VectorPath> paths = new ArrayList<>();
    List<Group> groups = new ArrayList<>();

    for (int i = 0; i < children.getLength(); i++) {
      Node item = children.item(i);
      if (item.getNodeName().equals("group")) {
        List<VectorPath> groupPaths = new ArrayList<>();
        for (int j = 0; j < item.getChildNodes().getLength(); j++) {
          VectorPath path = getVectorPathFromNode(item.getChildNodes().item(j));
          if (path != null) {
            groupPaths.add(path);
          }
        }
        if (!groupPaths.isEmpty()) {
          groups.add(new Group(groupPaths));
        }
      } else {
        VectorPath path = getVectorPathFromNode(item);
        if (path != null) {
          paths.add(path);
        }
      }
    }

    return new AndroidVectorDrawable(paths, groups, width, height);
  }

  private VectorPath getVectorPathFromNode(Node item) {
    if (item.getNodeName().equals("path")) {
      String pathData = null;
      String fillColor = null;
      for (int j = 0; j < item.getAttributes().getLength(); j++) {
        Node node = item.getAttributes().item(j);
        String name = node.getNodeName();
        String value = node.getNodeValue();
        if (name.equals("android:pathData")) {
          pathData = value;
        } else if (name.equals("android:fillColor") && value.startsWith("#")) {
          fillColor = value;
        }
      }
      if (pathData != null) {
        return new VectorPath(pathData, fillColor);
      }
    }
    return null;
  }

  private class VectorPath {

    private String pathData;
    private String fillColor;

    private VectorPath(String pathData, String fillColor) {
      this.pathData = pathData;
      this.fillColor = fillColor;
    }
  }

  private class Group {

    private final List<VectorPath> paths;

    public Group(List<VectorPath> paths) {
      this.paths = paths;
    }
  }

  private class AndroidVectorDrawable {

    private final List<VectorPath> paths;
    private final List<Group> groups;
    private final double height;
    private final double width;

    private AndroidVectorDrawable(
        List<VectorPath> paths, List<Group> groups, double width, double height) {
      this.paths = paths;
      this.groups = groups;
      this.height = height;
      this.width = width;
    }
  }
}
