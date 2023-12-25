/*
 * Copyright (C) 2015-2022 Igor A. Maznitsa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.igormaznitsa.mindmap.annotations.processor.builder;

import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import com.igormaznitsa.mindmap.annotations.MmdFile;
import com.igormaznitsa.mindmap.annotations.MmdFileRef;
import com.igormaznitsa.mindmap.annotations.MmdFiles;
import com.igormaznitsa.mindmap.annotations.MmdTopic;
import com.igormaznitsa.mindmap.annotations.MmdTopics;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Types;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Auxiliary class contains annotation processing utility methods.
 */
public final class AnnotationUtils {

  private AnnotationUtils() {
  }

  /**
   * Find all corresponding file annotations pointed by file link annotation if target class defined.
   *
   * @param typeUtils       type utilities, can't be null
   * @param fileLinkElement processing file link element, can't be null
   * @return list of found file elements marked by {@link MmdFile}
   * @see MmdFile
   * @see MmdFiles
   */
  public static List<Pair<MmdFile, Element>> findByTargetFile(final Types typeUtils,
                                                              final Pair<MmdFileRef, Element> fileLinkElement) {
    try {
      requireNonNull(fileLinkElement.getKey().target());
      throw new IllegalStateException(
          "Can't get expected MirroredTypeException for element field access");
    } catch (final MirroredTypeException ex) {
      final TypeElement element = (TypeElement) typeUtils.asElement(ex.getTypeMirror());
      if (element.getQualifiedName().toString().equals(MmdFileRef.class.getCanonicalName())) {
        return Collections.emptyList();
      } else {
        final List<Pair<MmdFile, Element>> fileAnnotation =
            findFirstWithAncestors(element, MmdFile.class, typeUtils, true);
        final List<Pair<MmdFiles, Element>> filesAnnotation =
            findFirstWithAncestors(element, MmdFiles.class, typeUtils, true);
        return Stream.concat(
                fileAnnotation.stream(),
                filesAnnotation.stream().flatMap(
                    x -> Arrays.stream(x.getKey().value()).map(file -> Pair.of(file, x.getRight()))))
            .collect(
                Collectors.toList());
      }
    }
  }

  /**
   * Find source line position for element.
   *
   * @param sourcePositions auxiliary utility class, must not be null
   * @param trees           auxiliary utility class, must not be null
   * @param element         element which position should be found
   * @return formed container with URI and line number.
   */
  public static UriLine findPosition(
      final SourcePositions sourcePositions, final Trees trees, final Element element) {
    final TreePath treePath = trees.getPath(element);
    final CompilationUnitTree compilationUnit = treePath.getCompilationUnit();

    final long startPosition = findStartPosition(sourcePositions, trees, element);
    final long lineNumber = compilationUnit.getLineMap().getLineNumber(startPosition);
    return new UriLine(compilationUnit.getSourceFile().toUri(), lineNumber);
  }

  /**
   * Find starting position of element inside file.
   *
   * @param sourcePositions auxiliary utility class, must not be null
   * @param trees           auxiliary utility class, must not be null
   * @param element         element which position should be found
   * @return start position of element inside file or -1 if not found
   * @see javax.tools.Diagnostic#NOPOS
   */
  public static long findStartPosition(
      final SourcePositions sourcePositions, final Trees trees, final Element element) {
    final TreePath treePath = trees.getPath(element);
    final CompilationUnitTree compilationUnit = treePath.getCompilationUnit();
    return sourcePositions.getStartPosition(compilationUnit, treePath.getLeaf());
  }

  /**
   * Find first annotations for element, start of list contain annotations found among enclosing elements, tails contains annotations found among ancestors.
   *
   * @param element        element to find annotations, can be null
   * @param annotationType annotation type, must not be null
   * @param typeUtils      type utils class, must not be null
   * @param <A>            annotation type
   * @return list of annotations and elements found by request, must not be null
   */
  public static <A extends Annotation> List<Pair<A, Element>> findFirstAmongEnclosingAndAncestors(
      final Element element,
      final Class<A> annotationType,
      final Types typeUtils
  ) {
    final List<Pair<A, Element>> result = new ArrayList<>();
    result.addAll(findFirstWithEnclosing(element, annotationType, false));
    result.addAll(findFirstWithAncestors(element, annotationType, typeUtils, false));
    return result;
  }

  /**
   * Find all class or interface elements.
   *
   * @param element element which enclosing elements to find, can be null
   * @return found class and interface elements for element, can't be null
   */
  public static List<Element> findAllTypeElements(final Element element) {
    if (element == null) {
      return List.of();
    }

    List<Element> result = new ArrayList<>();
    if (element.getKind().isInterface() || element.getKind().isClass()) {
      result.add(element);
    }
    result.addAll(findAllTypeElements(element.getEnclosingElement()));
    return result;
  }

  /**
   * Find enclosing type for element (i.e. class or interface)
   *
   * @param element target element, must not be null
   * @return found type element or empty otherwise
   */
  public static Optional<? extends Element> findEnclosingType(final Element element) {
    if (element == null) {
      return Optional.empty();
    }
    if (element.getKind() == ElementKind.MODULE || element.getKind() == ElementKind.PACKAGE) {
      return element.getEnclosedElements().stream()
          .filter(e -> e.getKind().isClass() || e.getKind().isInterface())
          .findFirst();
    } else if (element.getKind().isClass() || element.getKind().isInterface()) {
      return Optional.of(element);
    } else {
      return findEnclosingType(element.getEnclosingElement());
    }
  }

