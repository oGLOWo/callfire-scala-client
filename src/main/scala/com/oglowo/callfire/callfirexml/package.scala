package com.oglowo.callfire

import scala.xml.{Null, UnprefixedAttribute, MetaData}
import scala.annotation.tailrec
import scala.Option

package object callfirexml {
  def metadata(attributes: Map[String, _]): MetaData = {
    @tailrec
    def iterator(attributes: Map[String, _], currentAttribute: MetaData): MetaData = {
      if (!attributes.isEmpty) {
        val value = attributes.head._2 match {
          case Some(s) => s match {
            case string: String => string
            case default => default.toString
          }
          case None => Null
          case default => default.toString
        }
        iterator(attributes.tail, currentAttribute.append(new UnprefixedAttribute(attributes.head._1, attributes.head._2.toString, Null)))
      }
      else currentAttribute
    }
    if (attributes.isEmpty) Null
    else iterator(attributes.tail, new UnprefixedAttribute(attributes.head._1, attributes.head._2.toString, Null))
  }
}
