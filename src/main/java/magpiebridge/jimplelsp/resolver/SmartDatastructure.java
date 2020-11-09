package magpiebridge.jimplelsp.resolver;

import de.upb.swt.soot.core.signatures.Signature;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.Position;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// TODO: improve ds: implement or use dependency for sth like segment/intervaltree
class SmartDatastructure {

  @Nonnull
  List<Position> startPositions = new ArrayList<>();
  @Nonnull List<Position> endPositions = new ArrayList<>();
  @Nonnull List<Pair<Signature, String>> signaturesAndIdentifiers = new ArrayList<>();

  Comparator<Position> comparator = new PositionComparator();

  void add(ParserRuleContext token, Signature sig, @Nullable String identifier) {
    // insert sorted to be accessed via binary search
    final Position startPos = new Position(token.start.getLine(), token.start.getCharPositionInLine());
    int idx = Collections.binarySearch(startPositions, startPos, new PositionComparator());
    if(idx < 0){
      // calculate insertion index
      idx = -idx-1;
    }else{
      throw new IllegalStateException("position "+startPos+" is already taken.");
    }

    startPositions.add(idx, startPos);

    // calc end position (line number+char offset in line) as antlr is not capable to do it intuitively
    String tokenstr = token.getText();
    int lineCount = 0;
    int fromIdx = 0;
    int lastLineBreakIdx = 0;
    while ((fromIdx = tokenstr.indexOf("\n", fromIdx)) != -1 ){
      lastLineBreakIdx = fromIdx;
      lineCount++;
      fromIdx++;
    }

    int endCharLength = tokenstr.length()-lastLineBreakIdx;

    int line = token.stop.getLine();
    int charPositionInLine = token.stop.getCharPositionInLine();
    endPositions.add(idx, new Position(line+lineCount, charPositionInLine+endCharLength));

    signaturesAndIdentifiers.add(idx, Pair.of(sig, identifier));
  }

  @Nullable
  Pair<Signature, String> resolve(Position position) {
    int index = Collections.binarySearch(startPositions, position, PositionComparator.getInstance());

    if (index < 0) {
      // not exactly found: check if next smaller neighbour is surrounding it
      index = (-index)-1-1;
    } else if (index >= startPositions.size()) {
      // not exactly found: (greater than last element) check if next smaller neighbour is surrounding it
      index = index-1;
    }

    if (comparator.compare(startPositions.get(index), position) <= 0
        && comparator.compare(position, endPositions.get(index)) <= 0) {
      return signaturesAndIdentifiers.get(index);
    }
    return null;
  }
}
