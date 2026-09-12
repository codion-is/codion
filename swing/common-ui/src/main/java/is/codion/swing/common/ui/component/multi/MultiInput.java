/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.multi;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.swing.common.model.component.list.SwingFilterListModel;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.indicator.ModifiedIndicator;
import is.codion.swing.common.ui.component.indicator.ValidationIndicator;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;

import org.jspecify.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.common.i18n.Messages.clear;
import static is.codion.common.i18n.Messages.clearMnemonic;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingUtilities.updateComponentTreeUI;

/**
 * A field collecting a set of values through a wrapped component: the wrapped component, untouched, beside a button
 * showing how many values have been collected, the button opening a non-modal, undecorated dialog listing them
 * under the field, the caption as its border's title, gone when the focus goes elsewhere as a drop-down is.
 * <ul>
 * <li>{@link KeyEvent#VK_INSERT} adds the wrapped component's value to the set and clears the component, as does
 * {@link KeyEvent#VK_ENTER} while the component holds a value, see {@link Builder#addOnEnter(boolean)}; an Enter the
 * field does not use is the wrapped component's.
 * <li>{@link KeyEvent#VK_DOWN} with Alt held, or the button, opens the dialog, the list in it focused.
 * <li>In the dialog {@link KeyEvent#VK_DELETE} removes the selected values, the Clear button clears the set, and
 * {@link KeyEvent#VK_ESCAPE} or {@link KeyEvent#VK_ENTER} closes it.
 * </ul>
 * The value is the set collected plus whatever the wrapped component holds, in the order added, so a single value
 * typed into the component counts without being added. Setting the value sets the collected values and clears
 * the wrapped component. The wrapped component keeps every key of its own.
 * @param <C> the wrapped component type
 * @param <T> the value type
 * @see Components#multiInput()
 */
public final class MultiInput<C extends JComponent, T> extends JPanel {

	private static final int MAXIMUM_VISIBLE_ROWS = 8;
	private static final int MINIMUM_VISIBLE_ROWS = 3;
	private static final String WIDEST_COUNT = "00";
	private static final int TITLE_MARGIN = 24;

	private final ComponentValue<C, T> componentValue;
	private final SwingFilterListModel<T> members = SwingFilterListModel.builder().<T>items().build();
	private final @Nullable Format format;
	private final @Nullable String caption;
	private final State enabled = State.state(true);
	private final State present = State.state();
	private final JButton membersButton;

	private @Nullable JDialog dialog;
	private @Nullable JList<T> list;

	private MultiInput(DefaultBuilder<C, T> builder) {
		super(new BorderLayout());
		this.componentValue = builder.componentValue;
		this.format = builder.format;
		this.caption = builder.caption;
		JComponent component = componentValue.component();
		component.setInheritsPopupMenu(true);
		this.membersButton = createMembersButton(component);
		add(component, BorderLayout.CENTER);
		add(membersButton, BorderLayout.EAST);
		addFocusListener(new InputFocusAdapter(component));
		members.items().included().addListener(this::onMembersChanged);
		onMembersChanged();
		bindKeys(component, builder.addOnEnter == null ? !(component instanceof JComboBox) : builder.addOnEnter);
	}

	/**
	 * @return the wrapped component, the one the values are added through
	 */
	public C component() {
		return componentValue.component();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		this.enabled.set(enabled);
		componentValue.component().setEnabled(enabled);
	}

	@Override
	public void setToolTipText(@Nullable String text) {
		componentValue.component().setToolTipText(text);
	}

	@Override
	public boolean requestFocusInWindow() {
		return componentValue.component().requestFocusInWindow();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (dialog != null) {
			updateComponentTreeUI(dialog);
		}
	}

	@Override
	public void removeNotify() {
		// Taken out of the hierarchy with the dialog open, a condition panel switching operator say: the dialog goes too
		closeMembers();
		super.removeNotify();
	}

	/**
	 * @return a {@link Builder.ComponentStep}
	 */
	public static Builder.ComponentStep builder() {
		return DefaultBuilder.COMPONENT;
	}

	/**
	 * Builds a {@link MultiInput}.
	 * @param <C> the wrapped component type
	 * @param <T> the value type
	 */
	public interface Builder<C extends JComponent, T> extends ComponentValueBuilder<MultiInput<C, T>, Set<T>, Builder<C, T>> {

		/**
		 * @param format formats a value for the dialog and the button's tool tip, null for {@link Object#toString()}
		 * @return this builder instance
		 */
		Builder<C, T> format(@Nullable Format format);

		/**
		 * @param caption the caption of the border around the dialog's list, the caption of what is collected; null for none
		 * @return this builder instance
		 */
		Builder<C, T> caption(@Nullable String caption);

		/**
		 * Whether {@link KeyEvent#VK_ENTER} adds the wrapped component's value while it holds one, leaving the key
		 * to the component while it does not, so that typing, Enter, typing, Enter, Enter collects two values and
		 * then does what Enter does in the component. Default true, unless the wrapped component is a
		 * {@link JComboBox}, whose Enter is its own, the editor a combo box has taking it before the component does.
		 * @param addOnEnter true if Enter should add the value
		 * @return this builder instance
		 */
		Builder<C, T> addOnEnter(boolean addOnEnter);

