/*
substituter is an Evergreen tool that can substitute entered text with other text.

To compile this program, ensure you have the golang tools installed (package 'golang' in ubuntu)
and compile using:
$ go build substituter.go

This will compile the program to a binary called 'substituter' in the current directory.
Substituter uses the new hacky editor 'CommandInterpreter' protocol, so in your tool file
you should specify it with:

command=|!substituter

For example, in Go you might wish to have some kind of auto-complete for 'for' loops over maps.
In such a case, if you set up substituter correctly, then if you type 'forkv' into Evergreen and
hit the substituter-running hotkey, it could enter this:

  for k, v := range MAPNAME {
  }

... and leave 'MAPNAME' selected, so you can easily replace it with the correct name.

To use this program, you will need another program available to generate dialog boxes and get
user feedback, so that you can teach it new substitutions. The dialog box program used is
"yad", which supports multiple fields and multi-line strings (this is important). If yad is
not available (it is on Linux, no idea about Windows) then you may need to find an alternative.
In that case, you'll need to adjust the program which is run to collect user feedback. To do
this, edit the 'queryForNewEntry' function.
*/
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"regexp"
	"strconv"
	"strings"
	"unicode/utf8"
)

// The file in which to store substitutions. These are written in a json format, because
// Go has a very simple-to-use json codec.
var subFile = os.ExpandEnv("$HOME/.evergreen-substituter")

type entryList struct {
	Entries []*entry
}

type entry struct {
	Pattern, Replacement, FilePattern string
}

type evergreen struct {
	contents []byte
	filename string
	// line and char *index* (0-based).
	line, char      int
	lineStartIndex  int
	lineBeforeCaret []byte
}

func (ent *entry) substituteIfYouCan(state *evergreen) (bool, error) {
	if pathMatch, err := regexp.Compile(ent.FilePattern); err != nil {
		return false, err
	} else if !pathMatch.MatchString(state.filename) {
		return false, nil
	}
	matcher, err := regexp.Compile(ent.Pattern)
	if err != nil {
		return false, err
	}
	matches := matcher.FindAllIndex(state.lineBeforeCaret, -1)
	if len(matches) == 0 {
		return false, nil
	}
	lastMatch := matches[len(matches)-1]
	// We only consider a match which ends at the caret location.
	if lastMatch[1] != len(state.lineBeforeCaret) {
		return false, nil
	}
	// Pull out the text containing the last match. We'll do the substitution from here.
	matchedText := state.lineBeforeCaret[lastMatch[0]:lastMatch[1]]
	result := matcher.Expand([]byte{}, []byte(ent.Replacement), matchedText, matcher.FindSubmatchIndex(matchedText))
	origStartChar, origEndChar := runeIndicesFrom(state.lineBeforeCaret, lastMatch[0], lastMatch[1])

	// Calculate the location of the selection post-substitution.
	result, caretStart := extractCaret(result, len(result))
	result, caretEnd := extractCaret(result, caretStart)
	// caretStart and caretEnd are the byte-indexes of the start and end selection within result.
	// Now, using the known start *character* of the text we replaced (and its line), calculate
	// the start and end line + char indices.
	resultStr := string(result)
	caretStartLine, caretStartChar := toRuneCoordinates(resultStr, caretStart, state.line, origStartChar)
	caretEndLine, caretEndChar := toRuneCoordinates(resultStr, caretEnd, state.line, origStartChar)

	// We have all the info we need, now. Output the edit and new caret position.
	fmt.Println("command=replace")
	prInt("start_line", state.line+1)
	prInt("start_char", origStartChar)
	prInt("end_line", state.line+1)
	prInt("end_char", origEndChar)
	fmt.Printf("new_text=\"%s\"\n", doubleQuotes(resultStr))
	fmt.Println()
	fmt.Println("command=select")
	prInt("start_line", caretStartLine+1)
	prInt("start_char", caretStartChar)
	prInt("end_line", caretEndLine+1)
	prInt("end_char", caretEndChar)

	// Let the caller know that we've done the substitution, so that no other rule also
	// gets used.
	return true, nil
}

func toRuneCoordinates(in string, byteOffset, line, char int) (int, int) {
	for i, ch := range in {
		if i >= byteOffset {
			return line, char
		}
		if ch == '\n' {
			line++
			char = 0
		} else {
			char++
		}
	}
	panic("got to end of string")
}

func doubleQuotes(val string) string {
	var res bytes.Buffer
	for _, ch := range val {
		if ch == '"' {
			res.WriteRune(ch)
		}
		res.WriteRune(ch)
	}
	return res.String()
}

func prInt(key string, val int) {
	fmt.Printf("%s=%d\n", key, val)
}

func runeIndicesFrom(in []byte, start, end int) (int, int) {
	var startRes int
	runeIndex := 0
	for byteIndex, _ := range string(in) {
		if byteIndex == start {
			startRes = runeIndex
		}
		if byteIndex == end {
			return startRes, runeIndex
		}
		runeIndex++
	}
	return startRes, runeIndex
}

// Finds the first caret character ('^'), which indicates where the caret should end up.
// The returned array is the same as in, but with the caret removed. The second return value
// is the location where the caret was (or the value of dflt if there wasn't one).
func extractCaret(in []byte, dflt int) ([]byte, int) {
	res := bytes.IndexRune(in, '^')
	if res == -1 {
		return in, dflt
	}
	return append(in[:res], in[res+1:]...), res
}

