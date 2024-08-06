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

package com.igormaznitsa.mindmap.plugins.exporters;


import static java.util.Objects.requireNonNull;
import static org.apache.commons.text.StringEscapeUtils.escapeXml10;

import com.igormaznitsa.mindmap.model.Extra;
import com.igormaznitsa.mindmap.model.ExtraFile;
import com.igormaznitsa.mindmap.model.ExtraLink;
import com.igormaznitsa.mindmap.model.ExtraLinkable;
import com.igormaznitsa.mindmap.model.ExtraNote;
import com.igormaznitsa.mindmap.model.ExtraTopic;
import com.igormaznitsa.mindmap.model.MindMap;
import com.igormaznitsa.mindmap.model.Topic;
import com.igormaznitsa.mindmap.plugins.api.AbstractExporter;
import com.igormaznitsa.mindmap.plugins.api.PluginContext;
import com.igormaznitsa.mindmap.plugins.api.parameters.AbstractParameter;
import com.igormaznitsa.mindmap.swing.ide.IDEBridgeFactory;
import com.igormaznitsa.mindmap.swing.panel.MindMapPanelConfig;
import com.igormaznitsa.mindmap.swing.panel.ui.AbstractCollapsableElement;
import com.igormaznitsa.mindmap.swing.panel.utils.MindMapUtils;
import com.igormaznitsa.mindmap.swing.panel.utils.Utils;
import com.igormaznitsa.mindmap.swing.services.IconID;
import com.igormaznitsa.mindmap.swing.services.ImageIconServiceProvider;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;

public class FreeMindExporter extends AbstractExporter {

  private static final Icon ICO = ImageIconServiceProvider.findInstance().getIconForId(IconID.POPUP_EXPORT_FREEMIND);

