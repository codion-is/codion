/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultPanelBuilder extends AbstractComponentBuilder<Void, JPanel, PanelBuilder> implements PanelBuilder {

  private final LayoutManager layoutManager;
  private final List<ComponentConstraints> componentConstraints = new ArrayList<>();

  DefaultPanelBuilder(final LayoutManager layoutManager) {
    this.layoutManager = requireNonNull(layoutManager);
  }

  @Override
  public PanelBuilder add(final JComponent component) {
    componentConstraints.add(new ComponentConstraints(requireNonNull(component)));
    return this;
  }

  @Override
  public PanelBuilder add(final JComponent component, final Object constraints) {
    componentConstraints.add(new ComponentConstraints(requireNonNull(component), requireNonNull(constraints)));
    return this;
  }

  @Override
  public PanelBuilder add(final JComponent... components) {
    add(Arrays.asList(components));
    return this;
  }

  @Override
  public PanelBuilder add(final Collection<? extends JComponent> components) {
    requireNonNull(components).forEach(this::add);
    return this;
  }

  @Override
  protected JPanel buildComponent() {
    final JPanel panel = new JPanel(layoutManager);
    componentConstraints.forEach(componentConstraint -> {
      if (componentConstraint.constraints != null) {
        panel.add(componentConstraint.component, componentConstraint.constraints);
      }
      else {
        panel.add(componentConstraint.component);
      }
    });

    return panel;
  }

  @Override
  protected ComponentValue<Void, JPanel> buildComponentValue(final JPanel component) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void setInitialValue(final JPanel component, final Void initialValue) {}

  private static final class ComponentConstraints {

    private final JComponent component;
    private final Object constraints;

    private ComponentConstraints(final JComponent component) {
      this(component, null);
    }

    private ComponentConstraints(final JComponent component, final Object constraints) {
      this.component = component;
      this.constraints = constraints;
    }
  }
}