func queryForNewEntry(match, filePattern string) (*entry, error) {
	args := []string{
		"--title=Add an auto-complete",
		"--mouse",
		"--form",
		`--separator=\n`,
		"--field=No matching replacements found. Add a new one?:LBL",
		"--field=Replace:",
		"--field=With::TXT",
		"--field=In files whose names match:",
		"--width=600",
		"",
		match,
		"",
		filePattern,
	}
	cmd := exec.Command("yad", args...)
	var out bytes.Buffer
	cmd.Stdout = &out
	if err := cmd.Run(); err != nil {
		return nil, err
	}
	lines := strings.Split(out.String(), "\n")
	// We have four form fields. The first is just a label, but it produces an output line
	// anyway. The lines we're interested in are the others.
	if len(lines) < 4 {
		return nil, fmt.Errorf("missing output; only got %d lines", len(lines))
	}
	// The replacement field is a multi-line one, which means that backslash-escaping is used.
	for _, regexpFieldIndex := range []int{1, 3} {
		if _, err := regexp.Compile(lines[regexpFieldIndex]); err != nil {
			return nil, fmt.Errorf("regexp error in match '%s': %v", lines[regexpFieldIndex], err)
		}
	}
	fmt.Print(recapture(lines[2]))
	return &entry{
		Pattern:     lines[1],
		Replacement: recapture(lines[2]),
		FilePattern: lines[3],
	}, nil
}

// recapture takes an escaped string, and returns the original. Ha.
func recapture(in string) string {
	var res bytes.Buffer
	wasBackslash := false
	for _, ch := range in {
		if wasBackslash {
			res.WriteString(backslashMapping[ch])
			wasBackslash = false
		} else if ch == '\\' {
			wasBackslash = true
		} else {
			res.WriteRune(ch)
		}
	}
	return res.String()
}

var backslashMapping = map[rune]string{
	'n':  "\n",
	'r':  "\r",
	't':  "\t",
	'\\': "\\",
}

func readEntries() (*entryList, error) {
	res := &entryList{}
	bs, err := ioutil.ReadFile(subFile)
	if err != nil && !os.IsNotExist(err) {
		return nil, err
	}
	if bs != nil {
		if err := json.Unmarshal(bs, &res); err != nil {
			return nil, err
		}
	}
	return res, nil
}

func writeEntries(entries *entryList) error {
	bs, err := json.MarshalIndent(entries, "", "  ")
	if err != nil {
		return err
	}
	tmpFile := subFile + ".tmp"
	log.Printf("About to write to %s", tmpFile)
	if err := ioutil.WriteFile(tmpFile, bs, 0644); err != nil {
		return err
	}
	return os.Rename(tmpFile, subFile)
}

func readEvergreenState() (*evergreen, error) {
	contents, err := ioutil.ReadAll(os.Stdin)
	if err != nil {
		return nil, err
	}
	lineNumber, err := parseIntEnv("EVERGREEN_CURRENT_LINE_NUMBER")
	if err != nil {
		return nil, err
	}
	char, err := parseIntEnv("EVERGREEN_CURRENT_CHAR_OFFSET")
	if err != nil {
		return nil, err
	}

	lineStartIndex, err := findLineStartIndex(contents, lineNumber-1)
	if err != nil {
		return nil, err
	}
	caretIndex, err := findCaretIndex(contents, lineStartIndex, char)
	if err != nil {
		log.Printf("Error in findCaretIndex: %v", err)
	}

	return &evergreen{
		contents:        contents,
		filename:        os.Getenv("EVERGREEN_CURRENT_FILENAME"),
		line:            lineNumber - 1,
		char:            char,
		lineStartIndex:  lineStartIndex,
		lineBeforeCaret: contents[lineStartIndex:caretIndex],
	}, nil
}

func findLineStartIndex(bs []byte, line int) (int, error) {
	curLine := 0
	for res := 0; res < len(bs); {
		r, s := utf8.DecodeRune(bs[res:])
		if s == 0 {
			return 0, fmt.Errorf("invalid utf8 at offset %d", res)
		}
		res += s
		if r == '\n' {
			curLine++
			if curLine == line {
				return res, nil
			}
		}
	}
	return 0, fmt.Errorf("file too short; no line %d", line)
}

func findCaretIndex(bs []byte, lineStart, char int) (int, error) {
	curChar := 0
	for res := lineStart; res < len(bs); {
		if curChar == char {
			return res, nil
		}
		_, s := utf8.DecodeRune(bs[res:])
		if s == 0 {
			return 0, fmt.Errorf("invalid utf8 at offset %d", res)
		}
		res += s
		curChar++
	}
	return len(bs), nil
}

func parseIntEnv(name string) (int, error) {
	v64, err := strconv.ParseInt(os.Getenv(name), 10, 64)
	return int(v64), err
}

func main() {
	entries, err := readEntries()
	if err != nil {
		log.Fatal(err)
	}
	state, err := readEvergreenState()
	if err != nil {
		log.Fatal(err)
	}
	for _, entry := range entries.Entries {
		did, err := entry.substituteIfYouCan(state)
		if err != nil {
			log.Printf("Substitution failed: %v", err)
		}
		if did {
			// It's done. That's all we have to do.
			return
		}
	}
	// No substitution was made, so solicit a new one, based on the current situation.
	matchSuggestion := strings.TrimSpace(string(state.lineBeforeCaret))
	i := strings.LastIndexAny(state.filename, "/.")
	pathSuggestion := state.filename
	if i != -1 {
		esc := ""
		if state.filename[i] == '.' {
			esc = `\`
		}
		pathSuggestion = ".*" + esc + state.filename[i:]
	}
	entry, err := queryForNewEntry(matchSuggestion, pathSuggestion)
	if err != nil {
		log.Fatal(err)
	}
	entries.Entries = append(entries.Entries, entry)
	if err := writeEntries(entries); err != nil {
		log.Fatal(err)
	}
}
