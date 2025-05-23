package e.edit;

import e.ptextarea.*;
import e.util.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.*;
import org.eclipse.lsp4j.launch.*;
import org.eclipse.lsp4j.services.*;

import java.util.List;
import org.eclipse.lsp4j.Range;

public class LSP {
    private static ArrayList<Client> lsps;
    
    public static void init() {
        // TODO: When will this be called? On config reload would be ideal, but managing a nice
        // elegant transition between LSP configs is challenging.
        HashSet<String> seenNames = new HashSet<>();
        ArrayList<Client> lsps = new ArrayList<>();
        for (Map.Entry<String, String> entry : Parameters.getStringsTrimmed("lsp.").entrySet()) {
            String lspName = entry.getKey();
            int dot = lspName.indexOf(".");
            if (dot > -1) {
                lspName = lspName.substring(0, dot);
            }
            if (seenNames.contains(lspName)) {
                continue;
            }
            seenNames.add(lspName);
            
            String paramPrefix = "lsp." + lspName + ".";
            Map<String, String> params = Parameters.getStringsTrimmed(paramPrefix);
            Pattern pattern = null;
            
            // Do all the checking first, so we can report all the errors, and then bail out just
            // before we create the actual LSP client.
            boolean badLSP = false;
            for (String req : Arrays.asList("pattern", "command", "languages")) {
                if (params.get(req) == null) {
                    Log.warn("Missing LSP parameter " + paramPrefix + req);
                    badLSP = true;
                }
            }
            try {
                pattern = Pattern.compile(params.get("pattern"));
            } catch (PatternSyntaxException ex) {
                Log.warn("Bad regex pattern in " + paramPrefix + "pattern", ex);
                badLSP = true;
            }
            int sortPriority = 0;
            String priorityStr = params.get("priority");
            if (priorityStr != null) {
                try {
                    sortPriority = Integer.parseInt(priorityStr);
                } catch (NumberFormatException ex) {
                    Log.warn("Bad number format for property " + paramPrefix + "priority", ex);
                    badLSP = true;
                }
            }
            if (badLSP) {
                continue;
            }
            
            String[] command = params.get("command").split(" ");
            HashSet<String> languages = new HashSet<>();
            for (String language : params.get("languages").split(",")) {
                languages.add(language);
            }
            Client lsp = new Client(lspName, command, languages, pattern, params.get("replace"), sortPriority);
            lsps.add(lsp);
        }
        Collections.sort(lsps);
        LSP.lsps = lsps;
    }
    
    public static void workspaceOpened(Workspace wksp) {
        for (Client client : lsps) {
            client.addWorkspace(wksp.getCanonicalRootDirectory());
        }
    }
    
    public static void workspaceClosed(Workspace wksp) {
        for (Client client : lsps) {
            client.removeWorkspace(wksp.getCanonicalRootDirectory());
        }
    }
    
    public static FileClient clientFor(ETextWindow window) {
        for (Client client : lsps) {
            FileClient res = client.fileClientFor(window);
            if (res != null) {
                return res;
            }
        }
        return null;
    }
    
    public interface FileClient {
        /** dispose is called when the file is closed and the ETextWindow goes away. */
        public void dispose();
        
        /**
         * suggestCompletionsAt suggests auto-completion possibilities at the given coordinates.
         * This function will block. TODO: Implement non-blocking stuff, with GUI feedback and
         * the possibility to cancel. I don't really want Evergreen to lock up just because I try
         * to auto-complete while my train is going through a tunnel.
         */
        public List<Completion> suggestCompletionsAt(PCoordinates coords);
    }
    
    // These structs are not very java-like, but quite frankly having to create fields, getters
    // and setters for everything is a real pain. Instead, I'm using final instance variables
    // and ensuring the List is an unmodifiable one, so we end up with const data structures.
    public static class Completion {
        public final CompletionEdit edit;
        public final String documentation;
        public final List<CompletionEdit> otherEdits;
        
        public Completion(CompletionEdit edit, String documentation, List<CompletionEdit> otherEdits) {
            this.edit = edit;
            this.documentation = documentation;
            if (otherEdits == null) {
                otherEdits = Collections.emptyList();
            }
            this.otherEdits = Collections.unmodifiableList(otherEdits);
        }
        
