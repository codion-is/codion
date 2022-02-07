/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import java.awt.LayoutManager;

import static java.util.Objects.requireNonNull;

final class DefaultScrollPaneBuilder extends AbstractComponentBuilder<Void, JScrollPane, ScrollPaneBuilder> implements ScrollPaneBuilder {

  private final JComponent view;

  private int vsbPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
  private int hsbPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED;
  private boolean wheelScrollingEnabled = true;
  private LayoutManager layout;

  DefaultScrollPaneBuilder(final JComponent view) {
    this.view = requireNonNull(view);
  }

  @Override
  public ScrollPaneBuilder verticalScrollBarPolicy(final int verticalScrollBarPolicy) {
    this.vsbPolicy = verticalScrollBarPolicy;
    return this;
  }

  @Override
  public ScrollPaneBuilder horizontalScrollBarPolicy(final int horizontalScrollBarPolicy) {
    this.hsbPolicy = horizontalScrollBarPolicy;
    return this;
  }

  @Override
  public ScrollPaneBuilder wheelScrollingEnable(final boolean wheelScrollingEnabled) {
    this.wheelScrollingEnabled = wheelScrollingEnabled;
    return this;
  }

  @Override
  public ScrollPaneBuilder layout(final LayoutManager layout) {
    this.layout = layout;
    return this;
  }

  @Override
  protected JScrollPane buildComponent() {
    final JScrollPane scrollPane = new JScrollPane(view, vsbPolicy, hsbPolicy);
    scrollPane.setWheelScrollingEnabled(wheelScrollingEnabled);
    if (layout != null) {
      scrollPane.setLayout(layout);
    }

    return scrollPane;
  }

  @Override
  protected ComponentValue<Void, JScrollPane> buildComponentValue(final JScrollPane component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JScrollPane");
  }

  @Override
  protected void setInitialValue(final JScrollPane component, final Void initialValue) {}
}
