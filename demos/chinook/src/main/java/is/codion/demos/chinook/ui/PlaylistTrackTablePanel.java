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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.model.condition.ConditionModel;
import is.codion.demos.chinook.domain.api.Chinook.PlaylistTrack;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityConditionComponentFactory;
import is.codion.swing.framework.ui.EntityEditPanel.Confirmer;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.component.EntitySearchField;

import javax.swing.JComponent;

public final class PlaylistTrackTablePanel extends EntityTablePanel {

	public PlaylistTrackTablePanel(SwingEntityTableModel tableModel) {
		// We provide an edit panel to use when adding a new row. The Add action
		// is available via the popup menu, toolbar and keyboard shortcut (INSERT)
		super(tableModel, new PlaylistTrackEditPanel(tableModel.editModel()), config -> config
						// Custom condition component factory for the track condition panel
						.conditionComponentFactory(PlaylistTrack.TRACK_FK,
										new TrackConditionComponentFactory(tableModel.entityDefinition()))
						// Skip confirmation when deleting
						.deleteConfirmer(Confirmer.NONE)
						// No need to edit individual rows, we just add or delete
						.includeEditAttributeControl(false)
						.includeEditControl(false));
		// Hide the playlist column
		table().columnModel().visible(PlaylistTrack.PLAYLIST_FK).set(false);
	}

	@Override
	protected void setupControls() {
		// Modify the ADD control so that it is only enabled when a playlist is selected
		control(ControlKeys.ADD).map(control -> control.copy()
						.enabled(tableModel().editModel().editor().value(PlaylistTrack.PLAYLIST_FK).present())
						.build());
	}

	// A ComponentFactory, which uses the TrackSelectorFactory, displaying
	// a table instead of the default list when selecting tracks
	private static final class TrackConditionComponentFactory extends EntityConditionComponentFactory {

		private TrackConditionComponentFactory(EntityDefinition entityDefinition) {
			super(entityDefinition, PlaylistTrack.TRACK_FK);
		}

		@Override
		public <T> JComponent equal(ConditionModel<T> conditionModel) {
			return EntitySearchField.builder()
							.model(((ForeignKeyConditionModel) conditionModel).equalSearchModel())
							.singleSelection()
							.selector(new TrackSelector())
							.build();
		}

		@Override
		public <T> JComponent in(ConditionModel<T> conditionModel) {
			return EntitySearchField.builder()
							.model(((ForeignKeyConditionModel) conditionModel).inSearchModel())
							.multiSelection()
							.selector(new TrackSelector())
							.build();
		}
	}
}
