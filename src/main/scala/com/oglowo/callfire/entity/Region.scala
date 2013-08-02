package com.oglowo.callfire.entity

// TODO: prefix, localAccessTransportArea, and postalCode have been set to Long because CallFire's JSON
// TODO: is not returning the values inside of quotes so they are treated as a JsNumber
// TODO: When this is fixed, these all need to be changed to String values
case class Region(prefix: Option[Long] = None,
                  city: Option[String] = None,
                  state: Option[String] = None,
                  postalCode: Option[Long] = None,
                  country: Option[String] = None,
                  localAccessTransportArea: Option[Long] = None,
                  rateCenter: Option[String] = None,
                  latitude: Option[Float] = None,
                  longitude: Option[Float] = None,
                  timeZone: Option[String] = None) extends ApiEntity
