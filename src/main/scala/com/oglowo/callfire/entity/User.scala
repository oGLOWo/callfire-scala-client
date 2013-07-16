package com.oglowo.callfire.entity

import java.util.{Date, UUID}

case class User(id: UUID, firstName: String, lastName: String, createdOn: Date, modifiedOn: Date, emailAddress: Option[String])