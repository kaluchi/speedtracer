/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.graphics.client.Canvas;
import com.google.gwt.graphics.client.Color;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.Window;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.visualizations.model.TransientMarkerModel;

/**
 * Marker that represents the current selection.
 * 
 * Draws itself as a leader pointing up and to the right with a label trailing
 * until it approaches the right hand side of the window. At that point, the
 * label points toward the right. If the value being annotated is too high, the
 * label is drawn over the bottom of the map instead of over the top.
 */
public class CurrentSelectionMarker extends TransientMarker {
  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String duration();

    String icon();

    String label();

    int labelHeight();

    String lead();

    int leadHeight();

    int leadWidth();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/CurrentSelectionMarker.css")
    CurrentSelectionMarker.Css currentSelectionMarkerCss();
  }

  private static final int MINIMUM_DURATION_DISPLAY_WIDTH = 1;
  private final Canvas canvas;
  private final Css css;
  private final Element duration;
  private final Element label;
  private final int reverseTippingPoint = 250;

  public CurrentSelectionMarker(Element element,
      TransientMarkerModel markerItem, MainTimeLine mainTimeLine,
      CurrentSelectionMarker.Resources resources) {
    super(element, mainTimeLine, markerItem,
        resources.currentSelectionMarkerCss().icon());
    this.css = resources.currentSelectionMarkerCss();
    canvas = new Canvas(css.leadWidth(), css.leadHeight());
    canvas.getElement().setClassName(css.lead());
    label = DocumentExt.get().createDivWithClassName(css.label());
    duration = DocumentExt.get().createDivWithClassName(css.duration());
    duration.appendChild(canvas.getElement());
    duration.appendChild(label);

    element.appendChild(duration);
  }

  @Override
  public void destroy() {
    getParentElement().removeChild(duration);
    super.destroy();
  }

  @Override
  public void onModelChange(TransientMarkerModel m) {
    super.onModelChange(m);
    canvas.clear();
    if (isVisible() && !m.isNoSelection()) {
      // Compute the duration as pixels, and only display the
      // duration block if it exceeds a minimum size.
      double durationPixels = m.getDuration() * pixelsPerDomain;
      paint((int) durationPixels);
      label.setInnerHTML(m.getDescription());

    } else {
      label.setInnerHTML("");
      duration.getStyle().setProperty("display", "none");
    }
  }

  private void paint(int durationPixels) {
    canvas.setStrokeStyle(Color.BLACK);
    canvas.setLineWidth(2.0);
    canvas.beginPath();

    // Determine which horizontal direction the leader should be drawn.
    int distanceFromRight = Window.getInnerWidth()
        - (getParentElement().getAbsoluteLeft() + xPosition);
    boolean reverseLeader = distanceFromRight < reverseTippingPoint;
    Style canvasStyle = canvas.getElement().getStyle();
    Style labelStyle = label.getStyle();
    Style durationStyle = duration.getStyle();

    // If the duration div runs off of the left, it overwrites the
    // title, so fix that up.
    if (xPosition <= 0) {
      durationPixels += xPosition;
      xPosition = 0;
      durationStyle.setProperty("borderLeft", "none");
    } else {
      durationStyle.setProperty("borderLeft", "1px solid #000");
    }

    // Set the width of the duration div
    if (reverseLeader) {
      durationPixels = Math.min(distanceFromRight, durationPixels);
    }

    durationStyle.setProperty("display", "block");
    durationStyle.setPropertyPx("width", Math.max(durationPixels,
        MINIMUM_DURATION_DISPLAY_WIDTH));

    durationStyle.setPropertyPx("left", xPosition);

    if (reverseLeader) {
      // Close to the right margin, so flip the direction of the leader
      // draw the vertical part on the right side of the canvas.
      labelStyle.setPropertyPx("left",
          -(css.leadWidth() + label.getOffsetWidth()));
      canvasStyle.setPropertyPx("left", -css.leadWidth());
      // Draw the leader
      canvas.moveTo(css.leadWidth(), css.leadHeight());
      canvas.setLineWidth(1.0);
      canvas.lineTo(0, 0);
    } else {
      // Draw the vertical part of the leader on the left side of the canvas.
      // If the xPosition would run into the scale label, move it over a little.
      int labelOffset = css.leadWidth() + ((xPosition <= 20) ? 20 : 0);
      labelStyle.setPropertyPx("left", labelOffset);
      canvasStyle.setPropertyPx("left", 0);
      // Draw the leader
      canvas.moveTo(0, css.leadHeight());
      canvas.setLineWidth(0.5);
      canvas.lineTo(css.leadWidth(), 0);
    }
    canvas.stroke();
  }
}
