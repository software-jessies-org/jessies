#!/usr/bin/ruby -w

require 'cgi'

def formatChanges(changes)
  body = ""
  changes.each {
    |line|
    line = CGI.escapeHTML(line)
    color = "black"
    
    if line =~ /^Modified: /
      next
    elsif line =~ /^Added: /
      next
    elsif line =~ /^@@ /
      color = "gray"
    elsif line =~ /^-/
      color = "red"
    elsif line =~ /^\+/
      color = "blue"
    end
    
    if line =~ /^(---|\+\+\+)(.*)$/
      # Use <tt> for ---/+++ so they line up, and use distinctive background
      # colors. We can't use "---" and "+++" as class names, sadly.
      class_name = ($1 == "---" ? "triple-minus-line" : "triple-plus-line")
      body << "<div class=\"#{class_name}\"><tt>#$1</tt>#$2</div>"
      next
    end
    
    body << "<font color=\"#{color}\">"
    # Write the per-line prefix characters in fixed width
    line.gsub!(/^([-+ ])/) {
      |prefix|
      "<tt>#{prefix.gsub(' ', '&nbsp;')}</tt>"
    }
    # Don't collapse indentation
    line.gsub!(/( {2,})/) {
      |s|
      s.gsub(' ', '&nbsp;')
    }
    line.gsub!(/\t/, '&nbsp;&nbsp;&nbsp;&nbsp;')
    body << "#{line}<br/></font>\n"
  }
  return body
end

def sanitizeChanges(changes)
  # I don't think there's any source in the jessies code with more than the 2778 lines of ectags/c.c.
  # man-summary only has 4152 lines but it's uselessly boring to scroll through in an email.
  # The 533 lines of TerminatorMenuBar.java seems a more likely maximum for source that's likely to appear at once,
  # but we can conservatively set the limit higher.
  # Evergreen refused to load files larger than a megabyte at some stage (the limit's half a gig now).
  if changes.length() > 1000 || changes.join("\n").length() > 1024 * 1024
    patchHeader = changes.slice(0, 3)
    errorMessage = "(truncated due to fear of email breakage - this patch was too large to be a plausible manual change)"
    return patchHeader << errorMessage
  end
  return changes
end

def outputFormattedChanges(asciiArt, changes)
  changes = sanitizeChanges(changes)
  body = ""
  if asciiArt
    body << "<tt>"
    body << formatChanges(changes)
    body << "</tt>"
  else
    body << formatChanges(changes)
  end
  return body
end

def patchToHtml(subject, preamble, changes)
  body = ""
  body << "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
  body << "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
  body << "<html lang=\"EN\">\n"
  body << "<head>\n"
  body << " <title>#{subject}</title>\n"
  
  body << "  <style type=\"text/css\">\n"
  body << ".revision-header {background: #cccccc; padding: 6pt; margin-bottom: 0pt; text-align: right;}\n"
  body << ".check-in-comment {background: #eeeeee; padding: 6pt; margin-bottom: 6pt;}\n"
  body << ".triple-minus-line {background: #ffcccc; color: red;}\n"
  body << ".triple-plus-line {background: #ccccff; color: blue;}\n"
  body << "  </style>\n"
  
  body << " </head>\n"
  body << "<body>\n"
  body << preamble
  
  # Determine if we should output it in a fixed width font
  asciiArt = false
  changesSoFar = Array.new
  
  changes.each() {
    |line|
    line = line.chomp()
    
    if line =~ /^=====/
      body << outputFormattedChanges(asciiArt, changesSoFar)
      asciiArt = false
      changesSoFar = Array.new
      next
    end
    
    # ETextArea says 3 spaces and you're art
    if line =~ /^.\s*\S.*\s{3,}/
      asciiArt = true
    end
    changesSoFar.push(line)
  }
  body << outputFormattedChanges(asciiArt, changesSoFar)
  
  body << "</body></html>"
  return body
end

def sendHtmlEmail(from_address, to_address, reply_to_address, subject, preamble, changes)
  body = patchToHtml(subject, preamble, changes)
  
  # Write the header.
  header = ""
  header << "To: #{to_address}\n"
  header << "From: #{from_address}\n"
  header << "Subject: #{subject}\n"
  header << "Reply-to: #{reply_to_address}\n" if reply_to_address != nil
  header << "MIME-Version: 1.0\n"
  header << "Content-Type: text/html; charset=UTF-8\n"
  header << "Content-Transfer-Encoding: 8bit\n"
  header << "\n"

  sendmail="/usr/sbin/sendmail"

  IO.popen("#{sendmail} #{to_address}", "w") {
    |fd|
    fd.print(header)
    # The hope is that this would be bigger than anyone would read while being small enough
    # to avoid hard mail size limits - so we get to see there was a problem - and small enough
    # not to cause performance problems.
    maximumMailSize = 2 * 1024 * 1024
    fd.print(body.slice(0, maximumMailSize))
  }
end

# Work as a filter if we're run as a program rather than used as a library.
if __FILE__ == $0
  puts(patchToHtml("<stdin>", "", ARGF.readlines()))
end
