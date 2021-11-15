/*

This file is part of the Fuzion language server protocol implementation.

The Fuzion language server protocol implementation is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published
by the Free Software Foundation, version 3 of the License.

The Fuzion language server protocol implementation is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License along with The
Fuzion language implementation.  If not, see <https://www.gnu.org/licenses/>.

*/

/*-----------------------------------------------------------------------
 *
 * Tokiwa Software GmbH, Germany
 *
 * Source of class QueryASTTest
 *
 *---------------------------------------------------------------------*/


package test.flang.lsp.server.util;

import org.junit.jupiter.api.Test;

import dev.flang.ast.Call;
import dev.flang.lsp.server.SourceText;
import dev.flang.lsp.server.util.FuzionParser;
import dev.flang.lsp.server.util.LSP4jUtils;
import dev.flang.lsp.server.util.QueryAST;
import test.flang.lsp.server.BaseTest;

public class QueryASTTest extends BaseTest
{
  @Test
  public void DeclaredFeaturesRecursive()
  {
    SourceText.setText(uri(1), UnknownCall);
    SourceText.setText(uri(2), HelloWorld);
    SourceText.setText(uri(3), ManOrBoy);
    SourceText.setText(uri(4), PythagoreanTriple);
    assertEquals(1, QueryAST.DeclaredFeaturesRecursive(uri(1)).count());
    assertEquals(1, QueryAST.DeclaredFeaturesRecursive(uri(2)).count());
    assertEquals(13, QueryAST.DeclaredFeaturesRecursive(uri(3)).count());
    assertEquals(3, QueryAST.DeclaredFeaturesRecursive(uri(4)).count());
  }

  @Test
  public void AllOf()
  {
    SourceText.setText(uri(1), HelloWorld);
    assertEquals("HelloWorld", FuzionParser
      .main(uri(1))
      .get()
      .featureName()
      .baseName());
    assertEquals("say", QueryAST
      .AllOf(FuzionParser
        .main(uri(1))
        .get(), Call.class)
      .filter(call -> uri(1).equals(FuzionParser.getUri(call.pos())))
      .findFirst()
      .get()
      .calledFeature()
      .featureName()
      .baseName());
  }

  @Test
  public void callAt()
  {
    var sourceText = """
      ex7 is
        (1..10).myCall()
              """;
    SourceText.setText(uri(1), sourceText);
    var call = QueryAST.callAt(Cursor(uri(1), 1, 17))
      .get();
    assertEquals("myCall", call.name);
  }

  @Test
  public void featureAtWithinFunction()
  {
    var sourceText = """
      ex is
        (1..10).forAll(num -> say num)
                  """;
    SourceText.setText(uri(1), sourceText);

    assertEquals("infix ..", QueryAST.FeatureAt(Cursor(uri(1), 1, 4))
      .get()
      .featureName()
      .baseName());

    assertEquals("forAll", QueryAST.FeatureAt(Cursor(uri(1), 1, 10))
      .get()
      .featureName()
      .baseName());

    assertEquals("forAll", QueryAST.FeatureAt(Cursor(uri(1), 1, 16))
      .get()
      .featureName()
      .baseName());

    assertEquals("say", QueryAST.FeatureAt(Cursor(uri(1), 1, 24))
      .get()
      .featureName()
      .baseName());

    assertEquals("say", QueryAST.FeatureAt(Cursor(uri(1), 1, 26))
      .get()
      .featureName()
      .baseName());
  }

  @Test
  public void featureAtWithinLoop()
  {
    var sourceText = """
      ex8 is
        for s in ["one", "two", "three"] do
          say "$s"
                """;
    SourceText.setText(uri(1), sourceText);
    var feature = QueryAST.FeatureAt(Cursor(uri(1), 2, 4))
      .get();
    assertEquals("say", feature.featureName().baseName());
  }


  @Test
  public void featureAt()
  {
    var sourceText = """
      myfeat is

        myi32 :i32 is

        print(x, y myi32, z i32) =>
          say "$x"
      """;
    SourceText.setText(uri(1), sourceText);

    var feature = QueryAST.FeatureAt(Cursor(uri(1), 5, 4)).get();
    assertEquals("say", feature.featureName().baseName());

    feature = QueryAST.FeatureAt(Cursor(uri(1), 4, 8)).get();
    assertEquals("myi32", feature.featureName().baseName());

    feature = QueryAST.FeatureAt(Cursor(uri(1), 4, 20)).get();
    assertEquals("i32", feature.featureName().baseName());
  }

  @Test
  public void InFeature()
  {
    SourceText.setText(uri(1), ManOrBoy);
    assertEquals("a", QueryAST.InFeature(Cursor(uri(1), 4, 1)).get().featureName().baseName());
  }

}
