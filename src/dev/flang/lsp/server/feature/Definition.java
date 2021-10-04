package dev.flang.lsp.server.feature;

import java.util.Arrays;
import java.util.List;

import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import dev.flang.ast.Feature;
import dev.flang.lsp.server.Converters;
import dev.flang.lsp.server.FuzionHelpers;

/**
 * tries to provide the definition of a call
 * https://microsoft.github.io/language-server-protocol/specification#textDocument_definition
 */
public class Definition
{
  public static Either<List<? extends Location>, List<? extends LocationLink>> getDefinitionLocation(
      DefinitionParams params)
  {
    var feature = FuzionHelpers.featureAt(params);
    if(feature.isEmpty()){
      return null;
    }
    return getDefinition(feature.get());
  }

  private static Either<List<? extends Location>, List<? extends LocationLink>> getDefinition(Feature obj)
	{
    // NYI find better way
    if(obj.toString().startsWith("INVISIBLE")){
      return getDefinition(obj.outer());
    }
    Location location = Converters.ToLocation(obj.pos());
    return Either.forLeft(Arrays.asList(location));
	}

}