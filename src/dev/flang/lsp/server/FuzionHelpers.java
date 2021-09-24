package dev.flang.lsp.server;

import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;

import dev.flang.ast.Call;
import dev.flang.ast.Case;
import dev.flang.ast.Cond;
import dev.flang.ast.Contract;
import dev.flang.ast.Feature;
import dev.flang.ast.FormalGenerics;
import dev.flang.ast.Generic;
import dev.flang.ast.Impl;
import dev.flang.ast.Impl.Kind;
import dev.flang.ast.ReturnType;
import dev.flang.ast.Stmnt;
import dev.flang.ast.Type;
import dev.flang.ast.Types;
import dev.flang.util.SourcePosition;

public class FuzionHelpers
{

  public static Position ToPosition(SourcePosition sourcePosition)
  {
    return new Position(sourcePosition._line - 1, sourcePosition._column - 1);
  }

  public static Location ToLocation(SourcePosition sourcePosition)
  {
    var position = ToPosition(sourcePosition);
    return new Location(ParserHelper.getUri(sourcePosition), new Range(position, position));
  }

  // NYI remove once we have ISourcePosition interface
  /**
   * getPosition of ASTItem
   * @param entry
   * @return
   */
  public static SourcePosition getPosition(Object entry)
  {
    var result = getPositionOrNull(entry);
    if (result != null)
      {
        return result;
      }
    // Log.write("no src pos found for: " + entry.getClass());
    // NYI what to return?
    return SourcePosition.builtIn;
  }

  private static SourcePosition getPositionOrNull(Object entry)
  {
    if (entry instanceof Stmnt)
      {
        return ((Stmnt) entry).pos();
      }
    if (entry instanceof Type)
      {
        return ((Type) entry).pos;
      }
    if (entry instanceof Impl)
      {
        return ((Impl) entry).pos;
      }
    if (entry instanceof Generic)
      {
        return ((Generic) entry)._pos;
      }
    if (entry instanceof Case)
      {
        return ((Case) entry).pos;
      }
    // NYI
    if (entry instanceof ReturnType)
      {
        return SourcePosition.builtIn;
      }
    // NYI
    if (entry instanceof Cond)
      {
        return SourcePosition.builtIn;
      }
    // NYI
    if (entry instanceof FormalGenerics)
      {
        return SourcePosition.builtIn;
      }
    // NYI
    if (entry instanceof Contract)
      {
        return SourcePosition.builtIn;
      }

    System.err.println(entry.getClass());
    Util.PrintStackTraceAndExit(1);
    return null;
  }

  /**
   * given a TextDocumentPosition return all matching ASTItems
   * in the given file on the given line.
   * sorted by position descending.
   * @param params
   * @return
   */
  public static Stream<Object> getASTItemsOnLine(TextDocumentPositionParams params)
  {
    var baseFeature = getBaseFeature(params.getTextDocument());
    if (baseFeature.isEmpty())
      {
        return Stream.empty();
      }

    var astItems = HeirsVisitor.visit(baseFeature.get())
      .entrySet()
      .stream()
      .filter(IsItemNotBuiltIn(params))
      .filter(IsItemInFile(params))
      .filter(IsItemOnSameLineAsCursor(params))
      .filter(IsItemInScope(params))
      .map(entry -> entry.getKey())
      .sorted(FuzionHelpers.CompareBySourcePosition.reversed());

    return astItems;
  }

  private static Predicate<? super Entry<Object, Feature>> IsItemOnSameLineAsCursor(TextDocumentPositionParams params)
  {
    return (entry) -> {
      var astItem = entry.getKey();
      var cursorPosition = Util.getPosition(params);
      var sourcePosition = FuzionHelpers.getPosition(astItem);
      return cursorPosition.getLine() == FuzionHelpers.ToPosition(sourcePosition).getLine();
    };
  }

  private static Predicate<? super Entry<Object, Feature>> IsItemNotBuiltIn(TextDocumentPositionParams params)
  {
    return (entry) -> {
      var astItem = entry.getKey();
      var sourcePosition = FuzionHelpers.getPosition(astItem);
      return !sourcePosition.isBuiltIn();
    };
  }

  private static Predicate<? super Entry<Object, Feature>> IsItemInFile(TextDocumentPositionParams params)
  {
    return (entry) -> {
      var uri = Util.getUri(params);
      var sourcePosition = FuzionHelpers.getPosition(entry.getKey());
      return uri.equals(ParserHelper.getUri(sourcePosition));
    };
  }

