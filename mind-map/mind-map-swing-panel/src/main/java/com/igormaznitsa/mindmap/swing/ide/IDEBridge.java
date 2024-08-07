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

package com.igormaznitsa.mindmap.swing.ide;

import com.igormaznitsa.commons.version.Version;
import java.awt.Frame;
import java.awt.Window;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.swing.FocusManager;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/**
 * It describes some bridge to internal features of IDEs.
 */
public interface IDEBridge {
  /**
   * Allows to get the IDE version info.
   *
   * @return the IDE version info as version object, must not be null
   * @since 1.2.0
   */
  Version getIDEVersion();

  /**
   * Get locale for IDE.
   * @return should return currently selected locale for IDE, must not be null.
   * @since 1.6.0
   */
  default Locale getIDELocale(){
    return Locale.getDefault();
  }

  /**
   * Get generator ID for IDE.
   *
   * @return generator ID for the IDE, as string, must not be null
   * @since 1.6.0
   */
  String getIDEGeneratorId();

  /**
   * Show notification in IDE.
   *
   * @param title   the title of notification, must not be null, html tags will not be processed.
   * @param message the message, must not be null. html tags will not be processed
   * @param type    type of the notification, must not be null
   * @since 1.2.0
   */
  void showIDENotification(String title, String message, NotificationType type);

  /**
   * Send request to restart IDE.
   *
   * @since 1.2.0
   */
  void notifyRestart();

  /**
   * Load icon resource through inside tools provided by IDE if it is possible, the class loader of the provided class will be used to find resource.
   *
   * @param path  path to the icon, must not be null
   * @param baseClassToLoad class which class loader will be used for loading of resource, must not be null
   * @return the icon, it must not be null, if it is not found then IDE will process the situation by its inside mechanisms and some runtime exception will be thrown
   * @since 1.2.0
   */
  Icon loadIcon(String path, Class<?> baseClassToLoad);

  /**
   * Allows to make some communication with IDE for request custom objects.
   *
   * @param properties properties for requested object, must not be null
   * @return requested object map, must not be null but can be empty.
   * @see com.igormaznitsa.mindmap.plugins.exporters.SVGImageExporter
   * @since 1.6.0
   */
  default Map<String, Object> lookup(final Map<String, Object> properties) {
    return Collections.emptyMap();
  }

  /**
   * Method allows to get IDE application component if provided.
   *
   * @return IDE application component or component to be recognized as main one for application, can be null.
   * @since 1.6.0
   */
  default JComponent findApplicationComponent() {
    final Window window = FocusManager.getCurrentManager().getActiveWindow();
    if (window instanceof JWindow) {
      return SwingUtilities.getRootPane(window);
    }
    return Stream.of(Frame.getFrames()).findFirst().map(SwingUtilities::getRootPane).orElse(null);
  }
}
