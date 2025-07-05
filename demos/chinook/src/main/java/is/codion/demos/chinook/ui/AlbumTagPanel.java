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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.model.filter.FilterModel;
import is.codion.common.state.State;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;

final class AlbumTagPanel extends JPanel {

	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	private final ComponentValue<List<String>, FilterList<String>> tagsValue;
	private final FilterModel.Items<String> tagItems;
	private final State selectionEmpty = State.state(true);
	private final State movingTags = State.state(false);
	private final Control addTagControl = Control.builder()
					.command(this::addTag)
					.smallIcon(ICONS.get(Foundation.PLUS))
					.build();
	private final Control removeTagControl = Control.builder()
					.command(this::removeTag)
					.smallIcon(ICONS.get(Foundation.MINUS))
					.enabled(selectionEmpty.not())
					.build();
	private final Control moveSelectionUpControl = Control.builder()
					.command(this::moveSelectedTagsUp)
					.smallIcon(ICONS.up())
					.enabled(selectionEmpty.not())
					.build();
	private final Control moveSelectionDownControl = Control.builder()
					.command(this::moveSelectedTagsDown)
					.smallIcon(ICONS.down())
					.enabled(selectionEmpty.not())
					.build();

	/**
	 * @param tagsValue the list value providing the list component
	 */
	AlbumTagPanel(ComponentValue<List<String>, FilterList<String>> tagsValue) {
		super(borderLayout());
		this.tagsValue = tagsValue;
		this.tagsValue.component().addListSelectionListener(new UpdateSelectionEmptyState());
		this.tagItems = tagsValue.component().model().items();
		add(createCenterPanel(), BorderLayout.CENTER);
		setupKeyEvents();
	}

	ComponentValue<List<String>, FilterList<String>> tagsValue() {
		return tagsValue;
	}

	private JPanel createCenterPanel() {
		return borderLayoutPanel()
						.centerComponent(scrollPane(tagsValue.component())
										.preferredWidth(120)
										.build())
						.southComponent(borderLayoutPanel()
										.westComponent(createButtonPanel(moveSelectionDownControl, moveSelectionUpControl))
										.eastComponent(createButtonPanel(addTagControl, removeTagControl))
										.build())
						.build();
	}

	private JPanel createButtonPanel(Control leftControl, Control rightControl) {
		return buttonPanel(Controls.builder()
						.control(leftControl)
						.control(rightControl))
						.transferFocusOnEnter(true)
						.buttonGap(0)
						.build();
	}

	private void setupKeyEvents() {
		KeyEvents.builder(VK_INSERT)
						.action(addTagControl)
						.enable(tagsValue.component());
		KeyEvents.builder(VK_DELETE)
						.action(removeTagControl)
						.enable(tagsValue.component());
		KeyEvents.builder(VK_UP)
						.modifiers(CTRL_DOWN_MASK)
						.action(moveSelectionUpControl)
						.enable(tagsValue.component());
		KeyEvents.builder(VK_DOWN)
						.modifiers(CTRL_DOWN_MASK)
						.action(moveSelectionDownControl)
						.enable(tagsValue.component());
	}

	private void addTag() {
		State tagNull = State.state(true);
		tagItems.add(Dialogs.input()
						.component(stringField()
										.consumer(tag -> tagNull.set(tag == null)))
						.owner(this)
						.title(FrameworkMessages.add())
						.valid(tagNull.not())
						.show());
	}

	private void removeTag() {
		tagsValue.component().getSelectedValuesList().forEach(tagItems::remove);
	}

	private void moveSelectedTagsUp() {
		movingTags.set(true);
		try {
			int[] selected = tagsValue.component().getSelectedIndices();
			if (selected.length > 0 && selected[0] != 0) {
				moveTagsUp(selected);
				moveSelectionUp(selected);
			}
		}
		finally {
			movingTags.set(false);
		}
	}

	private void moveSelectedTagsDown() {
		movingTags.set(true);
		try {
			int[] selected = tagsValue.component().getSelectedIndices();
			if (selected.length > 0 && selected[selected.length - 1] != tagItems.visible().count() - 1) {
				moveTagsDown(selected);
				moveSelectionDown(selected);
			}
		}
		finally {
			movingTags.set(false);
		}
	}

	private void moveTagsUp(int[] selected) {
		for (int i = 0; i < selected.length; i++) {
			tagItems.visible().add(selected[i] - 1, tagItems.visible().remove(selected[i]));
		}
	}

	private void moveTagsDown(int[] selected) {
		for (int i = selected.length - 1; i >= 0; i--) {
			tagItems.visible().add(selected[i] + 1, tagItems.visible().remove(selected[i]));
		}
	}

	private void moveSelectionUp(int[] selected) {
		tagsValue.component().setSelectedIndices(Arrays.stream(selected)
						.map(index -> index - 1)
						.toArray());
		tagsValue.component().ensureIndexIsVisible(selected[0] - 1);
	}

	private void moveSelectionDown(int[] selected) {
		tagsValue.component().setSelectedIndices(Arrays.stream(selected)
						.map(index -> index + 1)
						.toArray());
		tagsValue.component().ensureIndexIsVisible(selected[selected.length - 1] + 1);
	}

	private final class UpdateSelectionEmptyState implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (!movingTags.get()) {
				selectionEmpty.set(tagsValue.component().isSelectionEmpty());
			}
		}
	}
}
