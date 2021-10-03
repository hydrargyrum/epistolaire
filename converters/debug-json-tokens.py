#!/usr/bin/env python3

from enum import Enum, auto
import sys
import re


class InvalidJson(Exception):
	pass


class DictState(Enum):
	START = auto()
	KEY = auto()
	COLON = auto()
	VALUE = auto()
	END = auto()


class ListState(Enum):
	START = auto()
	VALUE = auto()
	END = auto()


class Parser:
	def __init__(self, stream):
		self.stream = stream
		self.buf = None
		self.index = 0

	def noop(self, *args):
		pass

	enter_dict = enter_list = leave_dict = leave_list = noop
	found_number = found_string = found_const = found_comma = found_colon = noop

	def is_at_end(self):
		return self.index == len(self.buf)

	def init(self):
		self.buf = self.stream.read()

	def process(self):
		self.process_value()
		self.chomp_spaces()
		if not self.is_at_end():
			raise InvalidJson(f"Extra data at {self.index}")

	def chomp_spaces(self):
		while True:
			if self.is_at_end():
				break
			c = self.buf[self.index]
			if not c.isspace():
				break

			self.index += 1

	def process_obj_dict(self):
		state = DictState.START

		while True:
			self.chomp_spaces()

			if self.is_at_end():
				raise InvalidJson("EOF while processing dict")

			c = self.buf[self.index]

			if state == DictState.START:
				if c == '"':
					self.index += 1
					self.process_obj_string()
					state = DictState.COLON
				elif c == "}":
					self.index += 1
					self.leave_dict()
					return
				else:
					raise InvalidJson(f"Expected '}}' or string key at {self.index}")

			elif state == DictState.KEY:
				if c == '"':
					self.index += 1
					self.process_obj_string()
					state = DictState.COLON
				else:
					raise InvalidJson(f"Expected string key at {self.index}")

			elif state == DictState.COLON:
				if c == ":":
					self.index += 1
					state = DictState.VALUE
					self.found_colon()
				else:
					raise InvalidJson(f"Expected ':' at {self.index}")

			elif state == DictState.VALUE:
				self.process_value()
				state = DictState.END

			elif state == DictState.END:
				if c == "}":
					self.index += 1
					self.leave_dict()
					return
				elif c == ",":
					self.index += 1
					state = DictState.KEY
					self.found_comma()
				else:
					raise InvalidJson(f"Expected '}}' or ',' at {self.index}")

			else:
				raise NotImplementedError()

	def process_obj_list(self):
		state = ListState.START

		while True:
			self.chomp_spaces()
			if self.is_at_end():
				raise InvalidJson("EOF while processing list")

			c = self.buf[self.index]
			if state == ListState.START:
				if c == "]":
					self.index += 1
					self.leave_list()
					return
				else:
					self.process_value()
					state = ListState.END

			elif state == ListState.VALUE:
				self.process_value()
				state = ListState.END

			elif state == ListState.END:
				if c == "]":
					self.index += 1
					self.leave_list()
					return
				elif c == ",":
					self.index += 1
					state = ListState.VALUE
					self.found_comma()
				else:
					raise InvalidJson(f"Expecting ']' or ',' at {self.index}")

			else:
				raise NotImplementedError()

	number_re = re.compile(r"""
		-?
		(?:
			0
			|[1-9]\d*
		)
		(?:
			\.
			\d*
		)?
		(?:
			[eE]
			[+-]?
			\d+
		)?
	""", re.X)

	def process_obj_number(self):
		match = self.number_re.match(self.buf, self.index)
		if not match:
			raise InvalidJson(f"Malformed number starting at {self.index}")
		self.index = match.end()

		self.found_number(match.group())

	string_re = re.compile(r"""
		(?:
			\\  # match a single backslash
			(?:
				["\\/bfnrt]
				|u[0-9a-fA-F]{4}
			)
			|[^"\\\n]
		)+
		"
	""", re.X)

	def process_obj_string(self):
		match = self.string_re.match(self.buf, self.index)
		if not match:
			raise InvalidJson(f"Malformed string starting at {self.index}")
		self.index = match.end()

		self.found_string(match.group()[:-1])

	const_re = re.compile("null|true|false")

	def process_value(self):
		self.chomp_spaces()

		c = self.buf[self.index]
		if c in "-0123456789":
			self.process_obj_number()
		elif c == '"':
			self.index += 1
			self.process_obj_string()
		elif c == "[":
			self.enter_list()
			self.index += 1
			self.process_obj_list()
		elif c == "{":
			self.enter_dict()
			self.index += 1
			self.process_obj_dict()
		else:
			match = self.const_re.match(self.buf, self.index)
			if not match:
				raise InvalidJson(f"Could not recognize value at {self.index}")
			self.found_const(match.group())
			self.index = match.end()


class Indenter(Parser):
	def __init__(self, *args):
		super().__init__(*args)
		self.nested = []

	def print(self, s):
		indent = "    " * len(self.nested)
		print(f"{indent}{s}")

	def enter_dict(self):
		self.print("{")
		self.nested.append("{")

	def leave_dict(self):
		self.nested.pop()
		self.print("}")

	def enter_list(self):
		self.print("[")
		self.nested.append("[")

	def leave_list(self):
		self.nested.pop()
		self.print("]")
		return
		opposites = {"{": "}", "[": "]"}
		c = opposites[self.nested[-1]]
		super().leave()
		print("    " * len(self.nested) + c)

	def found_const(self, v):
		self.print(v)

	def found_number(self, v):
		self.print(v)

	def found_string(self, v):
		self.print(f'"{v}"')

	def found_comma(self):
		self.print(",")

	def found_colon(self):
		self.print(":")


def main():
	idt = Indenter(sys.stdin)
	idt.init()
	try:
		idt.process()
	except InvalidJson as exc:
		print(f"Error: {exc}")
		return 1
	return 0


if __name__ == "__main__":
	sys.exit(main())
