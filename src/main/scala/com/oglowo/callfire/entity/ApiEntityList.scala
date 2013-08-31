package com.oglowo.callfire.entity

trait ApiEntityList[T <: ApiEntity] {
  val JsonObjectName = "ResourceList"
  val TotalResultsKey = "@totalResults"

  val totalResults: Int
  val results: List[T]
}