		/**
		 * Provides a {@link Builder}
		 */
		interface ComponentStep {

			/**
			 * @param component the component the values are added through
			 * @param <C> the wrapped component type
			 * @param <T> the value type
			 * @return a {@link Builder}
			 */
			<C extends JComponent, T> Builder<C, T> component(ComponentValue<C, T> component);
		}
	}

	/**
	 * @return the values collected so far, the dialog's model; the wrapped component's current value is not among them
	 */
	Collection<T> members() {
		return members.items().included().get();
	}

	/**
	 * @return the list the dialog shows the members in, DEL removing the selected
	 */
	FilterList<T> createList() {
		return FilterList.builder()
						.model(members)
						.items()
						.cellRenderer(new Renderer<>(this::format))
						.visibleRowCount(Math.max(MINIMUM_VISIBLE_ROWS, Math.min(MAXIMUM_VISIBLE_ROWS, members.getSize())))
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_DELETE)
										.action(Control.action(e ->
														removeSelected((FilterList<T>) e.getSource()))))
						.build();
	}

	private JButton createMembersButton(JComponent component) {
		int height = component.getPreferredSize().height;
		int width = Math.max(height, component.getFontMetrics(component.getFont()).stringWidth(WIDEST_COUNT) + 10);

		return button()
						.control(Control.builder()
										.command(this::showMembers)
										.enabled(State.and(enabled, present))
										.build())
						.margin(new Insets(0, 2, 0, 2))
						.preferredSize(new Dimension(width, height))
						.build();
	}

	private void bindKeys(JComponent component, boolean addOnEnter) {
		// Ancestor of the focused component: the wrapped component may be a combo box, its editor having the focus
		KeyEvents.builder()
						.keyCode(VK_INSERT)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(command(this::addValue))
						.enable(component);
		// Alt-Down, the convention for opening a drop-down; not Ctrl-Down, which the condition panel uses for the next operator
		KeyEvents.builder()
						.keyCode(VK_DOWN)
						.modifiers(ALT_DOWN_MASK)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(command(this::showMembers))
						.enable(component);
		if (addOnEnter) {
			// A key listener rather than a key binding, so that Enter on an empty component is left unconsumed for
			// whoever binds it further up, the condition panel's refresh say
			component.addKeyListener(new AddOnEnter());
		}
	}

	private void addValue() {
		if (!componentValue.isNull()) {
			T value = componentValue.getOrThrow();
			if (!members.items().contains(value)) {
				members.items().add(value);
			}
			componentValue.clear();
		}
	}

	private void removeSelected(FilterList<T> list) {
		List<T> selected = new ArrayList<>(list.getSelectedValuesList());
		if (!selected.isEmpty()) {
			int index = list.getSelectedIndex();
			members.items().remove(selected);
			if (members.getSize() > 0) {
				list.setSelectedIndex(Math.min(index, members.getSize() - 1));
			}
		}
	}

	private void clearMembers() {
		members.items().clear();
		dialog.dispose();
	}

	private void showMembers() {
		if (!present.is() || !enabled.is()) {
			return;
		}
		if (dialog != null) {
			dialog.toFront();
			return;
		}
		list = createList();
		JPanel content = borderLayoutPanel()
						.center(scrollPane()
										.view(list))
						.south(borderLayoutPanel()
										.east(button()
														.control(Control.builder()
																		.command(this::clearMembers)
																		.caption(clear())
																		.mnemonic(clearMnemonic())
																		.build())))
						.border(caption == null ? null : createTitledBorder(caption))
						.build();
		Point location = getLocationOnScreen();
		dialog = Dialogs.builder()
						.component(content)
						.owner(this)
						.modal(false)
						.resizable(false)
						.disposeOnEscape(true)
						.size(dialogSize(content))
						// Under the field, as a drop-down would be
						.location(new Point(location.x, location.y + getHeight()))
						// No title bar and no close button: like a drop-down, it goes when the focus goes elsewhere
						.undecorated(true)
						.windowFocusListener(new DisposeOnFocusLost())
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ENTER)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(Control.action(e ->
														Ancestor.window().of((JComponent) e.getSource()).dispose())))
						.onShown(this::onMembersShown)
						.onClosed(this::onMembersClosed)
						.show();
	}

	private Dimension dialogSize(JComponent content) {
		Dimension size = content.getPreferredSize();
		int titleWidth = caption == null ? 0 : content.getFontMetrics(content.getFont()).stringWidth(caption) + TITLE_MARGIN;

		return new Dimension(Math.max(Math.max(size.width, getWidth()), titleWidth), size.height);
	}

	private void closeMembers() {
		if (dialog != null) {
			dialog.dispose();
			dialog = null;
		}
	}

	private void onMembersChanged() {
		List<T> included = members.items().included().get();
		present.set(!included.isEmpty());
		membersButton.setText(String.valueOf(included.size()));
		membersButton.setToolTipText(included.isEmpty() ? null : included.stream()
						.map(this::format)
						.collect(Collectors.joining("<br>", "<html>", "</html>")));
	}

	private void onMembersShown(JDialog dialog) {
		membersButton.setSelected(true);
		list.setSelectedIndex(0);
		list.requestFocusInWindow();
	}

	private void onMembersClosed(WindowEvent event) {
		membersButton.setSelected(false);
		dialog = null;
	}

	private String format(@Nullable T value) {
		if (value == null) {
			return "";
		}

		return format == null ? value.toString() : format.format(value);
	}

	private static final class DisposeOnFocusLost extends WindowAdapter {

		@Override
		public void windowLostFocus(WindowEvent e) {
			e.getWindow().dispose();
		}
	}

	private final class AddOnEnter extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			// consumed by the component, which uses the Enter, a search field searching for example
			if (e.getKeyCode() == VK_ENTER && e.getModifiersEx() == 0 && !e.isConsumed() && !componentValue.isNull()) {
				addValue();
				e.consume();
			}
		}
	}

	private static final class InputFocusAdapter extends FocusAdapter {

		private final JComponent component;

		private InputFocusAdapter(JComponent component) {
			this.component = component;
		}

		@Override
		public void focusGained(FocusEvent e) {
			component.requestFocusInWindow();
		}
	}

	private static final class Renderer<T> implements ListCellRenderer<T> {

		private final Function<T, String> formatter;
		private final DefaultListCellRenderer listCellRenderer = new DefaultListCellRenderer();

		private Renderer(Function<T, String> formatter) {
			this.formatter = formatter;
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends T> list, T value,
		                                              int index, boolean isSelected, boolean cellHasFocus) {
			return listCellRenderer.getListCellRendererComponent(list, formatter.apply(value), index, isSelected, cellHasFocus);
		}
	}

	private static final class DefaultComponentStep implements Builder.ComponentStep {

		@Override
		public <C extends JComponent, T> Builder<C, T> component(ComponentValue<C, T> component) {
			return new DefaultBuilder<>(requireNonNull(component));
		}
	}

	private static final class DefaultBuilder<C extends JComponent, T>
					extends AbstractComponentValueBuilder<MultiInput<C, T>, Set<T>, Builder<C, T>>
					implements Builder<C, T> {

		private static final Builder.ComponentStep COMPONENT = new DefaultComponentStep();

		private final ComponentValue<C, T> componentValue;

		private @Nullable Format format;
		private @Nullable String caption;
		private @Nullable Boolean addOnEnter;

		private DefaultBuilder(ComponentValue<C, T> componentValue) {
			this.componentValue = componentValue;
		}

		@Override
		public Builder<C, T> format(@Nullable Format format) {
			this.format = format;
			return this;
		}

		@Override
		public Builder<C, T> caption(@Nullable String caption) {
			this.caption = caption;
			return this;
		}

		@Override
		public Builder<C, T> addOnEnter(boolean addOnEnter) {
			this.addOnEnter = addOnEnter;
			return this;
		}

		@Override
		protected MultiInput<C, T> createComponent() {
			return new MultiInput<>(this);
		}

		@Override
		protected ComponentValue<MultiInput<C, T>, Set<T>> createValue(MultiInput<C, T> component) {
			return new MultiInputValue<>(component);
		}

		@Override
		protected void enable(TransferFocusOnEnter transferFocusOnEnter, MultiInput<C, T> component) {
			transferFocusOnEnter.enable(component.component());
			transferFocusOnEnter.enable(component.membersButton);
		}

		@Override
		protected void enable(ValidationIndicator validationIndicator, MultiInput<C, T> component, ObservableState valid, ObservableState warned) {
			validationIndicator.enable(component.component(), valid, warned);
		}

		@Override
		protected void enable(ModifiedIndicator modifiedIndicator, MultiInput<C, T> component, ObservableState modified) {
			modifiedIndicator.enable(component.component(), modified);
		}

		@Override
		protected JComponent field(MultiInput<C, T> component) {
			return component.component();
		}
	}

	private static final class MultiInputValue<C extends JComponent, T> extends AbstractComponentValue<MultiInput<C, T>, Set<T>> {

		private MultiInputValue(MultiInput<C, T> field) {
			super(field, emptySet());
			field.componentValue.addListener(this::notifyObserver);
			field.members.items().included().addListener(this::notifyObserver);
		}

		@Override
		protected Set<T> getComponentValue() {
			MultiInput<C, T> field = super.component();
			Set<T> values = new LinkedHashSet<>(field.members.items().included().get());
			field.componentValue.optional().ifPresent(values::add);

			return values;
		}

		@Override
		protected void setComponentValue(@Nullable Set<T> value) {
			MultiInput<C, T> field = super.component();
			field.members.items().set(value == null ? emptySet() : value);
			// otherwise a value pending in the component would remain, and be part of the value on the next change
			field.componentValue.clear();
		}
	}
}
