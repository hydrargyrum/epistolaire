# Epistolaire

Epistolaire is an Android app to make backups of SMSes AND MMSes (unlike most "SMS backup" apps), in a nutshell.

* saves SMS/MMSes to a JSON file that's very close to a dump of Android's message database
* also saves images etc. from MMSes
* cannot restore messages from the JSON file to Android SMS/MMSes
* includes a JSON to pretty-HTML converter
* free and open-source, no ads

## JSON format

* the output file is a JSON object having a "conversations" key (value is a JSON array)
* a conversation is a list of messages (JSON objects)
* a message is either a SMS or an MMS
* a SMS has all the keys a SMS message has in the content://sms/ context
* a MMS has all the keys a MMS message has in the content://mms/ context plus extra keys
* extra MMS keys are "addresses" (a list of strings being the sender then the recipients) and "parts"
* each part is a JSON object having all the keys a part has in the content://mms/part/ context

## JSON to HTML

The JSON to HTML converter is a separate Python script in the "viewer" directory.
It will create an HTML file per conversation.

## License

Epistolaire is licenses under the Unlicense, which is basically public domain.
