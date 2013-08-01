package com.oglowo.callfire.entity

case class PhoneNumberConfiguration(callFeature: Option[PhoneNumberFeature],
                                    textFeature: Option[PhoneNumberFeature],
                                    inboundCallConfiguration: Option[InboundCallConfiguration[_]])