        public String toString() {
            return edit.text;
        }
    }
    
    public static class CompletionEdit {
        public final int start;
        public final int end;
        public final String text;
        
        public CompletionEdit(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }
    
    private static class Client implements Comparable<Client>, Runnable, org.eclipse.lsp4j.services.LanguageClient {
        private final String name;
        private final String[] command;
        private final Pattern pattern;
        private final String replace;
        private final Set<String> languages;
        private final int sortPriority;
        
        private Thread reconnectThread;
        private LanguageServer langServer;
        private LinkedList<Runnable> requestQueue = new LinkedList<>();
        
        // openFiles holds all the currently-open FileClient implementations in this Client.
        // This will allow us to potentially have more than one ETextWindow open for the same
        // underlying PTextBuffer. That's not implemented yet, but I hope that it will happen
        // at some point.
        // If asked to return a FileClient for a file that's already open, we just return the
        // same one without adding any further listeners.
        private HashMap<Path, FileClientImpl> openFiles = new HashMap<>();
        
        // openWorkspaces tracks the currently-active workspaces registered with this LSP.
        // This is used if we have to reconnect to the LSP server (because the connection failed,
        // LSP crashed, or whatever other reason), so that we can re-initialise it with the same
        // set of workspaces that we've been asked to open by Evergreen.
        private HashSet<String> openWorkspaces = new HashSet<>();
        
        public Client(String name, String[] command, Set<String> languages, Pattern pattern, String replace, int sortPriority) {
            this.name = name;
            this.command = command;
            this.languages = languages;
            this.pattern = pattern;
            this.replace = replace;
            this.sortPriority = sortPriority;
        }
        
        public FileClient fileClientFor(ETextWindow window) {
            PTextArea textArea = window.getTextArea();
            String lspLang = textArea.getFileType().getLspLanguage();
            if (!languages.contains(lspLang)) {
                return null;
            }
            Path remotePath = window.getPath();
            Matcher matcher = pattern.matcher(remotePath.toString());
            if (!matcher.matches()) {
                return null;
            }
            if (replace != null) {
                remotePath = Paths.get(matcher.replaceFirst(replace));
            }
            FileClientImpl res = openFiles.get(remotePath);
            if (res != null) {
                res.increaseRefCount();
                return res;
            }
            res = new FileClientImpl(remotePath, textArea);
            openFiles.put(remotePath, res);
            textArea.getTextBuffer().addTextListener(res);
            Log.warn("Returning new file client for LSP " + name + " for " + remotePath.toString());
            res.sendOpened(lspLang, textArea.getTextBuffer().toString());
            return res;
        }
        
        private class FileClientImpl implements FileClient, PTextListener {
            // TODO: This PTextArea reference shouldn't be here, as we really want to be able to
            // support multiple PTextAreas showing the same file, and using the same FileClientImpl.
            // However, for now the PTextArea holds both the split lines (which depend on the width
            // of the text area) and the PLineList (which doesn't). We need the PLineList so as to
            // transform between byte offsets and line/char offsets, so we need access to the PTextArea.
            // When we get around to supporting multiple views of the same file, we'll need to refactor
            // the PTextArea to split the PLineList outside of the component itself, at which point
            // we should also fix this.
            private final PTextArea textArea;
            private final Path remotePath;
            private int version = 0;
            private int refCount = 1;  // Freshly-constructed instances always have one client.
            
            public FileClientImpl(Path remotePath, PTextArea textArea) {
                this.remotePath = remotePath;
                this.textArea = textArea;
            }
            
            public void increaseRefCount() {
                refCount++;
            }
            
            private String uri() {
                return remotePath.toUri().toString();
            }
            
            public void sendOpened(final String lspLang, final String content) {
                enqueue(new Runnable() {
                    public void run() {
                        TextDocumentItem item = new TextDocumentItem(uri(), lspLang, 0, content);
                        langServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(item));
                    }
                });
            }
            
