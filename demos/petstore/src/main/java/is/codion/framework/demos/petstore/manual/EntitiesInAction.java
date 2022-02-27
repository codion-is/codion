/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.manual;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petstore.domain.Petstore;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import java.util.List;

import static is.codion.framework.demos.petstore.domain.Petstore.Category;
import static is.codion.framework.demos.petstore.domain.Petstore.Product;

public final class EntitiesInAction {

  public static void main(String[] args) throws DatabaseException {
    // tag::entitiesInAction[]
    EntityConnectionProvider connectionProvider = EntityConnectionProvider.connectionProvider()
            .setDomainClassName(Petstore.class.getName())
            .setClientTypeId("Manual")
            .setUser(User.parse("scott:tiger"));

    Entities entities = connectionProvider.getEntities();

    EntityConnection connection = connectionProvider.getConnection();

    //populate a new category
    Entity insects = entities.builder(Category.TYPE)
            .with(Category.NAME, "Insects")
            .with(Category.DESCRIPTION, "Creepy crawlies")
            .build();

    connection.insert(insects);

    //populate a new product for the insect category
    Entity smallBeetles = entities.builder(Product.TYPE)
            .with(Product.CATEGORY_FK, insects)
            .with(Product.NAME, "Small Beetles")
            .with(Product.DESCRIPTION, "Beetles on the smaller side")
            .build();

    connection.insert(smallBeetles);

    //see what products are available for the Cats category
    Entity categoryCats = connection.selectSingle(Category.NAME, "Cats");

    List<Entity> catProducts = connection.select(Product.CATEGORY_FK, categoryCats);

    catProducts.forEach(System.out::println);
    // end::entitiesInAction[]
  }
}
