#!/usr/bin/ruby -w
require "dl"
require "dl/import"

module User32
    extend(DL::Importer)
    dlload("user32")
    extern("int MessageBox(const void*, const char*, const char*, int)")
end

