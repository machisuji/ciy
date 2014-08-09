package ciy

import scala.collection.JavaConverters._
import org.yaml.snakeyaml._

/**
 * Breaks if you try to parse maps with anything else than strings as keys for now.
 */
case class YamlMap(data: Map[String, Any]) {
  def string(key: String): Option[String] = get[String](key)
  def int(key: String): Option[Int] = get[Int](key)
  def long(key: String): Option[Long] = get[Long](key)
  def boolean(key: String): Option[Boolean] = get[Boolean](key)

  def map(key: String): Option[YamlMap] = get[YamlMap](key)
  def list[T](key: String)(implicit c: Converter[Any, T]): Option[List[T]] = get[List[T]](key)

  def get[T](key: String)(implicit c: Converter[Any, T]): Option[T] =
    if (key.contains(".")) {
      val keys = key.split("\\.")
      mapOfPath(keys).flatMap(_.get[T](keys.last))
    } else {
      data.get(key).flatMap(c(_))
    }

  def contains(key: String): Boolean = data.contains(key)
  def keys: Set[String] = data.keys.toSet

  private def mapOfPath(keys: Seq[String]): Option[YamlMap] =
    keys.init.foldLeft(Some(this): Option[YamlMap]) {
      case (mapOpt, key) => mapOpt.flatMap(_.map(key))
    }

  override def toString = data.toString.replace("Map", "YamlMap")
}

object YamlMap {
  def empty = YamlMap(Map())
}

object YAML {

  /**
   * Returns a YamlMap loaded from the given file if the file exists.
   */
  def fromFile(file: String): Option[YamlMap] =
    try Some(fromString(io.Source.fromFile(file).mkString))
    catch {
      case _: java.io.FileNotFoundException => None
    }

  def fromString(str: String): YamlMap = {
    val yaml = new Yaml()
    val data = yaml.load(str).asInstanceOf[java.util.Map[String, Any]].asScala

    YamlMap(Map() ++ data)
  }
}

trait Converter[F, T] {
  def apply(value: F): Option[T]
}

object Converter {
  implicit def string = new StringConverter
  implicit def int = new IntConverter
  implicit def long = new LongConverter
  implicit def boolean = new BooleanConverter
  implicit def list[T](implicit ec: Converter[Any, T]) = new ListConverter[T]
  implicit def map = new MapConverter
}

class StringConverter extends Converter[Any, String] {
  def apply(value: Any): Option[String] = value match {
    case s: java.lang.String => Some(s)
    case _ => None
  }
}

class IntConverter extends Converter[Any, Int] {
  def apply(value: Any): Option[Int] = value match {
    case i: java.lang.Integer => Some(i.toInt)
    case _ => None
  }
}

class LongConverter extends Converter[Any, Long] {
  def apply(value: Any): Option[Long] = value match {
    case l: java.lang.Long => Some(l.toLong)
    case _ => None
  }
}

class BooleanConverter extends Converter[Any, Boolean] {
  def apply(value: Any): Option[Boolean] = value match {
    case b: java.lang.Boolean => Some(b.asInstanceOf[scala.Boolean])
    case _ => None
  }
}

class ListConverter[T](implicit ec: Converter[Any, T]) extends Converter[Any, List[T]] {
  def apply(value: Any): Option[List[T]] = value match {
    case l: java.util.List[_] =>
      if (l.size == 0) {
        Some(List[T]())
      } else if (l.size >= 1) {
        val list = l.asScala.toList.flatMap(e => ec(e).toList)

        if (list.isEmpty) None // failed to convert anything
        else Some(list)
      } else {
        None
      }
    case _ => None
  }
}

class MapConverter extends Converter[Any, YamlMap] {
  def apply(value: Any): Option[YamlMap] =
    if (value.isInstanceOf[java.util.Map[_, _]]) {
      Some(YamlMap(Map() ++ value.asInstanceOf[java.util.Map[String, Any]].asScala))
    } else {
      None
    }
}
