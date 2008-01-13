/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class that represents the dependencies a Entity has
 */
public class EntityDependencies {

  private final String entityID;
  private final Set<Dependency> dependencies = new HashSet<Dependency>();

  public EntityDependencies(final String entityID) {
    this.entityID = entityID;
  }

  /**
   * @return Value for property 'entityID'.
   */
  public String getEntityID() {
    return entityID;
  }

  /**
   * @return Value for property 'dependencies'.
   */
  public Set<Dependency> getDependencies() {
    return dependencies;
  }

  public void addDependency(final String entityID, final List<Property> properties) {
    dependencies.add(new Dependency(entityID, properties));
  }

  /**
   * A class representing a dependency, an entityID and the properties involved in the reference
   */
  public static class Dependency {

    private final String entityID;
    private final List<Property> dependingProperties;

    public Dependency(final String entityID, final List<Property> dependingProperties) {
      this.entityID = entityID;
      this.dependingProperties = dependingProperties;
    }

    /**
     * @return Value for property 'entityID'.
     */
    public String getEntityID() {
      return entityID;
    }

    /**
     * @return Value for property 'dependingProperties'.
     */
    public List<Property> getDependingProperties() {
      return dependingProperties;
    }
  }
}