  private static Predicate<? super Entry<Object, Feature>> IsItemInScope(TextDocumentPositionParams params)
  {
    return (entry) -> {
      var astItem = entry.getKey();
      var outer = entry.getValue();
      var cursorPosition = Util.getPosition(params);

      var sourcePosition = FuzionHelpers.getPosition(astItem);

      boolean EndOfOuterFeatureIsAfterCursorPosition = Util.ComparePosition(cursorPosition,
        FuzionHelpers.ToPosition(FuzionHelpers.getEndOfFeature(outer))) <= 0;
      boolean ItemPositionIsBeforeOrAtCursorPosition =
        Util.ComparePosition(cursorPosition, FuzionHelpers.ToPosition(sourcePosition)) >= 0;

      return ItemPositionIsBeforeOrAtCursorPosition && EndOfOuterFeatureIsAfterCursorPosition;
    };
  }

  /**
   * returns the outermost feature found in uri
   * @param uri
   * @return
   */
  private static Optional<Feature> getBaseFeature(TextDocumentIdentifier params)
  {
    var baseFeature = allOf(Feature.class)
      .filter(IsFeatureInFile(Util.getUri(params)))
      .sorted(CompareBySourcePosition)
      .findFirst();
    if (baseFeature.isPresent())
      {
        Log.message("baseFeature: " + baseFeature.get().qualifiedName());
      }
    return baseFeature;
  }

  public static Stream<Feature> getParentFeatures(TextDocumentPositionParams params)
  {
    // find innermost then outer() until at universe
    return Stream.empty();
  }

  private static Predicate<? super Feature> IsFeatureInFile(String uri)
  {
    return feature -> {
      return uri.equals(ParserHelper.getUri(feature.pos()));
    };
  }

  public static Comparator<? super Object> CompareBySourcePosition =
    Comparator.comparing(obj -> obj, (obj1, obj2) -> {
      var result = getPosition(obj1).compareTo(getPosition(obj2));
      if (result != 0)
        {
          return result;
        }
      return obj1.equals(obj2) ? 0: 1;
    });

  private static Comparator<? super Call> CompareByEndOfCall =
    Comparator.comparing(obj -> obj, (obj1, obj2) -> {
      var result = getEndOfCall(obj1).compareTo(getEndOfCall(obj2));
      if (result != 0)
        {
          return result;
        }
      return obj1.equals(obj2) ? 0: 1;
    });

  public static boolean IsRoutineOrRoutineDef(Feature feature)
  {
    return IsRoutineOrRoutineDef(feature.impl);
  }

  public static boolean IsRoutineOrRoutineDef(Impl impl)
  {
    return Util.HashSetOf(Kind.Routine, Kind.RoutineDef).contains(impl.kind_);
  }

  public static boolean IsFieldLike(Feature feature)
  {
    return IsFieldLike(feature.impl);
  }

  public static boolean IsFieldLike(Impl impl)
  {
    return Util.HashSetOf(Kind.Field, Kind.FieldActual, Kind.FieldDef, Kind.FieldInit, Kind.FieldIter)
      .contains(impl.kind_);
  }

  public static boolean IsIntrinsic(Feature feature)
  {
    return IsIntrinsic(feature.impl);
  }

  public static boolean IsIntrinsic(Impl impl)
  {
    return impl.kind_ == Kind.Intrinsic;
  }

  public static Stream<Feature> calledFeaturesSortedDesc(TextDocumentPositionParams params)
  {
    var baseFeature = getBaseFeature(params.getTextDocument());
    if (baseFeature.isEmpty())
      {
        return Stream.empty();
      }

    return HeirsVisitor.visit(baseFeature.get())
      .entrySet()
      .stream()
      .filter(IsItemInFile(params))
      .filter(entry -> entry.getKey() instanceof Call)
      .map(entry -> new SimpleEntry<Call, Feature>((Call) entry.getKey(), entry.getValue()))
      .filter(entry -> PositionIsAfterOrAtCursor(params, getEndOfFeature(entry.getValue())))
      .filter(entry -> PositionIsBeforeCursor(params, entry.getKey().pos()))
      .map(entry -> entry.getKey())
      .filter(c -> !IsAnonymousInnerFeature(c.calledFeature()))
      .filter(c -> c.calledFeature().resultType() != Types.t_ERROR)
      .sorted(CompareByEndOfCall.reversed())
      .map(c -> {
        Log.message("call: " + c.pos().toString());
        return c.calledFeature();
      });
  }

