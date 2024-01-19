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

package com.igormaznitsa.sciareto.ui.editors;

import com.igormaznitsa.mindmap.print.MMDPrintPanel;
import com.igormaznitsa.mindmap.print.PrintableObject;
import com.igormaznitsa.mindmap.swing.panel.utils.ImageSelection;
import com.igormaznitsa.mindmap.swing.services.UIComponentFactory;
import com.igormaznitsa.mindmap.swing.services.UIComponentFactoryProvider;
import com.igormaznitsa.sciareto.Context;
import com.igormaznitsa.sciareto.SciaRetoStarter;
import com.igormaznitsa.sciareto.ui.DialogProviderManager;
import com.igormaznitsa.sciareto.ui.FindTextScopeProvider;
import com.igormaznitsa.sciareto.ui.ScaleStatusIndicator;
import com.igormaznitsa.sciareto.ui.UiUtils;
import com.igormaznitsa.sciareto.ui.tabs.TabTitle;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.commons.io.FilenameUtils;

public final class PictureViewer extends AbstractEditor {

  //NOI18N
  public static final Set<String> SUPPORTED_FORMATS =
      Set.of("png", //NOI18N
          "jpg", //NOI18N
          "gif", //NOI18N
          "svg");


  private final FileFilter fileFilterImage = new FileFilter() {
    @Override
    public boolean accept(@Nonnull final File f) {
      if (f.isDirectory()) {
        return true;
      }
      final String ext = FilenameUtils.getExtension(f.getName()).toLowerCase(Locale.ENGLISH);
      return SUPPORTED_FORMATS.contains(ext);
    }

    @Override
    @Nonnull
    public String getDescription() {
      return bundle.getString("editorAbstractPlUml.fileFilter.image.description");
    }
  };
  private final TabTitle title;
  private final JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
  private final JScrollPane scrollPane = new EditorScrollPanel();

  private final ScalableImage imageViewer;
  private final ScaleStatusIndicator scaleLabel;
  private final JLabel imageInfoLabel;
  private transient BufferedImage image;

