/*
 * Copyright (C) 2015-2022 Igor A. Maznitsa
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.igormaznitsa.sciareto.ui;

import static com.igormaznitsa.mindmap.swing.panel.utils.Utils.html2color;
import static com.igormaznitsa.sciareto.preferences.AdditionalPreferences.PROPERTY_TEXT_EDITOR_FONT;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.Assertions;
import com.igormaznitsa.meta.common.utils.IOUtils;
import com.igormaznitsa.mindmap.ide.commons.editors.AbstractNoteEditor;
import com.igormaznitsa.mindmap.ide.commons.editors.AbstractNoteEditorData;
import com.igormaznitsa.mindmap.ide.commons.preferences.ColorSelectButton;
import com.igormaznitsa.mindmap.model.MMapURI;
import com.igormaznitsa.mindmap.model.Topic;
import com.igormaznitsa.mindmap.model.logger.Logger;
import com.igormaznitsa.mindmap.model.logger.LoggerFactory;
import com.igormaznitsa.mindmap.swing.i18n.MmdI18n;
import com.igormaznitsa.mindmap.swing.panel.MindMapPanelConfig;
import com.igormaznitsa.mindmap.swing.panel.utils.Utils;
import com.igormaznitsa.mindmap.swing.services.UIComponentFactoryProvider;
import com.igormaznitsa.sciareto.SciaRetoStarter;
import com.igormaznitsa.sciareto.preferences.PreferencesManager;
import com.igormaznitsa.sciareto.ui.editors.mmeditors.FileEditPanel;
import com.igormaznitsa.sciareto.ui.editors.mmeditors.UriEditPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.apache.commons.lang3.SystemUtils;

public final class UiUtils {

  public static final MMapURI EMPTY_URI;
  private static final Logger LOGGER = LoggerFactory.getLogger(UiUtils.class);

  static {
    try {
      EMPTY_URI = new MMapURI("http://igormaznitsa.com/specialuri#empty"); //NOI18N
    } catch (URISyntaxException ex) {
      throw new Error("Unexpected exception", ex); //NOI18N
    }
  }

  private UiUtils() {
  }

  public static boolean figureOutThatDarkTheme() {
    final Color color = UIManager.getColor("Panel.background"); //NOI18N
    if (color == null) {
      return false;
    } else {
      return calculateBrightness(color) < 150;
    }
  }

  @Nonnull
  public static Image makeWithAlpha(@Nonnull final Image base, final float alpha) {
    final BufferedImage result =
        new BufferedImage(base.getWidth(null), base.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D gfx = result.createGraphics();
    try {
      gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, alpha));
      gfx.drawImage(base, 0, 0, null);
    } finally {
      gfx.dispose();
    }
    return result;
  }

  public static double findDeviceScale(@Nullable final GraphicsDevice device) {
    final AffineTransform transform =
        device == null ? null : device.getDefaultConfiguration().getDefaultTransform();
    return transform == null ? 1.0d : Math.max(transform.getScaleX(), transform.getScaleY());
  }

  @Nullable
  public static String loadUiScaleFactor() {
    String result = PreferencesManager.getInstance().getPreferences()
        .get(SciaRetoStarter.PROPERTY_SCALE_GUI, null);
    if (result != null && !result.matches("\\s*[1-5](?:\\.5)?\\s*")) {
      result = null;
    }
    return result == null ? null : result.trim();
  }

  public static void saveUiScaleFactor(@Nullable final String scaleFactor) {
    final Preferences preferences = PreferencesManager.getInstance().getPreferences();
    if (scaleFactor == null) {
      preferences.remove(SciaRetoStarter.PROPERTY_SCALE_GUI);
    } else {
      preferences.put(SciaRetoStarter.PROPERTY_SCALE_GUI, scaleFactor);
    }

    try {
      preferences.flush();
    } catch (BackingStoreException ex) {
      // ignore
    }
  }

  public static void assertSwingThread() {
    Assertions.assertTrue("Mus be called only from Swing Dispatcher!",
        SwingUtilities.isEventDispatchThread());
  }

  @Nullable
  public static <T> T findComponent(@Nonnull final Container compo, @Nonnull final Class<T> klazz) {
    for (int i = 0; i < compo.getComponentCount(); i++) {
      final Component ch = compo.getComponent(i);
      if (klazz.isAssignableFrom(ch.getClass())) {
        return klazz.cast(ch);
      }
    }
    for (int i = 0; i < compo.getComponentCount(); i++) {
      final Component ch = compo.getComponent(i);
      if (ch instanceof Container) {
        final T result = findComponent((Container) ch, klazz);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  public static boolean closeCurrentDialogWithResult(@Nonnull final Component component,
                                                     @Nullable final Object exitOption) {
    boolean result = false;
    final Window w = SwingUtilities.getWindowAncestor(component);
    if (w instanceof JDialog) {
      final JDialog d = (JDialog) w;
      final JOptionPane optpane = findComponent(d, JOptionPane.class);
      if (optpane != null) {
        optpane.setValue(exitOption);
        w.setVisible(false);
        result = true;
      }
    }
    return result;
  }

  public static void makeOwningDialogResizable(@Nonnull final Component component,
                                               @Nonnull @MustNotContainNull
                                               final Runnable... extraActions) {
    final HierarchyListener listener = new HierarchyListener() {
      @Override
      public void hierarchyChanged(@Nonnull final HierarchyEvent e) {
        final Window window = SwingUtilities.getWindowAncestor(component);
        if (window instanceof Dialog) {
          final Dialog dialog = (Dialog) window;
          if (!dialog.isResizable()) {
            dialog.setResizable(true);
            component.removeHierarchyListener(this);

            for (final Runnable r : extraActions) {
              r.run();
            }
          }
        }
      }
    };
    component.addHierarchyListener(listener);
  }

  @Nonnull
  public static Point getPointForCentering(@Nonnull final Window window) {
    try {
      final Point mousePoint = MouseInfo.getPointerInfo().getLocation();
      final GraphicsDevice[] devices =
          GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

      for (final GraphicsDevice device : devices) {
        final Rectangle bounds = device.getDefaultConfiguration().getBounds();
        if (mousePoint.x >= bounds.x && mousePoint.y >= bounds.y &&
            mousePoint.x <= (bounds.x + bounds.width) &&
            mousePoint.y <= (bounds.y + bounds.height)) {
          int screenWidth = bounds.width;
          int screenHeight = bounds.height;
          int width = window.getWidth();
          int height = window.getHeight();
          return new Point(((screenWidth - width) / 2) + bounds.x,
              ((screenHeight - height) / 2) + bounds.y);
        }
      }
    } catch (final Exception e) {
      LOGGER.error("Can't get point", e); //NOI18N
    }

    final Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
    return new Point((scrSize.width - window.getWidth()) / 2,
        (scrSize.height - window.getHeight()) / 2);
  }

  @Nullable
  @MustNotContainNull
  public static List<File> showSelectAffectedFiles(
      @Nonnull @MustNotContainNull final List<File> files) {
    final FileListPanel panel = new FileListPanel(files);
    if (DialogProviderManager.getInstance().getDialogProvider()
        .msgOkCancel(null, "Affected files", panel)) {
      return panel.getSelectedFiles();
    }
    return null;
  }

  public static int calculateBrightness(@Nonnull final Color color) {
    return (int) Math.sqrt(
        color.getRed() * color.getRed() * .241d
            + color.getGreen() * color.getGreen() * .691d
            + color.getBlue() * color.getBlue() * .068d);
  }

  @Nonnull
  public static Image iconToImage(@Nonnull Component context, @Nullable final Icon icon) {
    if (icon instanceof ImageIcon) {
      return ((ImageIcon) icon).getImage();
    }
    final int width = icon == null ? 16 : icon.getIconWidth();
    final int height = icon == null ? 16 : icon.getIconHeight();
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    if (icon != null) {
      final Graphics g = image.getGraphics();
      try {
        icon.paintIcon(context, g, 0, 0);
      } finally {
        g.dispose();
      }
    }
    return image;
  }

  @Nonnull
  public static Image makeBadgedRightBottom(@Nonnull final Image base, @Nonnull final Image badge) {
    final int width = Math.max(base.getWidth(null), badge.getWidth(null));
    final int height = Math.max(base.getHeight(null), badge.getHeight(null));
    final BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics gfx = result.getGraphics();
    try {
      gfx.drawImage(base, (width - base.getWidth(null)) / 2, (height - base.getHeight(null)) / 2,
          null);
      gfx.drawImage(badge, width - badge.getWidth(null) - 1, height - badge.getHeight(null) - 1,
          null);
    } finally {
      gfx.dispose();
    }
    return result;
  }

  @Nonnull
  public static Image makeBadgedRightTop(@Nonnull final Image base, @Nonnull final Image badge) {
    final int width = Math.max(base.getWidth(null), badge.getWidth(null));
    final int height = Math.max(base.getHeight(null), badge.getHeight(null));
    final BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics gfx = result.getGraphics();
    try {
      gfx.drawImage(base, (width - base.getWidth(null)) / 2, (height - base.getHeight(null)) / 2,
          null);
      gfx.drawImage(badge, width - badge.getWidth(null) - 1, 1, null);
    } finally {
      gfx.dispose();
    }
    return result;
  }

  @Nullable
  public static Image loadIcon(@Nonnull final String name) {
    final InputStream inStream =
        UiUtils.class.getClassLoader().getResourceAsStream("icons/" + name); //NOI18N
    Image result = null;
    if (inStream != null) {
      try {
        result = ImageIO.read(inStream);
      } catch (IOException ex) {
        result = null;
      } finally {
        IOUtils.closeQuietly(inStream);
      }
    }
    return result;
  }

  @Nullable
  public static MMapURI editURI(@Nonnull final String title, @Nullable final MMapURI uri) {
    final UriEditPanel textEditor =
        new UriEditPanel(UIComponentFactoryProvider.findInstance(),
            uri == null ? null : uri.asString(false, false), false);

    if (DialogProviderManager.getInstance().getDialogProvider()
        .msgOkCancel(null, title, textEditor.getPanel())) {
      final String text = textEditor.getText();
      if (text.isEmpty()) {
        return EMPTY_URI;
      }
      try {
        return new MMapURI(text.trim());
      } catch (URISyntaxException ex) {
        DialogProviderManager.getInstance().getDialogProvider()
            .msgError(SciaRetoStarter.getApplicationFrame(),
                String.format(
                    MmdI18n.getInstance().findBundle().getString("NbUtils.errMsgIllegalURI"),
                    text));
        return null;
      }
    } else {
      return null;
    }
  }

  public static boolean msgOkCancel(@Nonnull final String title, @Nonnull final Object component) {
    return JOptionPane.showConfirmDialog(SciaRetoStarter.getApplicationFrame(), component, title,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null) == JOptionPane.OK_OPTION;
  }

  @Nullable
  public static Color extractCommonColorForColorChooserButton(@Nonnull final String colorAttribute,
                                                              @Nonnull @MustNotContainNull
                                                              final Topic[] topics) {
    Color result = null;
    for (final Topic t : topics) {
      final Color color = html2color(t.getAttribute(colorAttribute), false);
      if (result == null) {
        result = color;
      } else if (!result.equals(color)) {
        return ColorSelectButton.DIFF_COLORS;
      }
    }
    return result;
  }

  @Nullable
  public static FileEditPanel.DataContainer editFilePath(@Nonnull final String title,
                                                         @Nullable final File projectFolder,
                                                         @Nullable
                                                         final FileEditPanel.DataContainer data) {
    final FileEditPanel filePathEditor = new FileEditPanel(
        UIComponentFactoryProvider.findInstance(),
        DialogProviderManager.getInstance().getDialogProvider(),
        projectFolder, data);

    FileEditPanel.DataContainer result = null;
    if (DialogProviderManager.getInstance().getDialogProvider()
        .msgOkCancel(SciaRetoStarter.getApplicationFrame(), title, filePathEditor.getPanel())) {
      result = filePathEditor.getData();
      if (!result.isValid()) {
        DialogProviderManager.getInstance().getDialogProvider()
            .msgError(SciaRetoStarter.getApplicationFrame(),
                String.format(
                    SrI18n.getInstance().findBundle()
                        .getString("MMDGraphEditor.editFileLinkForTopic.errorCantFindFile"),
                    result.getFilePathWithLine().getPath()));
        result = null;
      }
    }
    return result;
  }

  @Nullable
  public static AbstractNoteEditorData editText(@Nonnull final String title,
                                                @Nonnull final AbstractNoteEditorData data,
                                                @Nonnull final MindMapPanelConfig config) {
    final AbstractNoteEditor textEditor =
        new AbstractNoteEditor(SciaRetoStarter::getApplicationFrame,
            UIComponentFactoryProvider.findInstance(),
            DialogProviderManager.getInstance().getDialogProvider(),
            data) {
          @Nonnull
          @Override
          protected Font findEditorFont(@Nonnull final Font defaultFont) {
            return config.getOptionalProperty(PROPERTY_TEXT_EDITOR_FONT, DEFAULT_FONT);
          }

          @Nullable
          @Override
          protected Icon findToolbarIconForId(@Nonnull final IconId iconId) {
            switch (iconId) {
              case BROWSE: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("link16.png")));
              case COPY: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("page_copy16.png")));
              case CLEARALL: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("cross16.png")));
              case EXPORT: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("file_save16.png")));
              case IMPORT: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("disk16.png")));
              case PASSWORD_OFF: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("set_password16.png")));
              case PASSWORD_ON: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("set_password16on.png")));
              case REDO: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("redo.png")));
              case UNDO: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("undo.png")));
              case PASTE: return new ImageIcon(Objects.requireNonNull(UiUtils.loadIcon("paste_plain16.png")));
              default: return null;
            }
          }

          @Override
          public void onBrowseUri(@Nonnull final URI uri, final boolean preferInternalBrowser) throws Exception {
            UiUtils.browseURI(uri, false);
          }
        };
    try {
      if (DialogProviderManager.getInstance().getDialogProvider()
          .msgOkCancel(SciaRetoStarter.getApplicationFrame(), title,
              Utils.catchEscInParentDialog(textEditor.getPanel(),
                  DialogProviderManager.getInstance().getDialogProvider(),
                  dialog -> textEditor.isTextChanged(),
                  x -> textEditor.cancel()))) {
        return textEditor.getData();
      } else {
        return null;
      }
    } finally {
      textEditor.dispose();
    }
  }

  @Nullable
  public static String findPathToApplicationBundle() {
    try {
      final Class<?> classFileManager = Class.forName("com.apple.eio.FileManager");
      return (String) classFileManager.getMethod("getPathToApplicationBundle").invoke(null);
    } catch (Exception ex) {
      return null;
    }
  }

  public static void openLocalResourceInDesktop(@Nonnull final String resource) {
    final Runnable runner = () -> {
      if (Desktop.isDesktopSupported()) {
        final Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.OPEN)) {
          try {
            final File resourcePath = new File(resource);
            if (resourcePath.isAbsolute()) {
              desktop.open(resourcePath);
            } else {
              final String normalizedResourcePath = resource.replace('/', File.separatorChar);

              final File codeSourcePath =
                  new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation()
                      .toURI());
              final String applicationBundlePath = findPathToApplicationBundle();

              if (applicationBundlePath == null) {
                desktop.open(new File(codeSourcePath.getParent(), normalizedResourcePath));
              } else {
                desktop.open(new File(
                    applicationBundlePath + "/Contents/Resources/" + normalizedResourcePath));
              }
            }
          } catch (URISyntaxException | IOException | IllegalArgumentException ex) {
            LOGGER.error("Can't open in desktop: " + resource, ex);
          }
        }
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      runner.run();
    } else {
      SwingUtilities.invokeLater(runner);
    }
  }

  public static void openInSystemViewer(@Nonnull final File file) {
    final Runnable startEdit = () -> {
      boolean ok = false;
      if (Desktop.isDesktopSupported()) {
        final Desktop dsk = Desktop.getDesktop();
        if (dsk.isSupported(Desktop.Action.OPEN)) {
          try {
            dsk.open(file);
            ok = true;
          } catch (Throwable ex) {
            LOGGER.error("Can't open file in system viewer : " + file, ex);//NOI18N //NOI18N
          }
        }
      }
      if (!ok) {
        SwingUtilities.invokeLater(() -> {
          DialogProviderManager.getInstance().getDialogProvider()
              .msgError(SciaRetoStarter.getApplicationFrame(),
                  "Can't open file in system viewer! See the log!");//NOI18N
          Toolkit.getDefaultToolkit().beep();
        });
      }
    };
    final Thread thr = new Thread(startEdit, " MMDStartFileEdit");//NOI18N
    thr.setUncaughtExceptionHandler((final Thread t, final Throwable e) -> {
      LOGGER.error("Detected uncaught exception in openInSystemViewer() for file " + file,
          e); //NOI18N
    });

    thr.setDaemon(true);
    thr.start();
  }

  private static void showURL(@Nonnull final URL url) {
    showURLExternal(url);
  }

  private static void showURLExternal(@Nonnull final URL url) {
    if (Desktop.isDesktopSupported()) {
      final Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
          desktop.browse(url.toURI());
        } catch (Exception x) {
          LOGGER.error("Can't browse URL in Desktop", x); //NOI18N
        }
      } else if (SystemUtils.IS_OS_LINUX) {
        final Runtime runtime = Runtime.getRuntime();
        try {
          runtime.exec("xdg-open " + url); //NOI18N
        } catch (IOException e) {
          LOGGER.error("Can't browse URL under Linux", e); //NOI18N
        }
      } else if (SystemUtils.IS_OS_MAC) {
        final Runtime runtime = Runtime.getRuntime();
        try {
          runtime.exec("open " + url); //NOI18N
        } catch (IOException e) {
          LOGGER.error("Can't browse URL on MAC", e); //NOI18N
        }
      }
    }

  }

  public static boolean browseURI(@Nonnull final URI uri,
                                  final boolean preferInsideBrowserIfPossible) {
    try {
      if (preferInsideBrowserIfPossible) {
        showURL(uri.toURL());
      } else {
        showURLExternal(uri.toURL());
      }
      return true;
    } catch (MalformedURLException ex) {
      LOGGER.error("MalformedURLException", ex); //NOI18N
      return false;
    }
  }

  public static final class SplashScreen extends JFrame {

    private static final long serialVersionUID = 2481671028674534278L;

    private final Image image;

    public SplashScreen(
        @Nullable final GraphicsConfiguration gfc,
        @Nonnull final Image image
    ) {
      super(gfc);

      this.setAlwaysOnTop(true);
      this.setUndecorated(true);
      this.setBackground(new Color(0, 0, 0, 0));

      this.image = image;
      final int width = this.image.getWidth(null);
      final int height = this.image.getHeight(null);

      this.setBounds(0, 0, width, height);
      this.setMinimumSize(new Dimension(width, height));
      this.setSize(new Dimension(width, height));
      this.setMaximumSize(new Dimension(width, height));
      this.setPreferredSize(new Dimension(width, height));

      this.setLocation(getPointForCentering(this));
    }

    @Override
    public void paint(final Graphics gfx) {
      final Graphics2D gfx2 = (Graphics2D) gfx;

      gfx2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      gfx2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      gfx2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
          RenderingHints.VALUE_COLOR_RENDER_QUALITY);
      gfx2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
          RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

      gfx2.drawImage(this.image, 0, 0, null);
    }

    @Override
    public boolean isFocusable() {
      return false;
    }
  }

}