            public void dispose() {
                Log.warn("dispose on LSP client for " + remotePath.toString());
                refCount--;
                if (refCount > 0) {
                    return;
                }
                openFiles.remove(remotePath);
                enqueue(new Runnable() {
                    public void run() {
                        Log.warn("Sending didClose to LSP " + name + " for " + uri());
                        TextDocumentIdentifier id = new TextDocumentIdentifier(uri());
                        langServer.getTextDocumentService().didClose(new DidCloseTextDocumentParams(id));
                    }
                });
            }
            
            public List<Completion> suggestCompletionsAt(PCoordinates coords) {
                Position pos = new Position(coords.getLineIndex(), coords.getCharOffset());
                CompletionParams params = new CompletionParams(new TextDocumentIdentifier(uri()), pos);
                CompletableFuture<Either<List<CompletionItem>, CompletionList>> blockingRes = langServer.getTextDocumentService().completion(params);
                // Warning: the '.get()' here will block! Consider doing something more clever and adding something
                // that can be cancelled. For example, some visual display that we're waiting for the LSP, and if
                // the user hits Escape or starts typing, we just auto-cancel the request.
                Either<List<CompletionItem>, CompletionList> resp;
                try {
                    resp = blockingRes.get();
                } catch (ExecutionException ex) {
                    Log.warn("Completion execution failed.", ex);
                    return null;
                } catch (InterruptedException ex) {
                    Log.warn("Completion interrupted.", ex);
                    return null;
                }
                // A CompletionList is simply a wrapper around a List<CompletionItem> with an extra bool to say
                // whether the list is complete or not. Just pull out the list, as we don't care about anything else.
                List<CompletionItem> items = resp.isLeft() ? resp.getLeft() : resp.getRight().getItems();
                ArrayList<Completion> res = new ArrayList<>();
                for (CompletionItem item : items) {
                    ArrayList<CompletionEdit> otherEdits = new ArrayList<>();
                    if (item.getAdditionalTextEdits() != null) {
                        for (TextEdit edit : item.getAdditionalTextEdits()) {
                            otherEdits.add(editAt(edit.getRange(), edit.getNewText()));
                        }
                    }
                    CompletionEdit mainEdit;
                    if (item.getTextEdit().isLeft()) {
                        TextEdit edit = item.getTextEdit().getLeft();
                        mainEdit = editAt(edit.getRange(), edit.getNewText());
                    } else {
                        // I don't think I'm going to be getting the InsertReplaceEdit items here,
                        // as they apparently need to be explicitly requested. Log if we do though,
                        // as we need to check that we're dealing with them correctly. The javadoc
                        // is pretty vague on the difference between an 'insert' and a 'replace'.
                        InsertReplaceEdit edit = item.getTextEdit().getRight();
                        Log.warn("Got an InsertReplaceEdit: " + edit.toString());
                        Range range = edit.getInsert();
                        if (range == null) {
                            range = edit.getReplace();
                        }
                        mainEdit = editAt(range, edit.getNewText());
                    }
                    String doc = null;
                    if (item.getDocumentation().isLeft()) {
                        doc = item.getDocumentation().getLeft();
                    } else if (item.getDocumentation().isRight()) {
                        // We got some markup content. For now let's just treat it as text, as we don't
                        // have support for formatting markup.
                        doc = item.getDocumentation().getRight().getValue();
                    }
                    String text = item.getInsertText();
                    if (text == null || text.equals("")) {
                        text = item.getLabel();
                    }
                    res.add(new Completion(mainEdit, doc, otherEdits));
                }
                return res;
            }
            
            private CompletionEdit editAt(Range range, String text) {
                return new CompletionEdit(indexOf(range.getStart()), indexOf(range.getEnd()), text);
            }
            
            private int indexOf(Position pos) {
                return textArea.getTextIndex(new PCoordinates(pos.getLine(), pos.getCharacter()));
            }
            
            private VersionedTextDocumentIdentifier newVersionID() {
                version++;
                return new VersionedTextDocumentIdentifier(uri(), version);
            }
            
            /** Notification that some text has been inserted into the PText. */
            public void textInserted(PTextEvent event) {
                sendMessage(event);
            }
            
            /** Notification that some text has been removed from the PText. */
            public void textRemoved(PTextEvent event) {
                sendMessage(event);
            }
            
            /** Notification that all of the text held within the PText object has been completely replaced. */
            public void textCompletelyReplaced(PTextEvent event) {
                sendMessage(event);
            }
            
