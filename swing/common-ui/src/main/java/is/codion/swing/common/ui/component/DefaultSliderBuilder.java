/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

final class DefaultSliderBuilder extends AbstractComponentBuilder<Integer, JSlider, SliderBuilder> implements SliderBuilder {

  private final BoundedRangeModel boundedRangeModel;

  private int minorTickSpacing;
  private int majorTickSpacing;
  private boolean snapToTicks = false;
  private boolean paintTicks = false;
  private boolean paintTrack = true;
  private boolean paintLabels = false;
  private boolean inverted = false;
  private int orientation = SwingConstants.HORIZONTAL;

  DefaultSliderBuilder(final BoundedRangeModel boundedRangeModel, final Value<Integer> linkedValue) {
    super(linkedValue);
    this.boundedRangeModel = requireNonNull(boundedRangeModel);
  }

  @Override
  public SliderBuilder minorTickSpacing(final int minorTickSpacing) {
    this.minorTickSpacing = minorTickSpacing;
    return this;
  }

  @Override
  public SliderBuilder majorTickSpacing(final int majorTickSpacing) {
    this.majorTickSpacing = majorTickSpacing;
    return this;
  }

  @Override
  public SliderBuilder snapToTicks(final boolean snapToTicks) {
    this.snapToTicks = snapToTicks;
    return this;
  }

  @Override
  public SliderBuilder paintTicks(final boolean paintTicks) {
    this.paintTicks = paintTicks;
    return this;
  }

  @Override
  public SliderBuilder paintTrack(final boolean paintTrack) {
    this.paintTrack = paintTrack;
    return this;
  }

  @Override
  public SliderBuilder paintLabels(final boolean paintLabels) {
    this.paintLabels = paintLabels;
    return this;
  }

  @Override
  public SliderBuilder inverted(final boolean inverted) {
    this.inverted = inverted;
    return this;
  }

  @Override
  public SliderBuilder orientation(final int orientation) {
    this.orientation = orientation;
    return this;
  }

  @Override
  protected JSlider buildComponent() {
    final JSlider slider = new JSlider(boundedRangeModel);
    if (minorTickSpacing > 0) {
      slider.setMinorTickSpacing(minorTickSpacing);
    }
    if (majorTickSpacing > 0) {
      slider.setMajorTickSpacing(majorTickSpacing);
    }
    slider.setSnapToTicks(snapToTicks);
    slider.setPaintTicks(paintTicks);
    slider.setPaintTrack(paintTrack);
    slider.setPaintLabels(paintLabels);
    slider.setInverted(inverted);
    slider.setOrientation(orientation);

    return slider;
  }

  @Override
  protected ComponentValue<Integer, JSlider> buildComponentValue(final JSlider component) {
    return ComponentValues.slider(component);
  }

  @Override
  protected void setInitialValue(final JSlider component, final Integer initialValue) {
    component.setValue(initialValue);
  }
}
