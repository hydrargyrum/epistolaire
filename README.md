# Epistolaire

Epistolaire is an Android app to backup SMSes AND MMSes (unlike most "SMS backup" apps), in a nutshell.

[![Get it on F-Droid](https://f-droid.org/badge/get-it-on.png)](https://f-droid.org/packages/re.indigo.epistolaire/)

## Why use it?

* saves SMS/MMSes to a JSON file that's very close to a dump of Android's message database
* also saves images etc. from MMSes
* includes a JSON to pretty-HTML converter
* free and open-source, no ads

## Why not use it?

* *cannot* restore messages from the JSON file to Android SMS/MMSes
* UI is ugly because it's my first Android app

## JSON format

* the output file is a JSON object having a "conversations" key (value is a JSON array)
* a conversation is a list of messages (JSON objects)
* a message is either a SMS or an MMS
* a SMS has all the keys a SMS message has in the content://sms/ context
    * examples: _id, thread_id, address, date, date_sent, type, body
* a MMS has all the keys a MMS message has in the content://mms/ context plus extra keys
    * examples: _id, thread_id, date, msg_box
* extra MMS keys are "addresses" (a list of strings being the sender then the recipients) and "parts"
* each MMS part is a JSON object having all the keys a part has in the content://mms/part/ context
    * examples: _id, mid, text, ct, name
* MMS parts having a binary file have the content encoded as base64 in the "my_content" key

See [`backup.schema.json` file](backup.schema.json) for a JSON-schema description.

## JSON to HTML

The JSON to HTML converter is a separate Python script in the "viewer" directory.
It will create an HTML file per conversation.

    cd viewer
    ./converter.py path/to/backup.json

If your JSON file is very big, Python might not be able to load the whole file in RAM and thus crash.
Installing `ijson` (with `sudo apt install python3-ijson` or `pip install ijson`) will make the converter
able to process the JSON data without loading everything in memory at once.

## Privacy

Epistolaire doesn't upload your messages anywhere. It just dumps a JSON file on your device.
Make sure any third-party app doesn't harvest your device's files though.

## Why isn't Epistolaire on google-play

I don't use google-play, and I don't intend to ever do, so I won't upload Epistolaire on it.

## License

Epistolaire is licensed under the Unlicense, which is basically public domain.
