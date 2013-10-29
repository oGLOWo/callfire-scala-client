package com.oglowo.callfire

import scala.xml.{Null, UnprefixedAttribute, MetaData}
import scala.annotation.tailrec
import com.typesafe.scalalogging.log4j.Logging

package object callfirexml {
  def metadata(attributes: Map[String, _]): MetaData = {
    def filterAttributes(a: Map[String, _]) = a.filter(entry => {
      entry._2 match {
        case o: Option[_] => o match {
          case Some(s) => true
          case None => false
        }
        case _ => true
      }
    })

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

    val filteredAttributes = filterAttributes(attributes)
    println("Filtered attributes are: " + filteredAttributes)
    if (filteredAttributes.isEmpty) Null
    else iterator(filteredAttributes.tail, new UnprefixedAttribute(filteredAttributes.head._1, valueAsString(filteredAttributes.head._2), Null))
  }
}
