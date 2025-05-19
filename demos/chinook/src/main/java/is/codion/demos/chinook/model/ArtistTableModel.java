package is.codion.demos.chinook.model;

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.util.List;

import static is.codion.framework.db.EntityConnection.Update.where;
import static is.codion.framework.db.EntityConnection.transaction;
import static is.codion.framework.domain.entity.Entity.primaryKeys;

public final class ArtistTableModel extends SwingEntityTableModel {

	public ArtistTableModel(EntityConnectionProvider connectionProvider) {
		super(Artist.TYPE, connectionProvider);
	}

	public void combine(List<Entity> artistsToDelete, Entity artistToKeep) {
		EntityConnection connection = connection();
		transaction(connection, () -> {
			connection.update(where(Album.ARTIST_FK.in(artistsToDelete))
							.set(Album.ARTIST_ID, artistToKeep.primaryKey().value())
							.build());
			connection.delete(primaryKeys(artistsToDelete));
		});
	}

	public void onCombined(List<Entity> artistsToDelete, Entity artistToKeep) {
		selection().item().set(artistToKeep);
		items().remove(artistsToDelete);
		EntityEditModel.editEvents().deleted(Artist.TYPE).accept(artistsToDelete);
	}
}
