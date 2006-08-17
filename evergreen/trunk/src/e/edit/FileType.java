package e.edit;

public enum FileType {
    PLAIN_TEXT("Plain Text"),
    BASH("Bash"),
    C_PLUS_PLUS("C++"),
    JAVA("Java"),
    MAKE("Make"),
    RUBY("Ruby"),
    PERL("Perl"),
    PYTHON("Python"),
    VHDL("VHDL");
    
    private final String name;
    
    private FileType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
