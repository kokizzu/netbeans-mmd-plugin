package com.igormaznitsa.mindmap.annotations.processor;

import static java.util.Objects.requireNonNull;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Objects;
import javax.lang.model.element.Element;

/**
 * Immutable data object wrapping found MMD annotation, its path and line number.
 */
public class MmdAnnotationWrapper {
  /**
   * Found annotation.
   */
  private final Annotation annotation;
  /**
   * Path to source class file.
   */
  private final Path path;
  /**
   * Line of the annotation in the source class file.
   */
  private final long line;

  /**
   * Element providing the annotation.
   */
  private final Element element;

  /**
   * Constructor.
   *
   * @param element    element providing annotation, must not be null
   * @param annotation found annotation, must not be null
   * @param path       source class file path, must not be null
   * @param line       line number of the annotation in the source class file
   * @throws NullPointerException     thrown if any argument is null
   * @throws IllegalArgumentException thrown if line is zero or negative one
   */
  public MmdAnnotationWrapper(
      final Element element, final Annotation annotation, final Path path, final long line) {
    this.element = requireNonNull(element);
    this.annotation = requireNonNull(annotation);
    this.path = requireNonNull(path);
    if (line < 1) {
      throw new IllegalArgumentException("Unexpected line number: " + line);
    }
    this.line = line;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.annotation, this.path, this.line);
  }

  @Override
  public boolean equals(final Object that) {
    if (that == null) {
      return false;
    }
    if (that == this) {
      return true;
    }
    if (that instanceof MmdAnnotationWrapper) {
      final MmdAnnotationWrapper thatInstance = (MmdAnnotationWrapper) that;
      return this.annotation.equals(thatInstance.annotation)
          && this.line == thatInstance.line
          && this.path.equals(thatInstance.path);
    }
    return false;
  }

  /**
   * Get annotation and automatically cast to type.
   *
   * @param <A> cast to annotation type
   * @return the annotation, must not be null
   */
  @SuppressWarnings("unchecked")
  public <A extends Annotation> A asAnnotation() {
    return (A) this.annotation;
  }

  /**
   * Get source class file path.
   *
   * @return the source class file path, must not be null
   */
  public Path getPath() {
    return this.path;
  }

  /**
   * Get line number of the annotation in source class file.
   *
   * @return line number, positive one
   */
  public long getLine() {
    return this.line;
  }

  /**
   * Get the base annotated element.
   *
   * @return the annotated element, must not be null
   */
  public Element getElement() {
    return this.element;
  }

  @Override
  public String toString() {
    return "MmdAnnotationWrapper{" + "position=" + this.getPath().toString() + ":" +
        this.getLine() + '}';
  }
}