val
x = {
        "Call": {
            "@id": 148374945001,
            "FromNumber": "12134485916",
            "ToNumber": "18552007014",
            "State": "FINISHED",
            "ContactId": 56652259001,
            "Inbound": true,
            "Created": "2013-11-27T21:50:17Z",
            "Modified": "2013-11-27T21:51:32Z",
            "FinalResult": "LA",
            "CallRecord": [
                {
                    "@id": 88618279001,
                    "Result": "LA",
                    "FinishTime": "2013-11-27T21:51:26Z",
                    "BilledAmount": 3.3334,
                    "AnswerTime": "2013-11-27T21:50:17Z",
                    "Duration": 69,
                    "RecordingMeta": [
                        {"@id": 1146627001, "Name": "voicemail_recording", "Created": "2013-11-27T21:51:32Z", "LengthInSeconds": 16, "Link": "https://www.callfire.com/recording/26175001/dd2dad2997836c17c9977ca20bbc069e/148374945001_voicemail_recording.mp3"}
                    ]
                }
            ]
        }
}

val y = {
        "Call": {
            "@id": "148339716001",
            "FromNumber": 19514030229,
            "ToNumber": 18552007014,
            "State": "FINISHED",
            "ContactId": 85211975001,
            "Inbound": true,
            "Created": "2013-11-27T21:31:02Z",
            "Modified": "2013-11-27T21:31:40Z",
            "FinalResult": "XFER_LEG",
            "CallRecord": [
                {
                    "@id": "88616482001",
                    "Result": "XFER_LEG",
                    "FinishTime": "2013-11-27T21:31:39Z",
                    "BilledAmount": 0,
                    "AnswerTime": "2013-11-27T21:31:16Z",
                    "Duration": 23
                },
                {
                    "@id": "88616481001",
                    "Result": "XFER",
                    "FinishTime": "2013-11-27T21:31:39Z",
                    "BilledAmount": 1.6667,
                    "AnswerTime": "2013-11-27T21:31:03Z",
                    "Duration": 36
                }
            ]
        }
}