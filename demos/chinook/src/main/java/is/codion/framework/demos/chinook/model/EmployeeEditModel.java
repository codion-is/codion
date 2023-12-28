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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Collection;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

public final class EmployeeEditModel extends SwingEntityEditModel {

  public EmployeeEditModel(EntityConnectionProvider connectionProvider) {
    super(Employee.TYPE, connectionProvider);
  }

  public Insert createInsert() {
    return new Insert();
  }

  public Update createUpdate() {
    return new Update();
  }

  public UpdateMany createUpdate(Collection<Entity> entities) {
    return new UpdateMany(entities);
  }

  public Delete createDelete() {
    return new Delete();
  }

  public DeleteMany createDelete(Collection<Entity> entities) {
    return new DeleteMany(entities);
  }

  public final class Insert {

    public Entity execute() throws ValidationException, DatabaseException {
      sleep();
      Entity entity = entity().copy();
      notifyBeforeInsert(singleton(entity));
      validate();

      return connectionProvider().connection().insertSelect(entity);
    }

    public void onResult(Entity entity) {
      notifyAfterInsert(singleton(entity));
      setDefaults();
    }
  }

  public final class Update {

    public Entity execute() throws ValidationException, DatabaseException {
      sleep();
      Entity entity = entity().copy();
      notifyBeforeUpdate(singletonMap(entity.primaryKey(), entity));
      validate();

      return connectionProvider().connection().updateSelect(entity);
    }

    public void onResult(Entity entity) {
      set(entity);
      notifyAfterUpdate(singletonMap(entity.primaryKey(), entity));
    }
  }

  public final class UpdateMany {

    private final Collection<Entity> entities;

    private UpdateMany(Collection<Entity> entities) {
      this.entities = entities;
    }

    public Collection<Entity> execute() throws ValidationException, DatabaseException {
      sleep();
      notifyBeforeUpdate(Entity.mapToPrimaryKey(entities));
      validate(entities);

      return connectionProvider().connection().updateSelect(entities);
    }

    public void onResult(Collection<Entity> entities) {
      Entity activeEntity = entity();
      entities.stream()
              .filter(entity -> entity.equals(activeEntity))
              .findFirst()
              .ifPresent(EmployeeEditModel.this::set);
      notifyAfterUpdate(Entity.mapToPrimaryKey(entities));
    }
  }

  public final class Delete {

    public Entity execute() throws DatabaseException {
      sleep();
      Entity entity = entity();
      notifyBeforeDelete(singleton(entity));
      connectionProvider().connection().delete(entity.primaryKey());

      return entity;
    }

    public void onResult(Entity entity) {
      setDefaults();
      notifyAfterDelete(singleton(entity));
    }
  }

  public final class DeleteMany {

    private final Collection<Entity> entities;

    private DeleteMany(Collection<Entity> entities) {
      this.entities = entities;
    }

    public Collection<Entity> execute() throws DatabaseException {
      sleep();
      notifyBeforeDelete(entities);
      connectionProvider().connection().delete(Entity.primaryKeys(entities));

      return entities;
    }

    public void onResult(Collection<Entity> entities) {
      if (entities.contains(entity())) {
        setDefaults();
      }
      notifyAfterDelete(entities);
    }
  }

  // simulate a long running task
  private static void sleep() {
    try {
      Thread.sleep(1000);
    }
    catch (InterruptedException ignored) {}
  }
}