  private static SourcePosition getEndOfCall(Call call)
  {
    var result = call._actuals
      .stream()
      .map(expression -> expression.pos())
      .sorted(CompareBySourcePosition.reversed())
      .findFirst();
    if (result.isEmpty())
      {
        return call.pos();
      }

    return new SourcePosition(result.get()._sourceFile, result.get()._line, result.get()._column + 1);
  }

  private static boolean PositionIsAfterOrAtCursor(TextDocumentPositionParams params, SourcePosition sourcePosition)
  {
    return Util.ComparePosition(Util.getPosition(params), ToPosition(sourcePosition)) <= 0;
  }

  private static boolean PositionIsBeforeCursor(TextDocumentPositionParams params, SourcePosition sourcePosition)
  {
    return Util.ComparePosition(Util.getPosition(params), ToPosition(sourcePosition)) > 0;
  }

  /**
   * NYI replace by real end of feature once we have this information in the AST
   * !!!CACHED via Memory.EndOfFeature!!!
   * @param baseFeature
   * @return
   */
  public static SourcePosition getEndOfFeature(Feature baseFeature)
  {
    if (!Memory.EndOfFeature.containsKey(baseFeature))
      {
        SourcePosition endOfFeature = HeirsVisitor
          .visit(baseFeature)
          .entrySet()
          .stream()
          .filter(entry -> entry.getValue() != null)
          .filter(entry -> entry.getValue().compareTo(baseFeature) == 0)
          .map(entry -> getPosition(entry.getKey()))
          .sorted((Comparator<SourcePosition>) Comparator.<SourcePosition>reverseOrder())
          .map(position -> {
            return new SourcePosition(position._sourceFile, position._line, getEndColumn(position));
          })
          .findFirst()
          .orElse(baseFeature.pos());

        Memory.EndOfFeature.put(baseFeature, endOfFeature);
      }

    return Memory.EndOfFeature.get(baseFeature);
  }

  private static int getEndColumn(SourcePosition start)
  {
    var uri = ParserHelper.getUri(start);
    var line_text = FuzionTextDocumentService.getText(uri).split("\\R", -1)[start._line - 1];
    var column = start._column;
    while (line_text.length() > column && !Util.HashSetOf(')', '.', ' ').contains(line_text.charAt(column - 1)))
      {
        column++;
      }
    return column;
  }

  /**
   * @param feature
   * @return example: array<T>(length i32, init Function<array.T, i32>) => array<array.T>
   */
  public static String getLabel(Feature feature)
  {
    if (!IsRoutineOrRoutineDef(feature))
      {
        return feature.featureName().baseName();
      }
    var arguments = "(" + feature.arguments.stream()
      .map(a -> a.thisType().featureOfType().featureName().baseName() + " " + a.thisType().featureOfType().resultType())
      .collect(Collectors.joining(", ")) + ")";
    return feature.featureName().baseName() + feature.generics + arguments + " => " + feature.resultType();
  }

  public static boolean IsAnonymousInnerFeature(Feature f)
  {
    return f.featureName().baseName().startsWith("#");
  }

  public static Stream<Feature> getFeaturesDesc(TextDocumentPositionParams params)
  {
    var result = getASTItemsOnLine(params)
      .map(astItem -> {
        if (astItem instanceof Feature)
          {
            return (Feature) astItem;
          }
        if (astItem instanceof Call)
          {
            return ((Call) astItem).calledFeature();
          }
        if (astItem instanceof Type)
          {
            return ((Type) astItem).featureOfType();
          }
        return null;
      })
      .filter(f -> f != null)
      .filter(f -> !IsFieldLike(f))
      .filter(f -> !IsAnonymousInnerFeature(f))
      // NYI maybe there is a better way?
      .filter(f -> !Util.HashSetOf("Object", "Function", "call").contains(f.featureName().baseName()));

    return result;
  }

  /**
   * example: allOf(Call.class) returns all Calls
   * @param <T>
   * @param classOfT
   * @return
   */
  public static <T extends Object> Stream<T> allOf(Class<T> classOfT)
  {
    var universe = Memory.getMain().universe();
    return HeirsVisitor.visit(universe)
      .keySet()
      .stream()
      .filter(obj -> obj.getClass().equals(classOfT))
      .map(obj -> (T) obj);
  }

  /**
   * @param feature
   * @return all calls to this feature
   */
  public static Stream<Call> callsTo(Feature feature)
  {
    return allOf(Call.class)
      .filter(call -> call.calledFeature().equals(feature));
  }

}