            private void sendMessage(final PTextEvent event) {
                final ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
                Range range = null;
                String content = event.isRemove() ? "" : event.getCharacters().toString();
                if (!event.isCompleteReplacement()) {
                    PCoordinates coords = textArea.getCoordinates(event.getOffset());
                    range = new Range();
                    range.setStart(new Position(coords.getLineIndex(), coords.getCharOffset()));
                    if (event.isRemove()) {
                        // We have to compute the end coordinates based on the start coordinates and
                        // the contents of the PTextEvent. We must *NOT* try to fetch the equivalent
                        // textArea coordinates of the event end, as the removal event has already
                        // been applied to the textArea.
                        range.setEnd(computeRangeEnd(range.getStart(), event.getCharacters()));
                    } else {
                        range.setEnd(range.getStart());
                    }
                }
                changes.add(new TextDocumentContentChangeEvent(range, content));
                enqueue(new Runnable() {
                    public void run() {
                        //Log.warn("Sending change (caused by " + event.toString() + "): " + changes.get(0));
                        langServer.getTextDocumentService().didChange(new DidChangeTextDocumentParams(newVersionID(), changes));
                    }
                });
            }
        }
        
        private Position computeRangeEnd(Position start, CharSequence chars) {
            int line = start.getLine();
            int character = start.getCharacter();
            for (int i = 0; i < chars.length(); i++) {
                if (chars.charAt(i) == '\n') {
                    line++;
                    character = 0;
                } else {
                    character++;
                }
            }
            return new Position(line, character);
        }
        
        private void enqueue(Runnable runnable) {
            synchronized (requestQueue) {
                requestQueue.add(runnable);
            }
        }
        
        public void run() {
            final String prefix = "LSP " + name + ": ";
            boolean firstRun = true;
            while (true) {
                try {
                    Log.warn("Launching LSP " + name + " command: " + String.join(" ", command));
                    final Process proc = Runtime.getRuntime().exec(command);
                    Runnable stderrLogger = new Runnable() {
                        public void run() {
                            try {
                                BufferedReader in = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                                String line;
                                while ((line = in.readLine()) != null) {
                                    Log.warn(prefix + line);
                                }
                                in.close();
                            } catch (IOException ex) {
                                Log.warn(prefix + "read failure; shutting down", ex);
                            }
                        }
                    };
                    (new Thread(stderrLogger, prefix + " stderr logger")).start();
                    Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(this, proc.getInputStream(), proc.getOutputStream());
                    launcher.startListening();
                    LanguageServer langSrv = launcher.getRemoteProxy();
                    InitializeParams initParams = new InitializeParams();
                    initParams.setClientInfo(new ClientInfo("Evergreen"));
                    CompletableFuture<InitializeResult> initRes = langSrv.initialize(initParams);
                    // The following will block until init completes.
                    Log.warn("Waiting for " + prefix + " initialisation to complete...");
                    InitializeResult res = initRes.get();
                    Log.warn(prefix + " initialisation completed; server is " + res.getServerInfo());
                    CompletionOptions compOpts = res.getCapabilities().getCompletionProvider();
                    // As we got the InitializeResult, we now have to tell the langSrv that we're initialised,
                    // before we try sending any other messages to it.
                    langSrv.initialized(new InitializedParams());
                    // Now that everything's initialised and ready, we can set the langServer reference
                    // to let others use it.
                    synchronized(this) {
                        langServer = langSrv;
                    }
                    // If this is a reconnect, then we need to poke the lang server with whatever
                    // workspaces were added before.
                    if (firstRun) {
                        firstRun = false;
                    } else {
                        reopenWorkspaces();
                    }
                    // Run everything in the request queue as it appears, also watching the status of
                    // the sub-process to check it's not exited. We use 10ms because it should be
                    // responsive, while not hogging much CPU.
                    while (!proc.waitFor(10, TimeUnit.MILLISECONDS)) {
                        Runnable[] queueCopy;
                        synchronized (requestQueue) {
                            queueCopy = requestQueue.toArray(new Runnable[requestQueue.size()]);
                            requestQueue.clear();
                        }
                        for (Runnable runnable : queueCopy) {
                            runnable.run();
                        }
                    }
                    Log.warn(prefix + " sub-process terminated");
                } catch (Exception ex) {
                    Log.warn("Failed to start " + prefix + " sub-process [" + String.join(" ", command) + "]", ex);
                    try {
                        Thread.sleep(10 * 1000);  // Sleep 10s, then retry.
                    } catch (InterruptedException ex2) {
                        Log.warn("Interrupted during sleep", ex2);
                    }
                    continue;
                }
            }
        }
        
