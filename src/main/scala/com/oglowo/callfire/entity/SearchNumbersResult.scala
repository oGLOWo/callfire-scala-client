package com.oglowo.callfire.entity

case class SearchNumbersResult(totalResults: Int, results: List[PhoneNumber]) extends ApiEntityList[PhoneNumber]