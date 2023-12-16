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

package com.igormaznitsa.mindmap.swing.panel.ui.gfx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;

public class MMGraphics2DWrapper implements MMGraphics {

  private final Graphics2D delegate;
  private StrokeType strokeType = StrokeType.SOLID;
  private float strokeWidth = 1.0f;

  public MMGraphics2DWrapper(final Graphics2D delegate) {
    this.delegate = delegate;
    this.delegate.setStroke(new BasicStroke(this.strokeWidth));
  }

  public Graphics2D getWrappedGraphics() {
    return this.delegate;
  }

  @Override
  public void setClip(final int x, final int y, final int w, final int h) {
    this.delegate.setClip(x, y, w, h);
  }

  @Override
  public void drawRect(final int x, final int y, final int width, final int height,
                       final Color border, final Color fill) {
    if (fill != null) {
      this.delegate.setColor(fill);
      this.delegate.fillRect(x, y, width, height);
    }

    if (border != null) {
      this.delegate.setColor(border);
      this.delegate.drawRect(x, y, width, height);
    }
  }

  @Override
  public MMGraphics copy() {
    final MMGraphics2DWrapper result = new MMGraphics2DWrapper((Graphics2D) delegate.create());
    result.strokeType = this.strokeType;
    result.strokeWidth = this.strokeWidth;
    return result;
  }

  @Override
  public void dispose() {
    this.delegate.dispose();
  }

  @Override
  public void translate(final double x, final double y) {
    this.delegate.translate(x, y);
  }

  @Override
  public Rectangle getClipBounds() {
    return this.delegate.getClipBounds();
  }

  @Override
  public void setStroke(final float width, final StrokeType type) {
    if (type != this.strokeType || Float.compare(this.strokeWidth, width) != 0) {
      this.strokeType = type;
      this.strokeWidth = width;

      final Stroke stroke;

      switch (type) {
        case SOLID:
          stroke = new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
          break;
        case DASHES:
          stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {width * 3.0f, width}, 0.0f);
          break;
        case DOTS:
          stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {width, width * 2.0f}, 0.0f);
          break;
        default:
          throw new Error("Unexpected stroke type : " + type);
      }
      this.delegate.setStroke(stroke);
    }
  }

  @Override
  public void drawLine(final int startX, final int startY, final int endX, final int endY,
                       final Color color) {
    if (color != null) {
      this.delegate.setColor(color);
      this.delegate.drawLine(startX, startY, endX, endY);
    }
  }

  @Override
  public void draw(final Shape shape, final Color border, final Color fill) {
    if (fill != null) {
      this.delegate.setColor(fill);
      this.delegate.fill(shape);
    }

    if (border != null) {
      this.delegate.setColor(border);
      this.delegate.draw(shape);
    }
  }

  @Override
  public void drawCurve(final double startX, final double startY, final double endX,
                        final double endY, final Color color) {
    final Path2D path = new Path2D.Double();
    path.moveTo(startX, startY);
    path.curveTo(startX, endY, startX, endY, endX, endY);
    if (color != null) {
      this.delegate.setColor(color);
    }
    this.delegate.draw(path);
  }

  @Override
  public void drawOval(final int x, final int y, final int w, final int h, final Color border,
                       final Color fill) {
    if (fill != null) {
      this.delegate.setColor(fill);
      this.delegate.fillOval(x, y, w, h);
    }

    if (border != null) {
      this.delegate.setColor(border);
      this.delegate.drawOval(x, y, w, h);
    }
  }

  @Override
  public void drawImage(final Image image, final int x, final int y) {
    if (image != null) {
      this.delegate.drawImage(image, x, y, null);
    }
  }

  @Override
  public float getFontMaxAscent() {
    return this.delegate.getFontMetrics().getMaxAscent();
  }

  @Override
  public Rectangle2D getStringBounds(final String text) {
    return this.delegate.getFont().getStringBounds(text, this.delegate.getFontRenderContext());
  }

  @Override
  public void setFont(final Font font) {
    this.delegate.setFont(font);
  }

  @Override
  public void drawString(final String text, final int x, final int y, final Color color) {
    if (color != null) {
      this.delegate.setColor(color);
    }
    if (this.delegate.getFont().getSize2D() > 1.0f) {
      this.delegate.drawString(text, x, y);
    }
  }

}
