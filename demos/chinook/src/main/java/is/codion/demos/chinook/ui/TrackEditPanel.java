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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;

import static is.codion.demos.chinook.domain.api.Chinook.*;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_UP;

public final class TrackEditPanel extends EntityEditPanel {

	private final SwingEntityTableModel tableModel;

	public TrackEditPanel(SwingEntityEditModel editModel, SwingEntityTableModel tableModel) {
		super(editModel);
		this.tableModel = tableModel;
		addKeyEvents();
	}

	@Override
	protected void initializeUI() {
		initialFocusAttribute().set(Track.ALBUM_FK);

		createForeignKeySearchField(Track.ALBUM_FK);
		createTextField(Track.NAME)
						.columns(12);
		createForeignKeyComboBoxPanel(Track.MEDIATYPE_FK, this::createMediaTypeEditPanel)
						.preferredWidth(160)
						.includeAddButton(true)
						.includeEditButton(true);
		createForeignKeyComboBoxPanel(Track.GENRE_FK, this::createGenreEditPanel)
						.preferredWidth(160)
						.includeAddButton(true)
						.includeEditButton(true);
		createTextFieldPanel(Track.COMPOSER)
						.columns(12);

		DurationComponentValue durationValue = createDurationValue();
		component(Track.MILLISECONDS).set(durationValue.component());

		createIntegerField(Track.BYTES)
						.columns(6);
		createIntegerSpinner(Track.RATING)
						.columns(2);
		createTextField(Track.UNITPRICE)
						.columns(4);

		JPanel genreMediaTypePanel = gridLayoutPanel(1, 2)
						.add(createInputPanel(Track.GENRE_FK))
						.add(createInputPanel(Track.MEDIATYPE_FK))
						.build();

		JPanel durationPanel = gridLayoutPanel(1, 2)
						.add(createInputPanel(Track.BYTES))
						.add(durationValue.component())
						.build();

		JPanel unitPricePanel = borderLayoutPanel()
						.westComponent(createInputPanel(Track.RATING))
						.eastComponent(createInputPanel(Track.UNITPRICE))
						.build();

		setLayout(flexibleGridLayout(4, 2));
		addInputPanel(Track.ALBUM_FK);
		addInputPanel(Track.NAME);
		add(genreMediaTypePanel);
		addInputPanel(Track.COMPOSER);
		add(durationPanel);
		add(unitPricePanel);
	}

	private EntityEditPanel createMediaTypeEditPanel() {
		return new MediaTypeEditPanel(new SwingEntityEditModel(MediaType.TYPE, editModel().connectionProvider()));
	}

	private GenreEditPanel createGenreEditPanel() {
		return new GenreEditPanel(new SwingEntityEditModel(Genre.TYPE, editModel().connectionProvider()));
	}

	private DurationComponentValue createDurationValue() {
		DurationComponentValue durationValue = new DurationComponentValue();
		addValidator(Track.MILLISECONDS, durationValue.component().minutesField);
		addValidator(Track.MILLISECONDS, durationValue.component().secondsField);
		addValidator(Track.MILLISECONDS, durationValue.component().millisecondsField);
		durationValue.link(editModel().value(Track.MILLISECONDS));

		return durationValue;
	}

	private void addKeyEvents() {
		KeyEvents.builder()
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.modifiers(CTRL_DOWN_MASK)
						.keyCode(VK_UP)
						.action(command(this::moveSelectionUp))
						.enable(this)
						.keyCode(VK_DOWN)
						.action(command(this::moveSelectionDown))
						.enable(this);
	}

	private void moveSelectionUp() {
		if (readyForSelectionChange()) {
			tableModel.selection().indexes().decrement();
		}
	}

	private void moveSelectionDown() {
		if (readyForSelectionChange()) {
			tableModel.selection().indexes().increment();
		}
	}

	private boolean readyForSelectionChange() {
		// If the selection is empty
		if (tableModel.selection().isSelectionEmpty()) {
			return true;
		}
		// If the entity is not modified
		if (!editModel().entity().modified().get()) {
			return true;
		}
		// If the current item was modified and
		// successfully updated after user confirmation
		return updateWithConfirmation();
	}
}