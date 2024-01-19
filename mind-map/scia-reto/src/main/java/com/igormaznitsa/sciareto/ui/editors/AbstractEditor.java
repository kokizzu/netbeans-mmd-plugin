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

import com.igormaznitsa.meta.common.interfaces.Disposable;
import com.igormaznitsa.mindmap.model.logger.Logger;
import com.igormaznitsa.mindmap.model.logger.LoggerFactory;
import com.igormaznitsa.mindmap.swing.panel.DialogProvider;
import com.igormaznitsa.mindmap.swing.panel.MindMapPanelConfig;
import com.igormaznitsa.sciareto.SciaRetoStarter;
import com.igormaznitsa.sciareto.preferences.AdditionalPreferences;
import com.igormaznitsa.sciareto.preferences.PreferencesManager;
import com.igormaznitsa.sciareto.ui.DialogProviderManager;
import com.igormaznitsa.sciareto.ui.SrI18n;
import com.igormaznitsa.sciareto.ui.misc.MultiFileContainer;
import com.igormaznitsa.sciareto.ui.tabs.TabProvider;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

public abstract class AbstractEditor implements TabProvider, Disposable {

  private static final Map<String, ImageIcon> ICON_CACHE = new HashMap<>();
  protected final ResourceBundle bundle = SrI18n.getInstance().findBundle();
  protected final Logger logger;
  private final AtomicBoolean disposeFlag = new AtomicBoolean();
  protected MindMapPanelConfig mindMapPanelConfig;

  protected AbstractEditor() {
    super();
    this.logger = LoggerFactory.getLogger(this.getClass());
    this.mindMapPanelConfig = loadMindMapConfigFromPreferences();
  }

  @Nonnull
  protected static MindMapPanelConfig loadMindMapConfigFromPreferences() {
    final MindMapPanelConfig config = new MindMapPanelConfig();
    config.loadFrom(PreferencesManager.getInstance().getPreferences());
    return config;
  }

  @Nonnull
  protected static synchronized ImageIcon loadMenuIcon(@Nonnull final String name) {
    if (ICON_CACHE.containsKey(name)) {
      return ICON_CACHE.get(name);
    } else {
      final ImageIcon loaded =
          new javax.swing.ImageIcon(ClassLoader.getSystemResource("menu_icons/" + name + ".png"));
      ICON_CACHE.put(name, loaded);
      return loaded;
    }
  }

  @Nullable
  public MultiFileContainer.FileItem makeFileItem() throws IOException {
    return null;
  }

  public void restoreFromFileItem(@Nonnull final MultiFileContainer.FileItem fileItem) throws IOException {

  }

  @Override
  public void focusToEditor(final int line) {

  }

  @Override
  public final void updateConfiguration() {
    this.mindMapPanelConfig = loadMindMapConfigFromPreferences();
    this.doUpdateConfiguration();
  }

  public void doUpdateConfiguration() {

  }

  public boolean isZoomable() {
    return true;
  }

  public void doZoomIn() {

  }

  public void doZoomOut() {

  }

  public void doZoomReset() {

  }

  @Override
  public final boolean saveDocumentAs() throws IOException {
    final DialogProvider dialogProvider = DialogProviderManager.getInstance().getDialogProvider();
    final File file = this.getTabTitle().getAssociatedFile();
    File fileToSave = dialogProvider.msgSaveFileDialog(
        SciaRetoStarter.getApplicationFrame(),
        null,
        "save-as",
        this.bundle.getString("editorAbstract.dialogSaveAs.title"),
        file, true, new FileFilter[] {getFileFilter()},
        this.bundle.getString("editorAbstract.dialogSaveAs.approve"));
    if (fileToSave != null) {
      if (!fileToSave.getName().contains(".")) {
        final Boolean result =
            dialogProvider.msgConfirmYesNoCancel(SciaRetoStarter.getApplicationFrame(),
                this.bundle.getString("editorAbstract.dialogAddExtension.title"),
                String.format(this.bundle.getString("editorAbstract.dialogAddExtension.text"),
                    this.getDefaultExtension()));
        if (result == null) {
          return false;
        }
        if (result) {
          fileToSave = new File(fileToSave.getParentFile(),
              fileToSave.getName() + '.' + this.getDefaultExtension());
        }
      }
      this.getTabTitle().setAssociatedFile(fileToSave);
      this.onSelectedSaveDocumentAs(fileToSave);
      this.getTabTitle().setChanged(true);
      return this.saveDocument();
    }
    return false;
  }