  private static String generateString(final char chr, final int length) {
    final StringBuilder buffer = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      buffer.append(chr);
    }
    return buffer.toString();
  }

  private static String makeUID(final Topic t) {
    final int[] path = t.getPositionPath();
    final StringBuilder buffer = new StringBuilder("mmlink");
    for (final int i : path) {
      buffer.append('A' + i);
    }
    return buffer.toString();
  }

  private static String escapeXML(final String text) {
    return escapeXml10(text).replace("\n", "&#10;");
  }

  private void writeTopicRecursively(final Topic topic,
                                     final MindMapPanelConfig cfg, int shift,
                                     final State state) {
    final String mainShiftStr = generateString(' ', shift);

//    final Color edge = cfg.getConnectorColor();
    String position = topic.getTopicLevel() == 1 ?
        (AbstractCollapsableElement.isLeftSidedTopic(topic) ? "left" : "right") : "";

    state.append(mainShiftStr)
        .append("<node CREATED=\"")
        .append(System.currentTimeMillis())
        .append("\" MODIFIED=\"")
        .append(System.currentTimeMillis())
        .append("\" COLOR=\"")
        .append(
            requireNonNull(Utils.color2html(MindMapUtils.getTextColor(cfg, topic), false)))
        .append("\" BACKGROUND_COLOR=\"")
        .append(requireNonNull(
            Utils.color2html(MindMapUtils.getBackgroundColor(cfg, topic), false)))
        .append("\" ")
        .append(position.isEmpty() ? " " : String.format("POSITION=\"%s\"", position))
        .append(" ID=\"")
        .append(makeUID(topic))
        .append("\" ")
        .append("TEXT=\"")
        .append(escapeXML(topic.getText()))
        .append("\" ");

    final ExtraFile file = (ExtraFile) this.findExtra(topic, Extra.ExtraType.FILE);
    final ExtraLink link = (ExtraLink) this.findExtra(topic, Extra.ExtraType.LINK);
    final ExtraTopic transition = (ExtraTopic) this.findExtra(topic, Extra.ExtraType.TOPIC);

    final String thelink;

    final List<Extra<?>> extrasToSaveInText = new ArrayList<>();

    // make some prioritization for only attribute
    if (transition != null) {
      thelink = '#' + makeUID(requireNonNull(topic.getMap().findTopicForLink(transition)));
      if (file != null) {
        extrasToSaveInText.add(file);
      }
      if (link != null) {
        extrasToSaveInText.add(link);
      }
    } else if (file != null) {
      thelink = file.getValue().toString();
      if (link != null) {
        extrasToSaveInText.add(link);
      }
    } else if (link != null) {
      thelink = link.getValue().toString();
    } else {
      thelink = "";
    }

    if (!thelink.isEmpty()) {
      state.append(" LINK=\"").append(escapeXML(thelink)).append("\"");
    }
    state.append(">").nextLine();

    shift++;
    final String childShift = generateString(' ', shift);

    state.append(childShift).append("<edge WIDTH=\"thin\"/>");

    final ExtraNote note = (ExtraNote) topic.getExtras().get(Extra.ExtraType.NOTE);

    final StringBuilder htmlTextForNode = new StringBuilder();
    if (!extrasToSaveInText.isEmpty()) {
      htmlTextForNode.append("<ul>");
      for (final Extra<?> e : extrasToSaveInText) {
        htmlTextForNode.append("<li>");
        if (e instanceof ExtraLinkable) {
          final String linkAsText = ((ExtraLinkable) e).getAsURI().asString(true, e.getType() != Extra.ExtraType.FILE);
          htmlTextForNode.append("<b>").append(StringEscapeUtils.escapeHtml3(e.getType().name()))
              .append(": </b>").append("<a href=\"").append(linkAsText).append("\">")
              .append(linkAsText).append("</a>");
        } else {
          htmlTextForNode.append("<b>").append(StringEscapeUtils.escapeHtml3(e.getType().name()))
              .append(": </b>").append(StringEscapeUtils.escapeHtml3(e.getAsString()));
        }
        htmlTextForNode.append("</li>");
      }
      htmlTextForNode.append("</ul>");
    }

    if (note != null) {
      htmlTextForNode.append("<p><pre>").append(StringEscapeUtils.escapeHtml3(note.getValue()))
          .append("</pre></p>");
    }

    if (htmlTextForNode.length() > 0) {
      state.append(childShift).append("<richcontent TYPE=\"NOTE\">")
          .append("<html><head></head><body>" + htmlTextForNode + "</body></html>")
          .append("</richcontent>").nextLine();
    }

    for (final Topic ch : topic.getChildren()) {
      writeTopicRecursively(ch, cfg, shift, state);
    }

    state.append(mainShiftStr).append("</node>").nextLine();
  }

  private String makeContent(final MindMap model, final MindMapPanelConfig config) {
    final State state = new State();
    state.append("<map version=\"1.0.1\">").nextLine();

    state.append("<!--").nextLine()
        .append("Generated by " + IDEBridgeFactory.findInstance().getIDEGeneratorId() + ' ' +
            IDEBridgeFactory.findInstance().getIDEVersion() + " (https://sciareto.org)")
        .nextLine();
    state.append(DateTimeFormatter.ISO_DATE_TIME.format(Instant.now().atZone(ZoneId.systemDefault()))).nextLine().append("-->")
        .nextLine();

    final Topic root = model.getRoot();
    if (root != null) {
      writeTopicRecursively(root, config, 1, state);
    }

    state.append("</map>");

    return state.toString();
  }

  @Override
  public void doExportToClipboard(final PluginContext context, final Set<AbstractParameter<?>> options)
      throws IOException {
    final String text = makeContent(context.getModel(), context.getPanelConfig());

    SwingUtilities.invokeLater(() -> {
      final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      if (clipboard != null) {
        clipboard.setContents(new StringSelection(text), null);
      }
    });
  }

  @Override
  public void doExport(final PluginContext context, final Set<AbstractParameter<?>> options,
                       final OutputStream out) throws IOException {
    final String text = makeContent(context.getModel(), context.getPanelConfig());

    File fileToSaveMap = null;
    OutputStream theOut = out;
    if (theOut == null) {
      fileToSaveMap = MindMapUtils.selectFileToSaveForFileFilter(
          context.getPanel(),
          context,
          this.getClass().getName(),
          this.getResourceBundle().getString("FreeMindExporter.saveDialogTitle"),
          null,
          ".mm",
          this.getResourceBundle().getString("FreeMindExporter.filterDescription"),
          this.getResourceBundle().getString("FreeMindExporter.approveButtonText"));
      fileToSaveMap = MindMapUtils.checkFileAndExtension(context.getPanel(), fileToSaveMap, ".mm");
      theOut = fileToSaveMap == null ? null :
          new BufferedOutputStream(new FileOutputStream(fileToSaveMap, false));
    }
    if (theOut != null) {
      try {
        IOUtils.write(text, theOut, "UTF-8");
      } finally {
        if (fileToSaveMap != null) {
          IOUtils.closeQuietly(theOut);
        }
      }
    }

    if (fileToSaveMap != null) {
      FileUtils.writeStringToFile(fileToSaveMap, text, "UTF-8");
    }
  }

  @Override
  public String getMnemonic() {
    return "freemind";
  }

  @Override
  public String getName(final PluginContext context, final Topic actionTopic) {
    return this.getResourceBundle().getString("FreeMindExporter.exporterName");
  }

  @Override
  public String getReference(final PluginContext context, final Topic actionTopic) {
    return this.getResourceBundle().getString("FreeMindExporter.exporterReference");
  }

  @Override
  public Icon getIcon(final PluginContext context, final Topic actionTopic) {
    return ICO;
  }

  @Override
  public int getOrder() {
    return 1;
  }

  private static class State {

    private static final String NEXT_LINE = "\r\n";
    private final StringBuilder buffer = new StringBuilder(16384);

    public State append(final char ch) {
      this.buffer.append(ch);
      return this;
    }

    public State append(final long val) {
      this.buffer.append(val);
      return this;
    }

    public State append(final String str) {
      this.buffer.append(str);
      return this;
    }

    public State nextLine() {
      this.buffer.append(NEXT_LINE);
      return this;
    }

    @Override
    public String toString() {
      return this.buffer.toString();
    }

  }
}
