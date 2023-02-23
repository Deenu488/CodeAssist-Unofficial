package com.tyron.layoutpreview.resource;

import android.util.Pair;
import com.flipkart.android.proteus.value.Primitive;
import com.flipkart.android.proteus.value.Value;
import com.tyron.layoutpreview.util.XmlUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ResourceDimensionParser {

  public static Pair<String, Value> parseDimension(XmlPullParser parser)
      throws IOException, XmlPullParserException {
    parser.require(XmlPullParser.START_TAG, null, "dimen");

    String name = null;
    for (int i = 0; i < parser.getAttributeCount(); i++) {
      String attributeName = parser.getAttributeName(i);
      String attributeValue = parser.getAttributeValue(i);
      if ("name".equals(attributeName)) {
        name = attributeValue;
      }
    }

    String text = XmlUtils.readText(parser);

    Value value = new Primitive(text);

    parser.require(XmlPullParser.END_TAG, null, "dimen");
    return Pair.create(name, value);
  }
}
