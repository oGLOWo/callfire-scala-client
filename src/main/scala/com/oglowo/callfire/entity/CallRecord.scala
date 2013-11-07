package com.oglowo.callfire.entity

import org.joda.money.Money
import org.joda.time.DateTime

// TODO refactor this so there is an ActionRecord base
case class CallRecord(id: Long, result: Result, finishedOn: DateTime, billedAmount: Double, answeredOn: DateTime, durationInSeconds: Int, recordingsMetaData: Set[RecordingMeta] = Set.empty) extends ApiEntity {

}

