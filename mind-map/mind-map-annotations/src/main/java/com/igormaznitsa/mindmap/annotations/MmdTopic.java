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

package com.igormaznitsa.mindmap.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allows to mark source elements to generate MMD topic.
 * Multiple annotated elements allowed, they will be grouped as {@link MmdTopics} annotation.
 */
@Retention(RetentionPolicy.SOURCE)
@Repeatable(MmdTopics.class)
@Target({
    ElementType.TYPE,
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.CONSTRUCTOR,
    ElementType.ANNOTATION_TYPE,
    ElementType.PACKAGE,
    ElementType.TYPE_PARAMETER,
    ElementType.PARAMETER,
    ElementType.TYPE_USE,
    ElementType.LOCAL_VARIABLE
})
public @interface MmdTopic {

  /**
   * Allows to provide UID for the topic to be used as identifier in another topics.
   *
   * @return any text UID or empty if not provided.
   * @see MmdTopic#jumpTo()
   */
  String uid() default "";

  /**
   * Identifier of MMD file which should be a parent for the topic.
   *
   * @return MMD file UID or empty one if auto-select allowed.
   * @see MmdFile#uid()
   */
  String fileUid() default "";

  /**
   * Path to the topic in MMD file
   *
   * @return array contains path from root, every path item can be a topic UID or just title text if
   * there is no any topic with such UID.
   * @see #uid()
   */
  String[] path() default {};

  /**
   * Identifier of an emoticon to be added to the generated mind map topic.
   *
   * @return emoticon identifier
   */
  MmdEmoticon emoticon() default MmdEmoticon.EMPTY;

  /**
   * Title for generated topic.
   *
   * @return Text to be used as title for generated topic.
   */
  String title() default "";

  /**
   * File link path to be added into topic.
   *
   * @return file path to be added into topic as file link (can contain line number in format <i>path:line</i>), can be empty if omitted.
   * @see MmdFile#uid()
   */
  String fileLink() default "";

  /**
   * Add the source file with line position as file link but only if file attribute is empty. <b>File link path field has bigger priority.</b>
   *
   * @return true if should autogenerate source file line link, false otherwise
   * @see #fileLink()
   */
  boolean anchor() default true;

  /**
   * Allows to provide jump link to a topic in the same file.
   *
   * @return target topic UID or topic title text.
   * @see #uid()
   */
  String jumpTo() default "";

  /**
   * Allows to add text note for the topic.
   *
   * @return text for topic, empty if there is no note
   */
  String note() default "";

  /**
   * URI to be added into topic.
   *
   * @return URI or empty text if there is no URI
   */
  String uri() default "";

  /**
   * Text color for the topic.
   *
   * @return text color for the topic.
   */
  MmdColor colorText() default MmdColor.Default;

  /**
   * Background fill color for the topic.
   *
   * @return background fill color for the topic.
   */
  MmdColor colorFill() default MmdColor.Default;

  /**
   * Border fill color for the topic.
   *
   * @return border fill color for the topic.
   */
  MmdColor colorBorder() default MmdColor.Default;

  /**
   * Flag to ask topic to be collapsed
   *
   * @return true if topic should be collapsed, false otherwise
   */
  boolean collapse() default false;

  /**
   * Recommended direction for the topic.
   *
   * @return direction which should be used for the topic if it is possible
   */
  Direction direction() default Direction.AUTO;

  /**
   * Preferred order of the topic among sibling topics.
   *
   * @return index of order of the topic among siblings
   * @since 1.6.6
   */
  int order() default -1;

  /**
   * Flag shows that text fields contains some variables in format {@code ${variable.name}} and they should be replaced by values.
   *
   * @return true if text fields of the topic contains variables.
   * @since 1.6.8
   */
  boolean substitute() default false;
}
