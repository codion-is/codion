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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.i18n.Messages;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.ListCellRenderer;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.text.TextComponents.preferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

public class DefaultComboBoxBuilder<T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> extends AbstractComponentBuilder<T, C, B>
				implements ComboBoxBuilder<T, C, B> {

	protected final ComboBoxModel<T> comboBoxModel;

	private final List<ItemListener> itemListeners = new ArrayList<>();

	private boolean editable = false;
	private Completion.Mode completionMode = Completion.COMPLETION_MODE.get();
	private Completion.Normalize normalize = Completion.NORMALIZE.get();
	private ListCellRenderer<T> renderer;
	private ComboBoxEditor editor;
	private boolean mouseWheelScrolling = true;
	private boolean mouseWheelScrollingWithWrapAround = false;
	private int maximumRowCount = -1;
	private boolean moveCaretToFrontOnSelection = true;
	private int popupWidth = 0;

	protected DefaultComboBoxBuilder(ComboBoxModel<T> comboBoxModel, Value<T> linkedValue) {
		super(linkedValue);
		this.comboBoxModel = requireNonNull(comboBoxModel);
		value((T) comboBoxModel.getSelectedItem());
		preferredHeight(preferredTextFieldHeight());
		if (comboBoxModel instanceof FilterComboBoxModel) {
			popupMenuControl(comboBox -> Control.builder()
							.command(new RefreshCommand((FilterComboBoxModel<?>) comboBoxModel))
							.name(Messages.refresh())
							.build());
		}
	}

	@Override
	public final B editable(boolean editable) {
		this.editable = editable;
		return self();
	}

	@Override
	public final B completionMode(Completion.Mode completionMode) {
		this.completionMode = requireNonNull(completionMode);
		return self();
	}

	@Override
	public final B normalize(Completion.Normalize normalize) {
		this.normalize = requireNonNull(normalize);
		return self();
	}

	@Override
	public final B renderer(ListCellRenderer<T> renderer) {
		this.renderer = requireNonNull(renderer);
		return self();
	}

	@Override
	public final B editor(ComboBoxEditor editor) {
		this.editor = requireNonNull(editor);
		return self();
	}

	@Override
	public final B mouseWheelScrolling(boolean mouseWheelScrolling) {
		this.mouseWheelScrolling = mouseWheelScrolling;
		if (mouseWheelScrolling) {
			this.mouseWheelScrollingWithWrapAround = false;
		}
		return self();
	}

	@Override
	public final B mouseWheelScrollingWithWrapAround(boolean mouseWheelScrollingWithWrapAround) {
		this.mouseWheelScrollingWithWrapAround = mouseWheelScrollingWithWrapAround;
		if (mouseWheelScrollingWithWrapAround) {
			this.mouseWheelScrolling = false;
		}
		return self();
	}

	@Override
	public final B maximumRowCount(int maximumRowCount) {
		this.maximumRowCount = maximumRowCount;
		return self();
	}

	@Override
	public final B moveCaretToFrontOnSelection(boolean moveCaretToFrontOnSelection) {
		this.moveCaretToFrontOnSelection = moveCaretToFrontOnSelection;
		return self();
	}

	@Override
	public final B popupWidth(int popupWidth) {
		this.popupWidth = popupWidth;
		return self();
	}

	@Override
	public final B itemListener(ItemListener itemListener) {
		this.itemListeners.add(requireNonNull(itemListener));
		return self();
	}

	@Override
	protected final C createComponent() {
		C comboBox = createComboBox();
		if (editable) {
			comboBox.setEditable(true);
		}
		if (!editable && editor == null) {
			Completion.enable(comboBox, completionMode, normalize);
		}
		if (comboBoxModel instanceof FilterComboBoxModel && comboBox.isEditable() && moveCaretToFrontOnSelection) {
			((FilterComboBoxModel<T>) comboBoxModel).selection().item().addConsumer(new MoveCaretToStart<>(comboBox));
		}
		if (renderer != null) {
			comboBox.setRenderer(renderer);
		}
		if (editor != null) {
			comboBox.setEditor(editor);
		}
		if (mouseWheelScrolling) {
			comboBox.addMouseWheelListener(new ComboBoxMouseWheelListener(comboBoxModel, false));
		}
		if (mouseWheelScrollingWithWrapAround) {
			comboBox.addMouseWheelListener(new ComboBoxMouseWheelListener(comboBoxModel, true));
		}
		if (maximumRowCount >= 0) {
			comboBox.setMaximumRowCount(maximumRowCount);
		}
		itemListeners.forEach(new AddItemListener(comboBox));
		if (Utilities.systemOrCrossPlatformLookAndFeelEnabled()) {
			new SteppedComboBoxUI(comboBox, popupWidth);
		}
		comboBox.addPropertyChangeListener("editor", new CopyEditorActionsListener());

		return comboBox;
	}

	@Override
	protected final ComponentValue<T, C> createComponentValue(C component) {
		return new SelectedValue<>(component);
	}

	protected C createComboBox() {
		return (C) new FocusableComboBox<>(comboBoxModel);
	}

	private static final class AddItemListener implements Consumer<ItemListener> {

		private final JComboBox<?> comboBox;

		private AddItemListener(JComboBox<?> comboBox) {
			this.comboBox = comboBox;
		}

		@Override
		public void accept(ItemListener listener) {
			comboBox.addItemListener(listener);
		}
	}

	private static final class RefreshCommand implements Control.Command {

		private final FilterComboBoxModel<?> comboBoxModel;

		private RefreshCommand(FilterComboBoxModel<?> comboBoxModel) {
			this.comboBoxModel = comboBoxModel;
		}

		@Override
		public void execute() {
			comboBoxModel.refresh();
		}
	}
}