  public PictureViewer(@Nonnull final Context context, @Nonnull final File file)
      throws IOException {
    super();
    this.title = new TabTitle(context, this, file);
    this.imageViewer = new ScalableImage(this.mindMapPanelConfig);
    this.scaleLabel = new ScaleStatusIndicator(this.imageViewer, UiUtils.figureOutThatDarkTheme());

    this.scrollPane.getVerticalScrollBar().setBlockIncrement(ScalableImage.IMG_BLOCK_INCREMENT);
    this.scrollPane.getVerticalScrollBar().setUnitIncrement(ScalableImage.IMG_UNIT_INCREMENT);
    this.scrollPane.getHorizontalScrollBar().setBlockIncrement(ScalableImage.IMG_BLOCK_INCREMENT);
    this.scrollPane.getHorizontalScrollBar().setUnitIncrement(ScalableImage.IMG_UNIT_INCREMENT);

    this.scrollPane.setWheelScrollingEnabled(true);

    final JPanel toolbar = new JPanel(new GridBagLayout());

    final JButton buttonPrintImage = new JButton(loadMenuIcon("printer"));
    buttonPrintImage.setToolTipText("Print image");
    buttonPrintImage.setFocusPainted(false);
    buttonPrintImage.addActionListener(e -> {
      SciaRetoStarter.getApplicationFrame().endFullScreenIfActive();
      final MMDPrintPanel printPanel =
          new MMDPrintPanel(UIComponentFactoryProvider.findInstance(), DialogProviderManager.getInstance().getDialogProvider(), null,
              PrintableObject.newBuild().image(imageViewer.getImage()).build());
      UiUtils.makeOwningDialogResizable(printPanel);
      JOptionPane
          .showMessageDialog(mainPanel, printPanel, "Print image", JOptionPane.PLAIN_MESSAGE);
    });

    final JButton buttonClipboardImage = new JButton(loadMenuIcon("clipboard_image"));
    buttonClipboardImage.setToolTipText(
        this.bundle.getString("editorPictureViewer.buttonClipboardImage.tooltip"));

    buttonClipboardImage.addActionListener(e -> {
      final BufferedImage image = imageViewer.getImage();
      if (image != null) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new ImageSelection(image), null);
      }
    });

    final GridBagConstraints bc = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 1, 1,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

    toolbar.add(buttonClipboardImage, bc);
    toolbar.add(buttonPrintImage, bc);
    this.imageInfoLabel = new JLabel();
    toolbar.add(this.imageInfoLabel, bc);
    bc.weightx = 1000.0d;
    toolbar.add(Box.createHorizontalGlue(), bc);
    bc.weightx = 1.0d;
    toolbar.add(this.scaleLabel, bc);

    this.mainPanel.add(toolbar, BorderLayout.NORTH);
    this.mainPanel.add(this.scrollPane, BorderLayout.CENTER);

    loadContent(file);
  }

  @Nonnull
  private static BufferedImage renderSvg(@Nonnull final File svgFile)
      throws IOException, TranscoderException {
    final TranscodingHints transcoderHints = new TranscodingHints();
    transcoderHints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
    transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI,
        SVGConstants.SVG_NAMESPACE_URI);
    transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
    transcoderHints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION,
        SVGDOMImplementation.getDOMImplementation());

    final AtomicReference<BufferedImage> imagePointer = new AtomicReference<>();
    try (InputStream in = Files.newInputStream(svgFile.toPath())) {
      final TranscoderInput input = new TranscoderInput(in);
      final ImageTranscoder imageTranscoder = new ImageTranscoder() {
        @Override
        public BufferedImage createImage(final int w, final int h) {
          return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(@Nonnull final BufferedImage image,
                               @Nonnull final TranscoderOutput out)
            throws TranscoderException {
          imagePointer.set(image);
        }
      };
      imageTranscoder.setTranscodingHints(transcoderHints);
      imageTranscoder.transcode(input, null);
    }
    return imagePointer.get();
  }

  @Override
  public void doZoomReset() {
    this.scaleLabel.doZoomReset();
  }

  @Override
  public boolean isSelectCommandAllowed(@Nonnull SelectCommand command) {
      return false;
  }

  @Override
  public void doSelectCommand(@Nonnull SelectCommand command) {
  }
  
  @Override
  public void doZoomOut() {
    this.scaleLabel.doZoomOut();
  }

  @Override
  public void doZoomIn() {
    this.scaleLabel.doZoomIn();
  }

  @Nonnull
  @Override
  public String getDefaultExtension() {
    return "png";
  }

  @Override
  public void focusToEditor(final int line) {
  }

  @Override
  @Nonnull
  public FileFilter getFileFilter() {
    return fileFilterImage;
  }

  @Override
  public void loadContent(@Nullable final File file) throws IOException {
    BufferedImage loaded = null;
    if (file != null) {
      try {
        if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".svg")) {
          loaded = renderSvg(file);
        } else {
          loaded = ImageIO.read(file);
        }
      } catch (Exception ex) {
        logger.error("Can't load image", ex); //NOI18N
      }
    }

    this.image = loaded;

    this.imageInfoLabel.setText(String
        .format(
            this.bundle.getString("editorPictureViewer.buttonClipboardImage.imageInfoLabel"),
            this.image.getWidth(null), this.image.getHeight(null)));

    this.imageViewer.setImage(this.image, true);
    this.scrollPane.setViewportView(this.imageViewer);
    this.scrollPane.revalidate();
  }

  @Override
  public boolean saveDocument() throws IOException {
    boolean result = false;
    final File docFile = this.title.getAssociatedFile();
    if (docFile != null) {
      final String ext =
          FilenameUtils.getExtension(docFile.getName()).trim().toLowerCase(Locale.ENGLISH);
      if (SUPPORTED_FORMATS.contains(ext)) {
        try {
          ImageIO.write(this.image, ext, docFile);
          deleteBackup();
          result = true;
        } catch (Exception ex) {
          if (ex instanceof IOException) {
            throw (IOException) ex;
          }
          throw new IOException("Can't write image", ex); //NOI18N
        }
      } else {
        try {
          logger.warn("unsupported image format, will be saved as png : " + ext); //NOI18N
          ImageIO.write(this.image, "png", docFile); //NOI18N
          deleteBackup();
          result = true;
        } catch (Exception ex) {
          if (ex instanceof IOException) {
            throw (IOException) ex;
          }
          throw new IOException("Can't write image", ex); //NOI18N
        }
      }
    }
    return result;
  }

  @Override
  public void doUpdateConfiguration() {
    this.imageViewer.updateConfig(this.mindMapPanelConfig);
    this.scrollPane.revalidate();
    this.scrollPane.repaint();
  }

  @Override
  public boolean isEditable() {
    return false;
  }

  @Override
  public boolean isSavable() {
    return false;
  }

  @Override
  @Nonnull
  public TabTitle getTabTitle() {
    return this.title;
  }

  @Nullable
  @Override
  protected String getContentAsText() {
    return null;
  }

  @Override
  @Nonnull
  public EditorContentType getEditorContentType() {
    return EditorContentType.IMAGE;
  }

  @Override
  @Nonnull
  public JComponent getMainComponent() {
    return this.mainPanel;
  }

  @Override
  @Nonnull
  public JComponent getContainerToShow() {
    return this.mainPanel;
  }

  @Override
  @Nonnull
  public AbstractEditor getEditor() {
    return this;
  }

  @Override
  public boolean isRedo() {
    return false;
  }

  @Override
  public boolean isUndo() {
    return false;
  }

  @Override
  public boolean redo() {
    return false;
  }

  @Override
  public boolean undo() {
    return false;
  }

  @Override
  public boolean findNext(@Nonnull final Pattern pattern,
                          @Nonnull final FindTextScopeProvider provider) {
    return false;
  }

  @Override
  public boolean findPrev(@Nonnull final Pattern pattern,
                          @Nonnull final FindTextScopeProvider provider) {
    return false;
  }

  @Override
  public boolean doesSupportPatternSearch() {
    return false;
  }

  @Override
  public boolean doesSupportCutCopyPaste() {
    return false;
  }

  @Override
  public boolean isCutAllowed() {
    return false;
  }

  @Override
  public boolean doCut() {
    return false;
  }

  @Override
  public boolean isCopyAllowed() {
    return false;
  }

  @Override
  public boolean isPasteAllowed() {
    return false;
  }

  @Override
  public boolean doCopy() {
    return false;
  }

  @Override
  public boolean doPaste() {
    return false;
  }
}
