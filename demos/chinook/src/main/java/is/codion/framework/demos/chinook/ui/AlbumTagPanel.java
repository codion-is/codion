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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.state.State;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.List;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.dialog.Dialogs.inputDialog;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static javax.swing.BorderFactory.createEmptyBorder;

final class AlbumTagPanel extends JPanel {

	private final ComponentValue<List<String>, JList<String>> tagsValue;
	private final DefaultListModel<String> tagListModel;
	private final State selectionEmpty = State.state(true);
	private final Control addTagControl = Control.builder(this::addTag)
					.smallIcon(FrameworkIcons.instance().icon(Foundation.PLUS))
					.build();
	private final Control removeTagControl = Control.builder(this::removeTag)
					.smallIcon(FrameworkIcons.instance().icon(Foundation.MINUS))
					.enabled(selectionEmpty.not())
					.build();

	AlbumTagPanel(ComponentValue<List<String>, JList<String>> tagsValue) {
		super(borderLayout());
		this.tagsValue = tagsValue;
		this.tagsValue.component().addListSelectionListener(new UpdateSelectionEmptyState());
		this.tagListModel = (DefaultListModel<String>) tagsValue.component().getModel();
		KeyEvents.builder(KeyEvent.VK_INSERT)
						.action(addTagControl)
						.enable(tagsValue.component());
		KeyEvents.builder(KeyEvent.VK_DELETE)
						.action(removeTagControl)
						.enable(tagsValue.component());
		add(borderLayoutPanel()
						.centerComponent(scrollPane(tagsValue.component())
										.preferredSize(new Dimension(120, 60))
										.build())
						.southComponent(borderLayoutPanel()
										.eastComponent(buttonPanel(Controls.builder()
														.control(addTagControl)
														.control(removeTagControl))
														.buttonBuilder(buttonBuilder ->
																		buttonBuilder.transferFocusOnEnter(true))
														.buttonGap(0)
														.border(createEmptyBorder(0, 0, Layouts.GAP.get(), 0))
														.build())
										.build())
						.build(), BorderLayout.CENTER);
	}

	ComponentValue<List<String>, JList<String>> tagsValue() {
		return tagsValue;
	}

	private void addTag() {
		tagListModel.addElement(inputDialog(stringField().buildValue())
						.owner(this)
						.title(FrameworkMessages.add())
						.show());
	}

	private void removeTag() {
		tagsValue.component().getSelectedValuesList().forEach(tagListModel::removeElement);
	}

	private final class UpdateSelectionEmptyState implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			selectionEmpty.set(tagsValue.component().isSelectionEmpty());
		}
	}
}
