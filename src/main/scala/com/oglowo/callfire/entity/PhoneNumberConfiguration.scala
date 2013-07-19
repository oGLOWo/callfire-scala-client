package com.oglowo.callfire.entity

import PhoneNumberFeature._

case class PhoneNumberConfiguration(callFeature: Option[PhoneNumberFeature],
                                    textFeature: Option[PhoneNumberFeature],
                                    inboundCallConfiguration: Option[InboundCallConfiguration[_]])