/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.panel;

import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.ComponentBuilder;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for creating some basic panels.
 */
public final class Panels {

  private static final String CENTER_COMPONENT = "centerComponent";

  private Panels() {}

  /**
   * Creates a JPanel, using a BorderLayout, adding the given components to their respective positions.
   * @param northComponent the component to display in the BorderLayout.NORTH position
   * @param centerComponent the component to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the NORTH an CENTER positions in a BorderLayout
   */
  public static JPanel createNorthCenterPanel(final JComponent northComponent, final JComponent centerComponent) {
    requireNonNull(northComponent, "northComponent");
    requireNonNull(centerComponent, CENTER_COMPONENT);

    return builder(Layouts.borderLayout())
            .add(northComponent, BorderLayout.NORTH)
            .add(centerComponent, BorderLayout.CENTER)
            .build();
  }

  /**
   * Creates a JPanel, using a BorderLayout, adding the given components to their respective positions.
   * @param westComponent the component to display in the BorderLayout.WEST position
   * @param centerComponent the component to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the WEST an CENTER positions in a BorderLayout
   */
  public static JPanel createWestCenterPanel(final JComponent westComponent, final JComponent centerComponent) {
    requireNonNull(westComponent, "westComponent");
    requireNonNull(centerComponent, CENTER_COMPONENT);

    return builder(Layouts.borderLayout())
            .add(westComponent, BorderLayout.WEST)
            .add(centerComponent, BorderLayout.CENTER)
            .build();
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and a non-focusable button based on buttonAction
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonAction the button action
   * @return a panel
   * @see #createEastFocusableButtonPanel(JComponent, Action)
   */
  public static JPanel createEastButtonPanel(final JComponent centerComponent, final Action buttonAction) {
    return createEastButtonPanel(centerComponent, buttonAction, false);
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and a focusable button based on buttonAction
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonAction the button action
   * @return a panel
   */
  public static JPanel createEastFocusableButtonPanel(final JComponent centerComponent, final Action buttonAction) {
    return createEastButtonPanel(centerComponent, buttonAction, true);
  }

  public static PanelBuilder builder(final LayoutManager layoutManager) {
    return new DefaultPanelBuilder(layoutManager);
  }

  private static JPanel createEastButtonPanel(final JComponent centerComponent, final Action buttonAction,
                                              final boolean buttonFocusable) {
    requireNonNull(centerComponent, CENTER_COMPONENT);
    requireNonNull(buttonAction, "buttonAction");
    final JButton button = new JButton(buttonAction);
    button.setPreferredSize(new Dimension(centerComponent.getPreferredSize().height, centerComponent.getPreferredSize().height));
    button.setFocusable(buttonFocusable);

    return builder(new BorderLayout())
            .add(centerComponent, BorderLayout.CENTER)
            .add(button, BorderLayout.EAST)
            .build();
  }

  /**
   * Builds a JPanel instance.
   */
  public interface PanelBuilder extends ComponentBuilder<Void, JPanel, PanelBuilder> {

    /**
     * @param component the component to add
     * @return this builder instance
     */
    PanelBuilder add(JComponent component);

    /**
     * @param component the component to add
     * @param constraints the layout constraints
     * @return this builder instance
     */
    PanelBuilder add(JComponent component, Object constraints);

    /**
     * @return a JPanel based on this builder
     */
    JPanel build();

    /**
     * @param onBuild called after the panel has been built
     * @return a JPanel based on this builder
     */
    JPanel build(Consumer<JPanel> onBuild);
  }

  private static final class DefaultPanelBuilder extends AbstractComponentBuilder<Void, JPanel, PanelBuilder> implements PanelBuilder {

    private final LayoutManager layoutManager;
    private final List<ComponentConstraints> componentConstraints = new ArrayList<>();

    private DefaultPanelBuilder(final LayoutManager layoutManager) {
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
  }

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
