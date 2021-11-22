package com.zextras.team.core.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ITeamRepository { //TODO to be removed

  /**
   * Find an entity using the primary key
   *
   * @param entityClass the entity class type
   * @param primaryKey  the primary key of the entity to find
   * @param <T>         the return type T
   * @return the entity found, null if it not exists
   */
  <T> T find(Class<T> entityClass, Object primaryKey);

  /**
   * Merge the entity
   *
   * @param entity the entity to merge
   * @param <T>    the type T to return
   * @return the merged entity
   */
  <T> T merge(T entity);

  /**
   * Merge the entity list
   *
   * @param entities the entity to merge
   * @param <T>      the type T to return
   * @return the merged entity
   */
  <T> List<T> mergeCollection(Collection<T> entities);

  /**
   * Persist the entity
   *
   * @param entity the entity to persist
   * @param <T>    entity type
   */
  <T> void persist(T entity);

  /**
   * Persist the entity list
   *
   * @param entities the entity to persist
   * @param <T>      entity type
   */
  <T> void persistCollection(Collection<T> entities);

  /**
   * Delete the entity
   *
   * @param entity entity to delete
   * @param <T>    the entity type
   */
  <T> void delete(T entity);

  /**
   * Delete an entity by primary key
   *
   * @param entityClass entity class
   * @param primaryKey  primary key value
   * @param <T>         entity type
   */
  <T> void delete(Class<T> entityClass, Object primaryKey);

  /**
   * Delete an entity
   *
   * @param entity entity to delete
   * @param <T>    entity type
   * @return true if entity has been deleted false otherwise
   */
  <T> boolean deleteAndRetrieve(T entity);

  /**
   * Delete an entity by primary key
   *
   * @param entityClass entity class
   * @param primaryKey  primary key value
   * @param <T>         entity type
   * @return true if entity has been deleted false otherwise
   */
  <T> boolean deleteAndRetrieve(Class<T> entityClass, Object primaryKey);

  /**
   * return the entities list of the result of JPQL script query
   *
   * @param jpql       the JPQL script do execute
   * @param returnType the return type class
   * @param <T>        the class T to return
   * @return the result list
   */
  <T> List<T> getResultListByJpqlQuery(String jpql, Class<T> returnType);

  /**
   * return the entities list of the result of JPQL script query
   *
   * @param jpql       the JPQL script do execute
   * @param returnType the return type class
   * @param parameters the parameters to use
   * @param <T>        the class T to return
   * @return the result list
   */
  <T> List<T> getResultListByJpqlQuery(String jpql, Class<T> returnType, Map<String, Object> parameters);

  /**
   * return the entities list of the result of SQL script query
   *
   * @param sql        the SQL script do execute
   * @param returnType the return type class
   * @param <T>        the class T to return
   * @return the result list
   */
  <T> List<T> getResultListBySQLQuery(String sql, Class<T> returnType);

  /**
   * return the entities list of the result of SQL script query
   *
   * @param sql        the SQL script do execute
   * @param returnType the return type class
   * @param parameters the parameters to use
   * @param <T>        the class T to return
   * @return the result list
   */
  <T> List<T> getResultListBySQLQuery(String sql, Class<T> returnType, Map<String, Object> parameters);

  /**
   * return the entity of the result of SQL script query
   *
   * @param sql        the SQL script do execute
   * @param returnType the return type class
   * @param <T>        the class T to return
   * @return the result list
   */
  <T> T getSingleResultBySQLQuery(String sql, Class<T> returnType);

  /**
   * return the entity of the result of SQL script query
   *
   * @param sql        the SQL script do execute
   * @param returnType the return type class
   * @param parameters the parameters to use
   * @param <T>        the class T to return
   * @return the result list
   */
  <T> T getSingleResultBySQLQuery(String sql, Class<T> returnType, Map<String, Object> parameters);
}
