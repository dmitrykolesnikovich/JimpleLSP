package magpiebridge.jimplelsp;


import de.upb.swt.soot.core.IdentifierFactory;
import de.upb.swt.soot.core.frontend.ResolveException;
import de.upb.swt.soot.core.model.Position;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.core.signatures.PackageName;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.core.types.Type;
import de.upb.swt.soot.core.util.StringTools;
import de.upb.swt.soot.java.core.JavaIdentifierFactory;
import de.upb.swt.soot.jimple.JimpleBaseListener;
import de.upb.swt.soot.jimple.JimpleLexer;
import de.upb.swt.soot.jimple.JimpleParser;
import de.upb.swt.soot.jimple.parser.JimpleConverter;
import org.antlr.v4.runtime.*;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.security.Signature;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This Class holds information about (open) jimple files. Especially range information about occurences of Signature's or even Siganture+Identifier.
 *
 * @author Markus Schmidt
 */
public class JimpleDocumentPositionResolver {
  Collection<Signature> occurences = new ArrayList<>();
  private String fileUri;

  public JimpleDocumentPositionResolver(String fileUri, String contents) {
    this.fileUri = fileUri;

    JimpleLexer lexer = new JimpleLexer(CharStreams.fromString(contents));
    TokenStream tokens = new CommonTokenStream(lexer);
    JimpleParser parser = new JimpleParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        throw new ResolveException("Jimple Syntaxerror: " + msg, fileUri, new Position(line, charPositionInLine, -1, -1));
      }
    });

    parser.file();

  }

  private final class OccurenceAggregator extends JimpleBaseListener {

    // TODO: create JimpleConverterUtil in Soot -> e.g. to support import resolving
    JimpleConverterUtil util = new JimpleConverterUtil();

    @Override
    public void enterMethod(JimpleParser.MethodContext ctx) {
      // TODO: implement parsing the declaration

      super.enterMethod(ctx);
    }

    @Override
    public void enterMethod_signature(JimpleParser.Method_signatureContext ctx) {
      // TODO: do sth
      // ctx.class_name;
      ctx.method_subsignature().type();
      ctx.method_subsignature().type_list();
      ctx.method_subsignature().method_name();

      super.enterMethod_signature(ctx);
    }

    @Override
    public void enterField(JimpleParser.FieldContext ctx) {
      // (declaration)

      // TODO: do sth
      ctx.type().identifier();
      ctx.identifier();
      // occurences.add();


      super.exitField(ctx);
    }

    @Override
    public void enterField_signature(JimpleParser.Field_signatureContext ctx) {
      // TODO:
      super.enterField_signature(ctx);
    }

    @Override
    public void enterImportItem(JimpleParser.ImportItemContext ctx) {
      util.addImport(ctx, fileUri);
      super.enterImportItem(ctx);
    }
  }

  private final class SegmentTree {
    int[] tree;
    // FIXME: implement or dependency
  }

  // TODO: move up into to Soot
  private class JimpleConverterUtil {
    final IdentifierFactory identifierFactory = JavaIdentifierFactory.getInstance();
    private Map<String, PackageName> imports = new HashMap<>();

    private Type getType(String typename) {
      typename = StringTools.getUnEscapedStringOf(typename);
      PackageName packageName = imports.get(typename);
      return packageName == null ? identifierFactory.getType(typename) : identifierFactory.getType(packageName.getPackageName() + "." + typename);
    }

    private ClassType getClassType(String typename) {
      typename = StringTools.getUnEscapedStringOf(typename);
      PackageName packageName = this.imports.get(typename);
      return packageName == null ? this.identifierFactory.getClassType(typename) : this.identifierFactory.getClassType(typename, packageName.getPackageName());
    }

    @Nonnull
    private Position buildPositionFromCtx(@Nonnull ParserRuleContext ctx) {
      return new Position(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.stop.getLine(), ctx.stop.getCharPositionInLine());
    }

    public void addImport(JimpleParser.ImportItemContext item, @Nonnull String fileuri) {
      if (item == null || item.location == null) {
        return;
      }
      final ClassType classType = identifierFactory.getClassType(item.location.getText());
      final PackageName duplicate = imports.putIfAbsent(classType.getClassName(), classType.getPackageName());
      if (duplicate != null) {
        throw new ResolveException("Multiple Imports for the same ClassName can not be resolved!", fileuri, buildPositionFromCtx(item));
      }
    }

    @Nonnull
    private MethodSignature getMethodSignature(JimpleParser.Method_signatureContext ctx, ParserRuleContext parentCtx) {
      if (ctx == null) {
        throw new ResolveException("MethodSignature is missing.", fileUri, buildPositionFromCtx(parentCtx));
      } else {
        JimpleParser.IdentifierContext class_name = ctx.class_name;
        JimpleParser.TypeContext typeCtx = ctx.method_subsignature().type();
        JimpleParser.Method_nameContext method_nameCtx = ctx.method_subsignature().method_name();
        if (class_name != null && typeCtx != null && method_nameCtx != null) {
          String classname = class_name.getText();
          Type type = getType(typeCtx.getText());
          String methodname = method_nameCtx.getText();
          List<Type> params = getTypeList(ctx.method_subsignature().type_list());
          return identifierFactory.getMethodSignature(methodname, getClassType(classname), type, params);
        } else {
          throw new ResolveException("MethodSignature is not well formed.", fileUri, buildPositionFromCtx(ctx));
        }
      }
    }

    List<Type> getTypeList(JimpleParser.Type_listContext type_list) {
      if (type_list == null) {
        return Collections.emptyList();
      } else {
        List<JimpleParser.TypeContext> typeList = type_list.type();
        int size = typeList.size();
        if (size < 1) {
          return Collections.emptyList();
        } else {
          List<Type> list = new ArrayList(size);
          Iterator var5 = typeList.iterator();

          while (var5.hasNext()) {
            JimpleParser.TypeContext typeContext = (JimpleParser.TypeContext) var5.next();
            list.add(identifierFactory.getType(typeContext.getText()));
          }

          return list;
        }
      }
    }


  }
}