  protected boolean isOverwriteAllowed(@Nullable final TextFile textFile) throws IOException {
    return textFile == null
        || textFile.hasSameContent(textFile.getFile())
        || DialogProviderManager.getInstance().getDialogProvider().msgConfirmOkCancel(
        SciaRetoStarter.getApplicationFrame(),
        this.bundle.getString("editorAbstract.msgConfirmOverwrite.title"),
        String.format(this.bundle.getString("editorAbstract.msgConfirmOverwrite.text"),
            textFile.getFile().getName()));
  }

  protected void onSelectedSaveDocumentAs(@Nonnull final File selectedFile) {

  }

  protected boolean isAutoBackupAllowed() {
    return this.mindMapPanelConfig.getOptionalProperty(AdditionalPreferences.PROPERTY_BACKUP_LAST_EDIT_BEFORE_SAVE, true);
  }

  public void deleteBackup() {
    if (this.isEditable() && !this.isDisposed()) {
      final File associatedFile = this.getTabTitle().getAssociatedFile();
      if (isAutoBackupAllowed() && associatedFile != null) {
        TextFileBackup.getInstance().add(new TextFileBackup.BackupContent(associatedFile, null));
      }
    }
  }

  protected void backup(@Nullable final String text) {
    if (this.isEditable() && !this.isDisposed() && text != null) {
        final File associatedFile = this.getTabTitle().getAssociatedFile();
        if (isAutoBackupAllowed() && associatedFile != null) {
          TextFileBackup.getInstance().add(new TextFileBackup.BackupContent(associatedFile, text));
        }
      }
  }

  protected void backup() {
    this.backup(this.getContentAsText());
  }

  @Nullable
  protected abstract String getContentAsText();

  @Nonnull
  public abstract String getDefaultExtension();

  @Nonnull
  public abstract EditorContentType getEditorContentType();

  @Nonnull
  public abstract JComponent getContainerToShow();

  @Override
  public final void dispose() {
    if (disposeFlag.compareAndSet(false, true)) {
      if (this.isEditable()) {
        final File associatedFile = this.getTabTitle().getAssociatedFile();
        if (associatedFile != null) {
          TextFileBackup.getInstance().add(new TextFileBackup.BackupContent(associatedFile, null));
        }
      }
      try {
        final JComponent editComponent = this.getMainComponent();
        if (editComponent instanceof Disposable) {
          ((Disposable) editComponent).dispose();
        }
      } finally {
        this.doDispose();
      }
    }
  }

  protected void doDispose() {
  }

  @Override
  public boolean isDisposed() {
    return this.disposeFlag.get();
  }

  @Nullable
  protected TextFile restoreFromBackup(@Nonnull final File file) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new Error("Must be called only from Swing UI thread");
    }

    final File backupFile = TextFileBackup.findBackupForFile(file);
    if (backupFile == null) {
      return null;
    }

    SciaRetoStarter.disposeSplash();

    if (DialogProviderManager.getInstance().getDialogProvider()
        .msgConfirmYesNo(this.getContainerToShow(),

            this.bundle.getString("editorAbstract.msgRestoreFromBackup.title"),
            String.format(this.bundle.getString("editorAbstract.msgRestoreFromBackup.text"),
                backupFile.getName()))) {
      try {
        final TextFile result = new TextFileBackup.Restored(backupFile).asTextFile();
        DialogProviderManager.getInstance().getDialogProvider().msgWarn(this.getContainerToShow(),
            String.format(
                this.bundle.getString("editorAbstract.msgRestoredFromFileBackup.text"),
                backupFile.getName()
            ));
        return result;
      } catch (IOException ex) {
        DialogProviderManager.getInstance().getDialogProvider().msgError(this.getContainerToShow(),
            String.format(
                this.bundle.getString("editorAbstract.msgCantRestoredFromFileBackup.text"),
                ex.getMessage()));
        return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public boolean showSearchPane(@Nonnull final JPanel searchPanel) {
    return false;
  }
}
