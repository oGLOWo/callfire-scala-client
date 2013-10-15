package com.oglowo.callfire.entity

import spray.http.Uri


case class ResourceReference(id: Long, location: Uri) extends ApiEntityReference


