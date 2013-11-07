package com.oglowo.callfire.entity

import org.joda.time.DateTime

case class Call(id: Long,
                from: PhoneNumber,
                to: PhoneNumber,
                state: ActionState,
                contactId: Long,
                inbound: Boolean,
                createdOn: DateTime,
                modifiedOn: DateTime,
                finalResult:
                Option[Result] = None,
                callRecords: Set[CallRecord] = Set.empty) extends ApiEntity {

}

