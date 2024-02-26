/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Genre;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.domain.entity.Entity;

import java.util.ArrayList;
import java.util.List;

import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.RANDOM;
import static is.codion.framework.domain.entity.condition.Condition.all;

public final class ViewGenre implements Performer<EntityConnectionProvider> {

  @Override
  public void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();
    List<Entity> genres = connection.select(all(Genre.TYPE));
    List<Entity> tracks = connection.select(Track.GENRE_FK.equalTo(genres.get(RANDOM.nextInt(genres.size()))));
    if (!tracks.isEmpty()) {
      connection.dependencies(new ArrayList<>(tracks.subList(0, Math.min(10, tracks.size()))));
    }
  }
}
