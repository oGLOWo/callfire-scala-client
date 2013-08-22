package com.oglowo.callfire

import scala.xml.{Null, UnprefixedAttribute, MetaData}
import scala.annotation.tailrec

package object callfirexml {
  def metadata(attributes: Map[String, _]): MetaData = {
    def valueAsString(value: Any) = {
      value match {
        case Some(s) => s match {
          case string: String => string
          case _ => s.toString
        }
        case None => Null.toString
        case default => default.toString
      }
    }

    @tailrec
    def iterator(attributes: Map[String, _], currentAttribute: MetaData): MetaData = {
      if (!attributes.isEmpty) iterator(attributes.tail, currentAttribute.append(new UnprefixedAttribute(attributes.head._1, valueAsString(attributes.head._2), Null)))
      else currentAttribute
    }

    if (attributes.isEmpty) Null
    else iterator(attributes.tail, new UnprefixedAttribute(attributes.head._1, valueAsString(attributes.head._2), Null))
  }
}
