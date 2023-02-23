package com.tyron.completion.java.patterns.elements;

import javax.lang.model.element.Element;
import org.jetbrains.kotlin.com.intellij.util.ProcessingContext;

public interface JavacElementPattern {
  boolean accepts(Element element, ProcessingContext context);
}
