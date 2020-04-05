#!/usr/bin/env python3

# This is free and unencumbered software released into the public domain.
# See LICENSE file for details.

from base64 import b64decode
from datetime import datetime
import json
from email.message import EmailMessage
from email.utils import localtime, make_msgid
import hashlib
import locale
import mailbox
from mimetypes import guess_type


def set_header(msg, k, v):
    del msg[k]
    msg[k] = v


def hexdigest(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()


class Converter:
    def __init__(self, options):
        super().__init__()
        self.options = options

    def import_data(self, path):
        with open(path) as fd:
            self.jfile = json.load(fd)

    def convert(self, outpath):
        box = mailbox.Maildir(outpath)

        for jconversation in self.jfile['conversations']:
            conv_messages = []

            first_id = None
            for jmsg in jconversation:
                msg = self.build_message(jmsg)

                if self.options.threading:
                    # create a fake empty root message for each conversation

                    if first_id is None:
                        if 'address' in jmsg:  # sms
                            root = self.build_fake_root([jmsg['address']])
                        elif 'addresses' in jmsg:
                            root = self.build_fake_root(jmsg['addresses'])
                        else:
                            raise NotImplementedError('no address in the first message')

                        # conv_messages.append(root)
                        first_id = root['Message-ID']

                    set_header(msg, 'References', first_id)

                conv_messages.append(msg)

            for msg in conv_messages:
                box.add(msg)

    def build_fake_root(self, addresses):
        addresses = sorted(addresses)  # ensure consistency

        msg = EmailMessage()
        set_header(msg, 'Message-ID', f"<{hexdigest(','.join(addresses))}@{self.options.hostname}>")
        set_header(msg, 'Subject', f"Conversation with {', '.join(addresses)}")
        return msg

    def build_message(self, jmsg):
        msg = EmailMessage()

        dt = datetime.fromtimestamp(jmsg['date'] / 1000)
        set_header(msg, 'Date', localtime(dt))
        # set_header(msg, 'Subject', jmsg['subject'])
        set_header(msg, 'Message-ID', make_msgid(domain='localhost'))

        if 'parts' in jmsg:
            self.build_mms(jmsg, msg)
        else:
            self.build_sms(jmsg, msg)

        return msg

    def build_sms(self, jmsg, msg):
        if jmsg['type'] == 1:
            set_header(msg, 'From', jmsg['address'])
        else:
            set_header(msg, 'To', jmsg['address'])

        body = jmsg['body']
        for part in msg.iter_parts():
            if part.get_content_type() == 'text/plain':
                part.set_content(body)
                break
        else:
            msg.set_content(body)

    def build_mms(self, jmsg, msg):
        set_header(msg, 'From', jmsg['addresses'][0])
        set_header(msg, 'To', jmsg['addresses'][1:])

        # search for main text because it's harder to add it afterwards
        last_text = ''
        for jpart in jmsg['parts']:
            if jpart['text']:
                last_text = jpart['text']

        for part in msg.iter_parts():
            if part.get_content_type() == 'text/plain':
                part.set_content(last_text)
                break
        else:
            msg.set_content(last_text)

        # add all parts
        for jpart in jmsg['parts']:
            mime = None
            if jpart['name']:
                mime, _ = guess_type(jpart['name'])
            if not mime:
                mime = jpart['ct'] or 'application/octet-stream'
            maintype, _, subtype = mime.partition('/')

            content = jpart['text']
            if content:
                content = content.encode('utf-8')
            else:
                content = b64decode(jpart['my_content'])

            msg.add_attachment(
                content,
                maintype=maintype,
                subtype=subtype,
                filename=jpart['name'],
            )


def main():
    import argparse

    locale.setlocale(locale.LC_ALL, '')

    parser = argparse.ArgumentParser()
    parser.add_argument('file')
    parser.add_argument('output_dir')
    parser.add_argument('--hostname', default='localhost')
    parser.add_argument(
        '--disable-threading', action='store_const',
        dest='threading', const=False, default=True,
    )
    args = parser.parse_args()

    c = Converter(args)
    c.import_data(args.file)
    c.convert(args.output_dir)


if __name__ == '__main__':
    main()
