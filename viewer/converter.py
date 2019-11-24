
import locale
import datetime
from pathlib import Path
import sys
import json
import xml.etree.ElementTree as ET


class Converter:
    def import_data(self, path):
        with open(path) as fd:
            self.jfile = json.load(fd)

    def convert(self):
        for conversation in self.jfile['conversations']:
            try:
                addr = conversation[0]['address'].replace(' ', '')
            except KeyError:
                addr = ','.join(conversation[0]['addresses']).replace(' ', '')

            outfile = Path(f'{addr}.html')

            hconv = self.build_conversation(conversation)

            html = ET.Element('html')
            hhead = ET.SubElement(html, 'head')
            ET.SubElement(hhead, 'link', rel='stylesheet', href='https://cdn.jsdelivr.net/gh/kognise/water.css@latest/dist/dark.css')
            ET.SubElement(hhead, 'link', rel='stylesheet', href='style.css')
            hbody = ET.SubElement(html, 'body')
            hbody.append(hconv)
            with outfile.open('wb') as fd:
                fd.write(ET.tostring(html, method='html'))

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
        text_part = next((part for part in parts if part['ct'] == 'text/plain'), None)
        img_part = next((part for part in parts if part['ct'].startswith('image/')), None)

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

        if img_part:
            hdimg = ET.SubElement(hmsg, 'div')
            ET.SubElement(
                hdimg, 'img', **{
                    'class': 'message-photo',
                    'src': f'data:{img_part["ct"]};base64,{img_part["my_content"]}',
                })

        if text_part:
            hbody = ET.SubElement(hmsg, 'div', **{'class': 'message-body'})
            hbody.text = text_part['text']

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


locale.setlocale(locale.LC_ALL, '')

c = Converter()
c.import_data(sys.argv[1])
c.convert()
