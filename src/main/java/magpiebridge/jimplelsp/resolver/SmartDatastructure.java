package magpiebridge.jimplelsp.resolver;

import de.upb.swt.soot.core.signatures.Signature;
import de.upb.swt.soot.jimple.parser.JimpleConverterUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

// TODO: improve ds: implement or use dependency for sth like segment/intervaltree
class SmartDatastructure {

  @Nonnull List<Position> startPositions = new ArrayList<>();
  @Nonnull List<Position> endPositions = new ArrayList<>();
  @Nonnull List<Signature> signaturesAndIdentifiers = new ArrayList<>();

  Comparator<Position> comparator = new PositionComparator();

  void add(ParserRuleContext token, Signature sig) {
    // insert sorted to be accessed via binary search
    // lsp is zero indexed; antlrs line not
    final Position startPos =
        new Position(token.start.getLine() - 1, token.start.getCharPositionInLine());
    int idx = Collections.binarySearch(startPositions, startPos, new PositionComparator());
    if (idx < 0) {
      // calculate insertion index
      idx = -idx - 1;
    } else {
      throw new IllegalStateException("position " + startPos + " is already taken.");
    }

    startPositions.add(idx, startPos);
    final de.upb.swt.soot.core.model.Position position =
        JimpleConverterUtil.buildPositionFromCtx(token);
    endPositions.add(idx, new Position(position.getLastLine(), position.getLastCol()));

    signaturesAndIdentifiers.add(idx, sig);
  }

  @Nullable
  Pair<Signature, Range> resolve(Position position) {
    if (startPositions.isEmpty()) {
      return null;
    }
    int index =
        Collections.binarySearch(startPositions, position, PositionComparator.getInstance());
    if (index < 0) {
      // not exactly found: check if next smaller neighbour is surrounding it
      index = (-index) - 1 - 1;
    } else if (index >= startPositions.size()) {
      // not exactly found: (greater than last element) check if next smaller neighbour is
      // surrounding it
      index = index - 1;
    }

    if (index < 0) {
      return null;
    }

    final Position startPos = startPositions.get(index);
    final Position endPos = endPositions.get(index);
    if (comparator.compare(startPos, position) <= 0 && comparator.compare(position, endPos) <= 0) {
      return Pair.of(signaturesAndIdentifiers.get(index), new Range(startPos, endPos));
    }
    return null;
  }
}
