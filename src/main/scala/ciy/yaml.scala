package ciy

import scala.collection.JavaConverters._
import org.yaml.snakeyaml._

case class YamlMap(data: Map[String, Any]) {
  def string(key: String): Option[String] = tryGet(key, classOf[String])
  def int(key: String): Option[Int] = tryGet(key, classOf[java.lang.Integer]).map(_.toInt)
  def long(key: String): Option[Long] = tryGet(key, classOf[java.lang.Long]).map(_.toLong)
  def boolean(key: String): Option[Boolean] =
    tryGet(key, classOf[java.lang.Boolean]).map(_.asInstanceOf[scala.Boolean])

  def map(key: String): Option[YamlMap] =
    tryGet(key, classOf[java.util.LinkedHashMap[String, Any]]).map(m => YamlMap(Map() ++ m.asScala))

  def contains(key: String): Boolean = data.contains(key)

  /**
   * Tries to get the value for the given key.
   *
   * @return Returns the value of the given key given it has the assumed type.
   */
  private def tryGet[T](key: String, klass: Class[T]): Option[T] = {
    if (key.contains(".")) tryGetAtPath(key, klass)
    else data.get(key).flatMap { value =>
      if (value.getClass == klass)
        Some(value.asInstanceOf[T])
      else
        None
    }
  }

  private def tryGetAtPath[T](keys: Seq[String], klass: Class[T]): Option[T] = {
    val lastMap: Option[YamlMap] = keys.init.foldLeft(Some(this): Option[YamlMap]) {
      case (mapOpt, key) => mapOpt.flatMap(_.map(key))
    }

    lastMap.flatMap(_.tryGet(keys.last, klass))
  }

  private def tryGetAtPath[T](keys: String, klass: Class[T]): Option[T] =
    tryGetAtPath(keys.split("\\."), klass)

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
      case _: FileNotFoundException => None
    }

  def fromString(str: String): YamlMap = {
    val yaml = new Yaml()
    val data = yaml.load(str).asInstanceOf[java.util.Map[String, Any]].asScala

    YamlMap(Map() ++ data)
  }
}