  /**
   * Find first required annotations among enclosing elements.
   *
   * @param element        target element, can be null
   * @param annotationType annotation to find, must not be null
   * @param includeElement flag to include target element into search
   * @param <A>            annotation type
   * @return list of pairs found annotations, must not be null
   */
  public static <A extends Annotation> List<Pair<A, Element>> findFirstWithEnclosing(
      final Element element, final Class<A> annotationType, final boolean includeElement) {
    if (element == null) {
      return List.of();
    }
    final List<A> found =
        includeElement ? Arrays.asList(element.getAnnotationsByType(annotationType)) : List.of();
    if (found.isEmpty()) {
      return findFirstWithEnclosing(element.getEnclosingElement(), annotationType, true);
    } else {
      return found.stream().map(x -> Pair.of(x, element)).collect(Collectors.toList());
    }
  }

  /**
   * Find first required annotations among ancestors.
   *
   * @param element        target element, can be null
   * @param annotationType annotation to find, must not be null
   * @param includeElement flag to include target element into search
   * @param <A>            annotation type
   * @return list of pairs found annotations, must not be null
   */
  public static <A extends Annotation> List<Pair<A, Element>> findFirstWithAncestors(
      final Element element,
      final Class<A> annotationType,
      final Types typeUtils,
      final boolean includeElement) {
    if (element == null) {
      return List.of();
    }
    if (includeElement) {
      final A[] found = element.getAnnotationsByType(annotationType);
      if (found.length > 0) {
        return Stream.of(found)
            .map(x -> Pair.of(x, element))
            .collect(Collectors.toList());
      }
    }

    final List<Element> superElements = findAllTypeElements(element)
        .stream()
        .flatMap(x -> {
          try {
            return typeUtils.directSupertypes(x.asType()).stream();
          } catch (IllegalArgumentException ex) {
            return Stream.empty();
          }
        })
        .map(typeUtils::asElement)
        .collect(Collectors.toList());

    return superElements.stream()
        .map(x -> findFirstWithAncestors(x, annotationType, typeUtils, true))
        .filter(x -> !x.isEmpty())
        .findFirst()
        .orElse(List.of());
  }

  /**
   * Find all MmdTopic annotations inside executable element.
   *
   * @param trees             auxiliary utility class, must not be null
   * @param executableElement scanned executable element
   * @return list of pairs for all found MmdTopic annotations and their enclosing elements.
   * @since 1.6.6
   */
  public static List<Map.Entry<MmdTopic, Element>> findAllInternalMmdTopicAnnotations(
      final Trees trees,
      final ExecutableElement executableElement) {
    final CompilationUnitTree compilationUnitTree =
        trees.getPath(executableElement).getCompilationUnit();

    final List<Map.Entry<MmdTopic, Element>> result = new ArrayList<>();
    final MethodTree methodTree = trees.getTree(executableElement);
    methodTree.getBody().getStatements()
        .forEach(statement -> result.addAll(
            extractMmdTopicAnnotationsFromTree(compilationUnitTree, trees, statement)));
    return result;
  }

  private static List<Map.Entry<MmdTopic, Element>> extractMmdTopicAnnotationsFromTree(
      final CompilationUnitTree compilationUnitTree,
      final Trees trees,
      final StatementTree statementTree) {

    final AtomicReference<CompilationUnitTree> compilationUnitTreeRef =
        new AtomicReference<>(compilationUnitTree);

    final TreeScanner<List<Map.Entry<MmdTopic, Element>>, Void> scanner =
        new TreeScanner<>() {

          @Override
          public List<Map.Entry<MmdTopic, Element>> visitCompilationUnit(
              final CompilationUnitTree node,
              final Void unused) {
            compilationUnitTreeRef.set(node);
            return List.of();
          }

          @Override
          public List<Map.Entry<MmdTopic, Element>> visitVariable(
              final VariableTree node, final Void unused) {
            final TreePath treePath = trees.getPath(compilationUnitTreeRef.get(), node);
            final Element variableElement = trees.getElement(treePath);
            return
                concat(
                    Arrays.stream(variableElement.getAnnotationsByType(MmdTopics.class))
                        .flatMap(x -> Arrays.stream(x.value())),
                    Arrays.stream(variableElement.getAnnotationsByType(MmdTopic.class)))
                    .map(annotation -> entry(annotation, variableElement))
                    .collect(toList());
          }

          @Override
          public List<Map.Entry<MmdTopic, Element>> reduce(
              List<Map.Entry<MmdTopic, Element>> r1,
              List<Map.Entry<MmdTopic, Element>> r2) {
            if (r1 == null) {
              return r2;
            }
            if (r2 == null) {
              return r1;
            }
            return concat(r1.stream(), r2.stream()).collect(toList());
          }
        };
    return Objects.requireNonNullElse(scanner.scan(statementTree, null), List.of());
  }

  /**
   * Auxiliary container class to keep information about line and sources.
   */
  public static final class UriLine {
    private final URI uri;
    private final long line;

    private UriLine(final URI uri, final long line) {
      this.uri = requireNonNull(uri);
      this.line = line;
    }

    public URI getUri() {
      return this.uri;
    }

    public long getLine() {
      return this.line;
    }
  }

}
