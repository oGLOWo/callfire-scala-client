package com.oglowo.callfire.entity

import org.joda.time.DateTime

case class Text(id: Long,
                from: Option[PhoneNumber],
                to: PhoneNumber,
                state: ActionState,
                batchId: Option[Long],
                broadcastId: Option[Long],
                contactId: Long,
                inbound: Boolean,
                createdOn: DateTime,
                modifiedOn: DateTime,
                finalResult: Option[Result] = None,
                message: Option[String] = None,
                textRecords: Set[TextRecord] = Set.empty) extends ApiEntity {

}