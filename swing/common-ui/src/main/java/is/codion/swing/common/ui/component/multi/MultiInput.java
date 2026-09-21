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
import is.codion.common.utilities.Text;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.model.component.list.SwingFilterListModel;
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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.Format;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.common.i18n.Messages.clear;
import static is.codion.common.i18n.Messages.clearMnemonic;
import static is.codion.common.utilities.Configuration.booleanValue;
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
 * showing how many values have been collected, the button toggling a non-modal, undecorated dialog listing them
 * under the field, the caption as its border's title. The dialog stays when the focus goes elsewhere, so that the
 * values collected remain on display while more are added, follows the field around and goes when the field does.
 * <ul>
 * <li>{@link KeyEvent#VK_INSERT} adds the wrapped component's value to the set and clears the component, as does
 * {@link KeyEvent#VK_ENTER} while the component holds a value, see {@link Builder#addOnEnter(boolean)}; an Enter the
 * field does not use is the wrapped component's.
 * <li>{@link KeyEvent#VK_DOWN} with Alt held, or the button, opens the dialog, the list in it focused, or moves the focus
 * to the list in case the dialog is already open. {@link KeyEvent#VK_UP} with Alt held, or the button, closes it.
 * <li>In the dialog {@link KeyEvent#VK_DELETE} removes the selected values, the Clear button clears the set,
 * {@link KeyEvent#VK_UP} with Alt held moves the focus back to the field, leaving the dialog open, for adding further
 * values, and {@link KeyEvent#VK_ESCAPE} or {@link KeyEvent#VK_ENTER} closes it. Alt held, Down and Up thereby step
 * from the field, into the list, and back out again, opening the dialog on the way in and closing it on the way out.
 * </ul>
 * The value is the set collected, sorted, see {@link Builder#comparator(Comparator)}, plus whatever the wrapped
 * component holds, last, so a single value typed into the component counts without being added. Setting the value
 * sets the collected values and clears the wrapped component. The wrapped component keeps every key of its own.
 * @param <C> the wrapped component type
 * @param <T> the value type
 * @see Components#multiInput()
 */
public final class MultiInput<C extends JComponent, T> extends JPanel {

	/**
	 * Specifies whether the member button is focusable by default.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final PropertyValue<Boolean> BUTTON_FOCUSABLE =
					booleanValue(MultiInput.class.getName() + ".buttonFocusable", false);

	private static final int MAXIMUM_VISIBLE_ROWS = 8;
	private static final int MINIMUM_VISIBLE_ROWS = 3;
	private static final String WIDEST_COUNT = "00";
	private static final int TITLE_MARGIN = 24;

	private final ComponentValue<C, T> componentValue;
	private final SwingFilterListModel<T> members;
	private final @Nullable Format format;
	private final @Nullable String caption;
	private final State enabled = State.state(true);
	private final State present = State.state();
	// The single source of truth for whether the members list is displayed, the members button being
	// a true toggle on it, with every way of closing the list reporting back, see closeMembers()
	private final State membersVisible = State.builder()
					.consumer(this::onMembersVisibleChanged)
					.build();
	private final Control closeMembers = command(this::closeMembers);
	private final JToggleButton membersButton;

	private @Nullable JDialog dialog;
	private @Nullable JList<T> list;
	private @Nullable JComponent membersContent;
	private final FollowField followField = new FollowField();

	private MultiInput(DefaultBuilder<C, T> builder) {
		super(new BorderLayout());
		this.componentValue = builder.componentValue;
		this.format = builder.format;
		this.caption = builder.caption;
		JComponent component = componentValue.component();
		component.setInheritsPopupMenu(true);
		membersButton = createMembersButton(component, builder.buttonFocusable);
		add(component, BorderLayout.CENTER);
		add(membersButton, BorderLayout.EAST);
		addFocusListener(new InputFocusAdapter(component));
		members = SwingFilterListModel.builder()
						.<T>items()
						.comparator(builder.comparator)
						.build();
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
	public void setName(String name) {
		super.setName(name);
		componentValue.component().setName(name);
	}

	@Override
	public void requestFocus() {
		componentValue.component().requestFocus();
	}

	@Override
	public boolean requestFocusInWindow() {
		return componentValue.component().requestFocusInWindow();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (componentValue != null) {
			componentValue.component().updateUI();
		}
		if (membersButton != null) {
			membersButton.updateUI();
		}
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
		 * <p>The default comparator collates strings according to {@link Text#COLLATOR_LOCALE}, compares other
		 * {@link Comparable} values naturally and the rest by their string representation.
		 * @param comparator the comparator to use when sorting member items, null for insertion order
		 * @return this builder instance
		 */
		Builder<C, T> comparator(@Nullable Comparator<T> comparator);

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
		 * @param buttonFocusable true if the members button should be focusable
		 * @return this builder instance
		 * @see #BUTTON_FOCUSABLE
		 */
		Builder<C, T> buttonFocusable(boolean buttonFocusable);

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
						.visibleRowCount(visibleRowCount())
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_DELETE)
										.action(command(this::removeSelected)))
						.name("MultiInput:memberList" + caption())
						.build();
	}

	private int visibleRowCount() {
		return Math.max(MINIMUM_VISIBLE_ROWS, Math.min(MAXIMUM_VISIBLE_ROWS, members.getSize()));
	}

	private JToggleButton createMembersButton(JComponent component, boolean focusable) {
		int height = component.getPreferredSize().height;
		int width = Math.max(height, component.getFontMetrics(component.getFont()).stringWidth(WIDEST_COUNT) + 10);

		return toggleButton()
						.toggle(Control.builder()
										.toggle(membersVisible)
										.enabled(State.and(enabled, present))
										.build())
						.preferredSize(new Dimension(width, height))
						.name("MultiInput:membersButton" + caption())
						.focusable(focusable)
						.build();
	}

	private void bindKeys(JComponent component, boolean addOnEnter) {
		// Ancestor of the focused component: the wrapped component may be a combo box, its editor having the focus
		KeyEvents.builder()
						.keyCode(VK_INSERT)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(command(this::addValue))
						.enable(component);
		// Alt-Down and Alt-Up, the convention for opening and closing a drop-down; not Ctrl-Down, which the condition
		// panel uses for the next operator, and not Escape for closing, which the field may well have other uses for
		KeyEvents.builder()
						.keyCode(VK_DOWN)
						.modifiers(ALT_DOWN_MASK)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(command(this::showOrFocusMembers))
						.enable(component, membersButton);
		KeyEvents.builder()
						.keyCode(VK_UP)
						.modifiers(ALT_DOWN_MASK)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(closeMembers)
						.enable(component, membersButton);
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

	private void removeSelected() {
		FilterListSelection<T> selection = members.selection();
		if (selection.present().is()) {
			int index = selection.index().getOrThrow();
			members.items().remove(selection.items().get());
			if (members.items().size() > 0) {
				selection.index().set(Math.min(index, members.items().size() - 1));
			}
		}
	}

	private void clearMembers() {
		members.items().clear();
		closeMembers();
	}

	private void onMembersVisibleChanged(boolean visible) {
		if (!visible) {
			closeMembers();
		}
		else if (present.is() && enabled.is()) {
			showMembers();
		}
		else {
			// Not to be displayed after all. Reset once this change notification has finished, resetting from
			// within it leaves the members button selected, it being notified of this change, not the reset.
			SwingUtilities.invokeLater(() -> membersVisible.set(false));
		}
	}

	// The list stays when the focus goes elsewhere, so with it already displayed this is the way back into it from the keyboard
	private void showOrFocusMembers() {
		if (dialog != null && list != null) {
			dialog.toFront();
			list.requestFocusInWindow();
		}
		else {
			membersVisible.set(true);
		}
	}

	private void focusField() {
		// Not requestFocusInWindow(), the dialog being the focused window at this point
		componentValue.component().requestFocus();
	}

	private void showMembers() {
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
																		.build())
														.name("MultiInput:clearMembers" + caption())))
						.border(caption == null ? null : createTitledBorder(caption))
						.build();
		membersContent = content;
		dialog = Dialogs.builder()
						.component(content)
						.owner(this)
						.modal(false)
						.resizable(false)
						.disposeOnEscape(true)
						// Under the field, as a drop-down would be, following it around, see FollowField
						.size(dialogSize(content))
						.location(dialogLocation())
						// No title bar and no close button, closed via the members button, Escape, Enter or Alt-Up
						// in the field. It stays when the focus goes elsewhere, the members remaining on display
						// while more are added.
						.undecorated(true)
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ENTER)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(closeMembers))
						// Back to the field, the dialog staying, Alt-Up in the field then closing it
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_UP)
										.modifiers(ALT_DOWN_MASK)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(command(this::focusField)))
						.onShown(this::onMembersShown)
						// Escape, which disposes the dialog without going through closeMembers()
						.onClosed(event -> closeMembers())
						.show();
	}

	private String caption() {
		return caption != null ? ":" + caption : "";
	}

	private Point dialogLocation() {
		Point location = getLocationOnScreen();

		return new Point(location.x, location.y + getHeight());
	}

	private Dimension dialogSize(JComponent content) {
		Dimension size = content.getPreferredSize();
		int titleWidth = caption == null ? 0 : content.getFontMetrics(content.getFont()).stringWidth(caption) + TITLE_MARGIN;

		return new Dimension(Math.max(Math.max(size.width, getWidth()), titleWidth), size.height);
	}

	// Every way of closing the members list ends up here, so that the members button follows
	private void closeMembers() {
		if (dialog != null) {
			JDialog closing = dialog;
			dialog = null;
			followField.uninstall();
			membersContent = null;
			closing.dispose();
		}
		membersVisible.set(false);
	}

	private void onMembersChanged() {
		List<T> included = members.items().included().get();
		present.set(!included.isEmpty());
		if (included.isEmpty() && dialog != null) {
			// Nothing left to display, and the members button is disabled, so it could not be closed from there.
			// Disabled it can not take the focus back either, in case it had it when the list was opened, which would
			// send the focus to the first component in the window, so the wrapped component gets it.
			closeMembers();
			componentValue.component().requestFocusInWindow();
		}
		else if (dialog != null && list != null && list.getVisibleRowCount() != visibleRowCount()) {
			// The list grows and shrinks with its members while displayed, within the row limits
			list.setVisibleRowCount(visibleRowCount());
			// the preferred size of the content is cached, all the way up from the list
			list.invalidate();
			followField.positionMembers();
		}
		membersButton.setText(String.valueOf(included.size()));
		membersButton.setToolTipText(included.isEmpty() ? null : included.stream()
						.map(this::format)
						.collect(Collectors.joining("<br>", "<html>", "</html>")));
	}

	private void onMembersShown(JDialog dialog) {
		followField.install();
		list.setSelectedIndex(0);
		list.requestFocusInWindow();
	}

	private String format(@Nullable T value) {
		if (value == null) {
			return "";
		}

		return format == null ? value.toString() : format.format(value);
	}

	// The list stays when the focus goes elsewhere, so it follows the field around instead, when the field is moved or
	// resized or any of its ancestors are, a table column or the window say, and goes when the field stops showing, a
	// tab being switched say
	private final class FollowField extends ComponentAdapter implements HierarchyListener, HierarchyBoundsListener {

		private void install() {
			addComponentListener(this);
			addHierarchyListener(this);
			addHierarchyBoundsListener(this);
		}

		private void uninstall() {
			removeComponentListener(this);
			removeHierarchyListener(this);
			removeHierarchyBoundsListener(this);
		}

		@Override
		public void hierarchyChanged(HierarchyEvent e) {
			if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
				closeMembers();
			}
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			positionMembers();
		}

		@Override
		public void componentResized(ComponentEvent e) {
			positionMembers();
		}

		@Override
		public void ancestorMoved(HierarchyEvent e) {
			positionMembers();
		}

		@Override
		public void ancestorResized(HierarchyEvent e) {
			positionMembers();
		}

		private void positionMembers() {
			if (dialog != null && membersContent != null && isShowing()) {
				dialog.setBounds(new Rectangle(dialogLocation(), dialogSize(membersContent)));
				dialog.validate();
			}
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

		private @Nullable Comparator<T> comparator = Text.comparator();
		private @Nullable Format format;
		private @Nullable String caption;
		private @Nullable Boolean addOnEnter;
		private boolean buttonFocusable = BUTTON_FOCUSABLE.getOrThrow();

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
		public Builder<C, T> comparator(@Nullable Comparator<T> comparator) {
			this.comparator = comparator;
			return this;
		}

		@Override
		public Builder<C, T> addOnEnter(boolean addOnEnter) {
			this.addOnEnter = addOnEnter;
			return this;
		}

		@Override
		public Builder<C, T> buttonFocusable(boolean buttonFocusable) {
			this.buttonFocusable = buttonFocusable;
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
		protected void enable(ValidationIndicator validationIndicator, MultiInput<C, T> component, ObservableState invalid, ObservableState warned) {
			validationIndicator.enable(component.component(), invalid, warned);
		}

		@Override
		protected void enable(ModifiedIndicator modifiedIndicator, MultiInput<C, T> component, ObservableState modified) {
			modifiedIndicator.enable(component.component(), modified);
		}

		@Override
		protected JComponent input(MultiInput<C, T> component) {
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
		protected void setComponentValue(Set<T> value) {
			MultiInput<C, T> field = super.component();
			field.members.items().set(value);
			// otherwise a value pending in the component would remain, and be part of the value on the next change
			field.componentValue.clear();
		}
	}
}
