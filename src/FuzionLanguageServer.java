import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.HoverOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.eclipse.lsp4j.services.LanguageClient;

public class FuzionLanguageServer implements LanguageServer {

  private LanguageClient _client;

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    final InitializeResult res = new InitializeResult(new ServerCapabilities());
    var capabilities = res.getCapabilities();
    initializeCompletion(capabilities);
    initializeHover(capabilities);
    capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
    return CompletableFuture.supplyAsync(() -> res);
  }

  private void initializeHover(ServerCapabilities serverCapabilities) {
    var hoverOptions = new HoverOptions();
    hoverOptions.setWorkDoneProgress(Boolean.FALSE);
    serverCapabilities.setHoverProvider(hoverOptions);
  }

  private void initializeCompletion(ServerCapabilities serverCapabilities) {
    CompletionOptions completionOptions = new CompletionOptions();
    completionOptions.setResolveProvider(Boolean.FALSE);
    serverCapabilities.setCompletionProvider(completionOptions);
  }

  public void setClient(LanguageClient client){
    _client = client;
  }

  public LanguageClient getClient(){
    return _client;
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    return CompletableFuture.supplyAsync(() -> Boolean.TRUE);
  }

  @Override
  public void exit() {
    System.exit(0);
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return new FuzionTextDocumentService(this.getClient());
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return new FuzionWorkspaceService();
  }
}
