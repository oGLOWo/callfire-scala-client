package com.oglowo.callfire.entity

case class Region(prefix: Option[String] = None,
                  city: Option[String] = None,
                  state: Option[String] = None,
                  postalCode: Option[String] = None,
                  country: Option[String] = None,
                  localAccessTransportArea: Option[String] = None,
                  rateCenter: Option[String] = None,
                  latitude: Option[Float] = None,
                  longitude: Option[Float] = None,
                  timeZone: Option[String] = None) extends ApiEntity
