#!/usr/bin/ruby -w
require "fiddle"
require "fiddle/import"

module User32
    extend(Fiddle::Importer)
    dlload("user32")
    extern("int MessageBox(const void*, const char*, const char*, int)")
end

