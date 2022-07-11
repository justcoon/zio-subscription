package com.jc.subscription.module.repo

import zio.{UIO, ZIO}
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer

trait Repository[R, ID, E <: Repository.Entity[ID]] {

  def insert(value: E): ZIO[R, Throwable, Boolean]

  def update(value: E): ZIO[R, Throwable, Boolean]

  def delete(id: ID): ZIO[R, Throwable, Boolean]

  def find(id: ID): ZIO[R, Throwable, Option[E]]

  def findAll(): ZIO[R, Throwable, Seq[E]]
}

object Repository {

  trait Entity[ID] {
    def id: ID
  }
}

trait SearchRepository[R, E <: Repository.Entity[_]] {

  def search(
    query: Option[String],
    page: Int,
    pageSize: Int,
    sorts: Iterable[SearchRepository.FieldSort]
  ): ZIO[R, Throwable, SearchRepository.PaginatedSequence[E]]

  def suggest(
    query: String
  ): ZIO[R, Throwable, SearchRepository.SuggestResponse]
}

object SearchRepository {

  final case class FieldSort(property: String, asc: Boolean)

  final case class PaginatedSequence[E](items: Seq[E], page: Int, pageSize: Int, count: Int)

  final case class SuggestResponse(items: Seq[PropertySuggestions])

  final case class TermSuggestion(text: String, score: Double, freq: Int)

  final case class PropertySuggestions(property: String, suggestions: Seq[TermSuggestion])

}

abstract class AbstractCombinedRepository[R, ID, E <: Repository.Entity[ID]]
    extends Repository[R, ID, E] with SearchRepository[R, E] {

  protected def repository: Repository[R, ID, E]
  protected def searchRepository: SearchRepository[R, E]

  override def insert(value: E): ZIO[R, Throwable, Boolean] = repository.insert(value)

  override def update(value: E): ZIO[R, Throwable, Boolean] = repository.update(value)

  override def delete(id: ID): ZIO[R, Throwable, Boolean] = repository.delete(id)

  override def find(id: ID): ZIO[R, Throwable, Option[E]] = repository.find(id)

  override def findAll(): ZIO[R, Throwable, Seq[E]] = repository.findAll()

  override def search(
    query: Option[String],
    page: Int,
    pageSize: Int,
    sorts: Iterable[SearchRepository.FieldSort]): ZIO[R, Throwable, SearchRepository.PaginatedSequence[E]] =
    searchRepository.search(query, page, pageSize, sorts)

  override def suggest(query: String): ZIO[R, Throwable, SearchRepository.SuggestResponse] =
    searchRepository.suggest(query)
}

class InMemoryRepository[R, ID, E <: Repository.Entity[ID]](private val store: ConcurrentHashMap[ID, E])
    extends Repository[R, ID, E] {

  override def insert(value: E): ZIO[R, Throwable, Boolean] = {
    ZIO.succeed {
      val res = Option(store.put(value.id, value))
      res.isDefined
    }
  }

  override def update(value: E): ZIO[R, Throwable, Boolean] = {
    ZIO.succeed {
      val res = Option(store.put(value.id, value))
      res.isDefined
    }
  }

  override def delete(id: ID): ZIO[R, Throwable, Boolean] = {
    ZIO.succeed {
      Option(store.remove(id)).isDefined
    }
  }

  override def find(id: ID): ZIO[R, Throwable, Option[E]] = {
    ZIO.succeed {
      Option(store.get(id))
    }
  }

  override def findAll(): ZIO[R, Throwable, Seq[E]] = {
    import scala.jdk.CollectionConverters._
    ZIO.succeed {
      store.values().asScala.toSeq
    }
  }

  def find(predicate: E => Boolean): ZIO[R, Throwable, Seq[E]] = {
    ZIO.succeed {
      val found = scala.collection.mutable.ListBuffer[E]()
      store.forEach {
        makeBiConsumer { (_, e) =>
          if (predicate(e)) {
            found += e
          }
        }
      }
      found.toSeq
    }
  }

  private[this] def makeBiConsumer(f: (ID, E) => Unit): BiConsumer[ID, E] =
    new BiConsumer[ID, E] {
      override def accept(t: ID, u: E): Unit = f(t, u)
    }
}

object InMemoryRepository {

  def apply[R, ID, E <: Repository.Entity[ID]](): InMemoryRepository[R, ID, E] = {
    new InMemoryRepository(new ConcurrentHashMap[ID, E]())
  }
}

class NoOpSearchRepository[R, E <: Repository.Entity[_]]() extends SearchRepository[R, E] {

  override def suggest(query: String): ZIO[R, Throwable, SearchRepository.SuggestResponse] =
    ZIO.succeed(SearchRepository.SuggestResponse(Nil))

  override def search(
    query: Option[String],
    page: Int,
    pageSize: Int,
    sorts: Iterable[SearchRepository.FieldSort]): ZIO[R, Throwable, SearchRepository.PaginatedSequence[E]] =
    ZIO.succeed(SearchRepository.PaginatedSequence[E](Nil, page, pageSize, 0))
}

object NoOpSearchRepository {

  def apply[R, E <: Repository.Entity[_]](): NoOpSearchRepository[R, E] = {
    new NoOpSearchRepository()
  }
}
