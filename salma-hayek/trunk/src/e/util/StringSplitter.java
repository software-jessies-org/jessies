package e.util;

import java.util.regex.*;

/**
 * Like String.split, but (a) lets you compile the regular expression just once
 * and (b) hands you instances of CharSequence directly, as it finds them.
 * 
 * This class is supposed to be equivalent to split().each() in Ruby.
 * 
 * At the time of writing, I don't have a use for this, but I wonder if its
 * availability will suggest uses. I always use the idiom in Ruby.
 */
public abstract class StringSplitter {
    private Pattern pattern;
    
    public StringSplitter(String regex) {
        pattern = Pattern.compile(regex);
    }
    
    public abstract void processChunk(CharSequence chunk);
    
    public void split(CharSequence input) {
        Matcher matcher = pattern.matcher(input);
        int index = 0;
        while (matcher.find()) {
            processChunk(input.subSequence(index, matcher.start()));
            index = matcher.end();
        }
        processChunk(input.subSequence(index, input.length()));
    }
    
    public static void main(String[] argValues) {
        // string.split(";").each() { |chunk| puts(chunk) }
        new StringSplitter(";") {
            public void processChunk(CharSequence chunk) {
                System.out.println(chunk);
            }
        }.split("this;is;;a;list;separated;by;semi-colons;");
        
        // Count the chunks without creating String instances or an array, or
        // an internal ArrayList, or needing more than one CharSequence
        // instance at any one time.
        class Counter extends StringSplitter {
            public int count = 0;
            public Counter() {
                super(";");
            }
            public void processChunk(CharSequence chunk) {
                ++count;
            }
        }
        Counter counter = new Counter();
        counter.split("one;two;three;four");
        System.out.println(counter.count);
    }
}
