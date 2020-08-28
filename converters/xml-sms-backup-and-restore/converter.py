#!/usr/bin/env python3

# This is free and unencumbered software released into the public domain.
# See LICENSE file for details.

from datetime import datetime
import json
import locale
import xml.etree.ElementTree as ET


def stringify_values(d):
    for k, v in d.items():
        if v is None:
            v = 'null'
        d[k] = str(v)


# SMS backup&restore is a non-free app which uses an undocumented XML format.
# I used this XSL https://github.com/anohako/SMS-Backup-Restore-XSL-Transform-to-HTML
# as a "documentation".

class Converter:
    def import_data(self, path):
        with open(path, encoding='utf-8') as fd:
            self.jfile = json.load(fd)

    def convert(self, outpath):
        xroot = ET.Element('smses')

        n = 0
        for jconv in self.jfile['conversations']:
            for jmsg in jconv:
                xmsg = self.build_message(jmsg)
                xroot.append(xmsg)
                n += 1

        xroot.attrib['count'] = str(n)

        with open(str(outpath), 'wb') as fd:
            for line in ET.tostringlist(xroot, method='xml'):
                fd.write(line)

    def build_message(self, jmsg):
        if 'parts' in jmsg:
            return self.build_mms(jmsg)
        else:
            return self.build_sms(jmsg)

    def build_sms(self, jmsg):
        jmsg = jmsg.copy()
        jmsg['readable_date'] = datetime.fromtimestamp(jmsg['date'] / 1000).strftime('%c')
        jmsg['contact_name'] = jmsg['address']

        stringify_values(jmsg)
        return ET.Element('sms', **jmsg)

    def build_mms(self, jmsg):
        mattrs = jmsg.copy()
        # not the right address?
        mattrs['address'] = jmsg['addresses'][0]
        # epistolaire doesn't fetch contacts (yet?)
        mattrs['contact_name'] = jmsg['addresses'][0]

        mattrs.pop('addresses', None)
        mattrs.pop('parts', None)

        stringify_values(mattrs)
        xmms = ET.Element('mms', **mattrs)

        xparts = ET.SubElement(xmms, 'parts')
        for jpart in jmsg['parts']:
            jpart = jpart.copy()

            if jpart.get('my_content'):
                jpart['data'] = jpart['my_content']
                del jpart['my_content']

            stringify_values(jpart)
            ET.SubElement(xparts, 'part', **jpart)

        xaddrs = ET.SubElement(xmms, 'addrs')
        for addr in jmsg['addresses']:
            # the XSL used addr[3]? what's it supposed to be?
            # what order should addresses be?
            ET.SubElement(xaddrs, 'addr', address=addr)

        return xmms


def main():
    import argparse

    locale.setlocale(locale.LC_ALL, '')

    parser = argparse.ArgumentParser()
    parser.add_argument('file')
    parser.add_argument('output_file')
    args = parser.parse_args()

    c = Converter()
    c.import_data(args.file)
    c.convert(args.output_file)


if __name__ == '__main__':
    main()
