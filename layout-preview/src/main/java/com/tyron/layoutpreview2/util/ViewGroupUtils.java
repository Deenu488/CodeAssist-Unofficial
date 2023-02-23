package com.tyron.layoutpreview2.util;

import android.content.res.XmlResourceParser;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ViewGroupUtils {

  private static XmlResourceParser sParser;

  @SuppressWarnings("DanglingJavadoc")
  public static ViewGroup.LayoutParams generateDefaultLayoutParams(@NonNull ViewGroup parent) {

    /**
     * This whole method is a hack! To generate layout params, since no other way exists. Refer :
     * http://stackoverflow.com/questions/7018267/generating-a-layoutparams-based-on-the-type-of-parent
     */
    if (null == sParser) {
      synchronized (ViewGroupUtils.class) {
        if (null == sParser) {
          initializeAttributeSet(parent);
        }
      }
    }

    return parent.generateLayoutParams(sParser);
  }

  private static void initializeAttributeSet(@NonNull ViewGroup parent) {
    sParser = parent.getResources().getLayout(android.R.layout.test_list_item);
    try {
      //noinspection StatementWithEmptyBody
      while (sParser.nextToken() != XmlPullParser.START_TAG) {
        // Skip everything until the view tag.
      }
    } catch (XmlPullParserException | IOException e) {
      e.printStackTrace();
    }
  }
}
