/*
 * Copyright (C) 2015-2024 Igor A. Maznitsa
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

package com.igormaznitsa.mindmap.annotations.processor.exporters;

import static java.util.Objects.requireNonNull;

import com.igormaznitsa.mindmap.model.Extra;
import com.igormaznitsa.mindmap.model.ExtraFile;
import com.igormaznitsa.mindmap.model.MindMap;
import com.igormaznitsa.mindmap.model.Topic;
import com.igormaznitsa.mindmap.plugins.api.AbstractExporter;
import com.igormaznitsa.mindmap.plugins.api.ExternallyExecutedPlugin;
import com.igormaznitsa.mindmap.plugins.api.PluginContext;
import com.igormaznitsa.mindmap.swing.panel.DialogProvider;
import com.igormaznitsa.mindmap.swing.panel.MindMapPanel;
import com.igormaznitsa.mindmap.swing.panel.MindMapPanelConfig;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public abstract class AbstractMmdPanelBasedExporter implements MindMapBinExporter {
  protected final AbstractExporter delegate;
  protected final BaseFolderAwareConverter baseFolderAwareConverter;

  protected AbstractMmdPanelBasedExporter(final AbstractExporter delegate) {
    this.delegate = requireNonNull(delegate);
    this.baseFolderAwareConverter = new BaseFolderAwareConverter(this);
  }

  public AbstractExporter getDelegate() {
    return this.delegate;
  }

  protected AbstractExporter.ExtrasToStringConverter getExtrasStringConverter() {
    return this.baseFolderAwareConverter;
  }

  @Override
  public byte[] export(final Path rootFolder, final Path targetFile, final MindMap map)
      throws IOException {
    final PluginContext context = new PluginContext() {
      @Override
      public MindMapPanelConfig getPanelConfig() {
        return new MindMapPanelConfig();
      }

      @Override
      public MindMap getModel() {
        return map;
      }

      @Override
      public MindMapPanel getPanel() {
        return null;
      }

      @Override
      public DialogProvider getDialogProvider() {
        return null;
      }

      @Override
      public File getProjectFolder() {
        return rootFolder.toFile();
      }

      @Override
      public File getMindMapFile() {
        return targetFile.toFile();
      }

      @Override
      public Topic[] getSelectedTopics() {
        return new Topic[0];
      }

      @Override
      public void openFile(File file, boolean preferSystemBrowser) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void processPluginActivation(ExternallyExecutedPlugin plugin, Topic activeTopic) {
        throw new UnsupportedOperationException();
      }
    };
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    this.delegate.doExport(context, Set.of(), buffer, this.getExtrasStringConverter());
    return buffer.toByteArray();
  }

  protected static final class BaseFolderAwareConverter
      implements AbstractExporter.ExtrasToStringConverter {
    private final AbstractMmdPanelBasedExporter delegatingExporter;

    BaseFolderAwareConverter(final AbstractMmdPanelBasedExporter delegatingExporter) {
      this.delegatingExporter = delegatingExporter;
    }

    @Override
    public String apply(final PluginContext pluginContext, final Extra<?> extra) {
      if (extra.getType() == Extra.ExtraType.FILE) {
        final Path thisFileFolder = pluginContext.getMindMapFile().getParentFile().toPath();
        final Path linkedFile =
            ((ExtraFile) extra).getValue().asFile(pluginContext.getProjectFolder()).toPath();

        try {
          return thisFileFolder.relativize(linkedFile).normalize().toString();
        } catch (IllegalArgumentException ex) {
          // can't relativize
          return linkedFile.toAbsolutePath().toString();
        }

      } else {
        return this.delegatingExporter.getDelegate().getDefaultExtrasStringConverter()
            .apply(pluginContext, extra);
      }
    }
  }
}
