package com.oglowo.callfire.entity

import org.joda.time.DateTime

case class TextRecord(id: Long, result: Result, finishedOn: DateTime, billedAmount: Double, message: String) {

}
