#!/usr/bin/env python3

# This is free and unencumbered software released into the public domain.
# See LICENSE file for details.

import locale
import logging
import datetime
from pathlib import Path
import json
import xml.etree.ElementTree as ET


LOGGER = logging.getLogger(__name__)


class Converter:
    @staticmethod
    def iter_conversations(inpath):
        try:
            import ijson

        except ImportError:
            with open(inpath, encoding="utf-8") as fd:
                data = json.load(fd)
            yield from data["conversations"]

        else:
            with open(inpath, encoding="utf-8") as fd:
                # if we return instead of yield, the file will be closed when
                # caller gets the value, and thus can't iterate on anything
                yield from ijson.items(fd, "conversations.item")

    def convert(self, inpath, outpath, css_text=None):
        seen = set()

        def convert_conversation(conversation):
            try:
                addr = ','.join(conversation[0]['addresses']).replace(' ', '')
            except KeyError:
                addr = conversation[0]['address'].replace(' ', '')
            except IndexError:
                return

            outfile = Path(outpath, f"{addr[:200]}{addr[200:] and '...'}.html")
            if outfile in seen:
                raise FileExistsError(f"oops, {outfile} has already been used")
            seen.add(outfile)

            hconv = self.build_conversation(conversation)

            html = ET.Element('html')
            hhead = ET.SubElement(html, 'head')

            ET.SubElement(hhead, 'link', rel='stylesheet', href='https://cdn.jsdelivr.net/gh/kognise/water.css@latest/dist/dark.css')
            if css_text:
                hstyle = ET.SubElement(hhead, 'style')
                hstyle.text = css_text

            hbody = ET.SubElement(html, 'body')
            hbody.append(hconv)
            with outfile.open('wb') as fd:
                fd.write(ET.tostring(html, method='html'))

            LOGGER.info('done conversation %r', addr)

        for conversation in self.iter_conversations(inpath):
            try:
                convert_conversation(conversation)
            except Exception as exc:
                LOGGER.exception('could not convert a conversation: %s', exc)

    def build_conversation(self, jconv):
        hconv = ET.Element('div', **{
            'itemscope': 'itemscope',
            'itemtype': 'http://schema.org/Message',
        })

        for jmsg in sorted(jconv, key=lambda jmsg: jmsg['date']):
            if 'parts' in jmsg:
                self.build_mms(jmsg, hconv)
            else:
                self.build_sms(jmsg, hconv)

        return hconv

    def build_mms(self, jmsg, hconv):
        parts = jmsg['parts']

        is_received = jmsg['msg_box'] == 1
        dt = datetime.datetime.fromtimestamp(jmsg['date'] / 1000)

        hmsg = ET.SubElement(
            hconv, 'div', id=str(jmsg['_id']),
            **{
                'class': f'message message-{"received" if is_received else "sent"}',
                'itemscope': 'itemscope',
                'itemprop': 'hasPart',
                'itemtype': 'http://schema.org/Message',
            },
        )

        htime = ET.SubElement(
            hmsg, 'time', **{
                'class': 'message-date',
                'itemprop': 'dateReceived',
                'datetime': dt.isoformat(),
            })
        htime.text = dt.strftime('%Y-%m-%d %H:%M:%S')

        for part in parts:
            if part['ct'].startswith('image/'):
                hdimg = ET.SubElement(hmsg, 'div')
                ET.SubElement(
                    hdimg, 'img', **{
                        'class': 'message-photo',
                        'src': f'data:{part["ct"]};base64,{part["my_content"]}',
                    })

            elif part['ct'] == 'text/plain':
                hbody = ET.SubElement(hmsg, 'div', **{'class': 'message-body'})
                hbody.text = part['text']

        LOGGER.debug('done mms %r', jmsg['_id'])

    def build_sms(self, jmsg, hconv):
        is_received = jmsg['type'] == 1
        dt = datetime.datetime.fromtimestamp(jmsg['date'] / 1000)

        hmsg = ET.SubElement(
            hconv, 'div', id=str(jmsg['_id']),
            **{
                'class': f'message message-{"received" if is_received else "sent"}',
                'itemscope': 'itemscope',
                'itemprop': 'hasPart',
                'itemtype': 'http://schema.org/Message',
            },
        )

        # haddr = ET.SubElement(
        #     hmsg, 'div', **{
        #         'class': 'message-address',
        #         'itemprop': 'sender' if is_received else 'recipient',
        #     })
        # haddr.text = jmsg['address']

        htime = ET.SubElement(
            hmsg, 'time', **{
                'class': 'message-date',
                'itemprop': 'dateReceived',
                'datetime': dt.isoformat(),
            })
        htime.text = dt.strftime('%Y-%m-%d %H:%M:%S')

        hbody = ET.SubElement(hmsg, 'div', **{'class': 'message-body'})
        hbody.text = jmsg['body']

        LOGGER.debug('done sms %r', jmsg['_id'])


def main():
    import argparse

    locale.setlocale(locale.LC_ALL, '')

    parser = argparse.ArgumentParser()
    parser.add_argument('file')
    parser.add_argument('-o', '--output-dir', default='.')
    parser.add_argument('-v', '--verbose', action='store_true')

    default_css = Path(__file__).with_name('style.css')
    parser.add_argument('--include-css', metavar='FILE', default=default_css)
    args = parser.parse_args()

    levels = {
        False: logging.INFO,
        True: logging.DEBUG,
    }
    logging.basicConfig(
        level=levels[args.verbose],
        format='%(message)s'
    )

    css_text = None
    if args.include_css:
        css_text = Path(args.include_css).read_text()

    c = Converter()
    c.convert(args.file, args.output_dir, css_text)


if __name__ == '__main__':
    main()