        public int compareTo(Client other) {
            if (sortPriority != other.sortPriority) {
                // We consider higher sort priority to be better, but want the best sorted first,
                // so do a reverse sort.
                return other.sortPriority - sortPriority;
            }
            // If there's no explicit sort priority, we assume that a more complex matching pattern is
            // a sign of higher specificity, and so is likely to match less often. So we sort longer
            // patterns earlier so as to allow them to match in preference over shorter, more generic
            // matchers. At least, that's the idea.
            return other.pattern.toString().length() - pattern.toString().length();
        }
        
        // addWorkspace is to be called when the workspace is first activated. If this LSP is
        // configured to match the path, then we inform the LSP server. We also store the workspace
        // in openWorkspaces so that we can re-inform the LSP server if we have to restart it.
        public void addWorkspace(String path) {
            if (!pattern.matcher(path).matches()) {
                return;
            }
            // We only start the LSP when we add a workspace which it is set up to serve.
            // Therefore, this is the correct (and only necessary) place to start the thread
            // if it's not running. This function is always called from the event dispatch thread.
            if (reconnectThread == null) {
                reconnectThread = new Thread(this, "LSP " + name + " controller");
                reconnectThread.start();
            }
            synchronized(openWorkspaces) {
                openWorkspaces.add(path);
            }
            openWorkspace(path);
        }
        
        // removeWorkspace is to be called when the workspace is closed. If the LSP server has
        // been told about the workspace, it's informed of its removal.
        public void removeWorkspace(String path) {
            synchronized(openWorkspaces) {
                if (!openWorkspaces.contains(path)) {
                    return;
                }
                openWorkspaces.remove(path);
            }
        }
        
        private void reopenWorkspaces() {
            ArrayList<WorkspaceFolder> added = new ArrayList<>();
            synchronized(openWorkspaces) {
                for (String path : openWorkspaces) {
                    added.add(makeWorkspaceFolder(path));
                }
            }
            WorkspaceFoldersChangeEvent wkspEvent = new WorkspaceFoldersChangeEvent(added, Collections.emptyList());
            langServer.getWorkspaceService().didChangeWorkspaceFolders(new DidChangeWorkspaceFoldersParams(wkspEvent));
        }
        
        // openWorkspace actually informs the LSP of the open workspace, either because the user
        // activated the workspace, or because the LSP had to be restarted and we had to inform
        // it of the current state.
        private void openWorkspace(String path) {
            Log.warn("LSP " + name + " being notified of new workspace " + path.toString());
            List<WorkspaceFolder> added = Collections.singletonList(makeWorkspaceFolder(path));
            WorkspaceFoldersChangeEvent wkspEvent = new WorkspaceFoldersChangeEvent(added, Collections.emptyList());
            enqueue(new Runnable() {
                public void run() {
                    langServer.getWorkspaceService().didChangeWorkspaceFolders(new DidChangeWorkspaceFoldersParams(wkspEvent));
                }
            });
        }
        
        private WorkspaceFolder makeWorkspaceFolder(String path) {
            Path remotePath = Paths.get((replace == null) ? path : pattern.matcher(path).replaceFirst(replace));
            return new WorkspaceFolder(remotePath.toUri().toString(), remotePath.toString());
        }
        
        //
        // The following functions implement the LSPClient interface.
        //
        
        public void logMessage(MessageParams message) {
            System.err.println("logMessage");
        }
        
        public void showMessage(MessageParams message) {
            System.err.println("showMessage");
        }
        
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams params) {
            System.err.println("showMessageRequest : " + params);
            return null;
        }
        
        public void telemetryEvent(Object obj) {
            System.err.println("telemetryEvent");
        }
        
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
            System.err.println("publishDiagnostics");
        }
    }
}
