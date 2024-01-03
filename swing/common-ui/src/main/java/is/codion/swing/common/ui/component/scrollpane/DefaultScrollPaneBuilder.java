/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.scrollpane;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.LayoutManager;

import static java.util.Objects.requireNonNull;

final class DefaultScrollPaneBuilder extends AbstractComponentBuilder<Void, JScrollPane, ScrollPaneBuilder> implements ScrollPaneBuilder {

  private final JComponent view;

  private int vsbPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
  private int hsbPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
  private boolean wheelScrollingEnabled = true;
  private int verticalUnitIncrement;
  private int horizontalUnitIncrement;
  private int verticalBlockIncrement;
  private int horizontalBlockIncrement;
  private LayoutManager layout;

  DefaultScrollPaneBuilder(JComponent view) {
    this.view = requireNonNull(view);
  }

  @Override
  public ScrollPaneBuilder verticalScrollBarPolicy(int verticalScrollBarPolicy) {
    this.vsbPolicy = verticalScrollBarPolicy;
    return this;
  }

  @Override
  public ScrollPaneBuilder horizontalScrollBarPolicy(int horizontalScrollBarPolicy) {
    this.hsbPolicy = horizontalScrollBarPolicy;
    return this;
  }

  @Override
  public ScrollPaneBuilder verticalUnitIncrement(int verticalUnitIncrement) {
    this.verticalUnitIncrement = verticalUnitIncrement;
    return this;
  }

  @Override
  public ScrollPaneBuilder horizontalUnitIncrement(int horizontalUnitIncrement) {
    this.horizontalUnitIncrement = horizontalUnitIncrement;
    return this;
  }

  @Override
  public ScrollPaneBuilder verticalBlockIncrement(int verticalBlockIncrement) {
    this.verticalBlockIncrement = verticalBlockIncrement;
    return this;
  }

  @Override
  public ScrollPaneBuilder horizontalBlockIncrement(int horizontalBlockIncrement) {
    this.horizontalBlockIncrement = horizontalBlockIncrement;
    return this;
  }

  @Override
  public ScrollPaneBuilder wheelScrollingEnable(boolean wheelScrollingEnabled) {
    this.wheelScrollingEnabled = wheelScrollingEnabled;
    return this;
  }

  @Override
  public ScrollPaneBuilder layout(LayoutManager layout) {
    this.layout = layout;
    return this;
  }

  @Override
  protected JScrollPane createComponent() {
    JScrollPane scrollPane = new JScrollPane(view, vsbPolicy, hsbPolicy);
    scrollPane.setWheelScrollingEnabled(wheelScrollingEnabled);
    if (verticalUnitIncrement > 0) {
      scrollPane.getVerticalScrollBar().setUnitIncrement(verticalUnitIncrement);
    }
    if (horizontalUnitIncrement > 0) {
      scrollPane.getHorizontalScrollBar().setUnitIncrement(horizontalUnitIncrement);
    }
    if (verticalBlockIncrement > 0) {
      scrollPane.getVerticalScrollBar().setBlockIncrement(verticalBlockIncrement);
    }
    if (horizontalBlockIncrement > 0) {
      scrollPane.getHorizontalScrollBar().setBlockIncrement(horizontalBlockIncrement);
    }
    if (layout != null) {
      scrollPane.setLayout(layout);
    }

    return scrollPane;
  }

  @Override
  protected ComponentValue<Void, JScrollPane> createComponentValue(JScrollPane component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JScrollPane");
  }

  @Override
  protected void setInitialValue(JScrollPane component, Void initialValue) {}
}
