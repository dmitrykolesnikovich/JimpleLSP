package magpiebridge.jimplelsp.resolver;

import de.upb.swt.soot.core.frontend.ResolveException;
import de.upb.swt.soot.core.jimple.Jimple;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.core.signatures.Signature;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.core.types.Type;
import de.upb.swt.soot.jimple.JimpleBaseListener;
import de.upb.swt.soot.jimple.parser.JimpleConverterUtil;
import de.upb.swt.soot.jimple.JimpleParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nullable;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.Position;

/**
 * This Class holds information about (open) jimple files. Especially range information about
 * occurences of Signature's or even Siganture+Identifier.
 *
 * @author Markus Schmidt
 */
public class SignaturePositionResolver {
  private final SignatureOccurenceAggregator occurences = new SignatureOccurenceAggregator();
  private final Path fileUri;
  private final JimpleConverterUtil util;

  public SignaturePositionResolver(Path fileUri) throws IOException {
    this (fileUri, CharStreams.fromPath(fileUri));
  }

  public SignaturePositionResolver(Path fileUri, String content) {
    this(fileUri, CharStreams.fromString(content));
  }

  private SignaturePositionResolver(Path fileUri, CharStream charStream) {
    this.fileUri = fileUri;
    util = new JimpleConverterUtil(fileUri);
    JimpleParser parser = JimpleConverterUtil.createJimpleParser(charStream, fileUri);

    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(occurences, parser.file() );
  }

  @Nullable
  public Signature resolve(org.eclipse.lsp4j.Position position) {
    return occurences.resolve(position);
  }

  private final class SignatureOccurenceAggregator extends JimpleBaseListener {
    SmartDatastructure positionContainer = new SmartDatastructure();
    ClassType clazz;

    @Override
    public void enterFile(JimpleParser.FileContext ctx  ) {
      if (ctx.classname == null) {
        throw new ResolveException(
            "Identifier for this unit is not found.", fileUri, JimpleConverterUtil.buildPositionFromCtx(ctx));
      }
      String classname = Jimple.unescape(ctx.classname.getText());
      clazz = util.getClassType(classname);
      positionContainer.add(ctx.classname.start, ctx.classname.stop, clazz, null);

      if (ctx.extends_clause() != null) {
        ClassType superclass = util.getClassType(ctx.extends_clause().classname.getText());
        positionContainer.add(
            ctx.extends_clause().classname.start, ctx.extends_clause().classname.stop, superclass, null);
      }

      if (ctx.implements_clause() != null) {
        List<ClassType> interfaces = util.getClassTypeList(ctx.implements_clause().type_list());
        for (int i = 0, interfacesSize = interfaces.size(); i < interfacesSize; i++) {
          ClassType anInterface = interfaces.get(i);
          final JimpleParser.TypeContext interfaceToken =
              ctx.implements_clause().type_list().type(i);
          positionContainer.add(interfaceToken.start, interfaceToken.stop, anInterface, null);
        }
      }

      super.enterFile(ctx);
    }

    @Override
    public void enterMethod(JimpleParser.MethodContext ctx) {
      // parsing the declaration

      Type type = util.getType(ctx.type().getText());
      if (type == null) {
        throw new ResolveException(
            "Returntype not found.", fileUri, JimpleConverterUtil.buildPositionFromCtx(ctx));
      }
      String methodname = ctx.method_name().getText();
      if (methodname == null) {
        throw new ResolveException(
            "Methodname not found.", fileUri, JimpleConverterUtil.buildPositionFromCtx(ctx));
      }

      List<Type> params = util.getTypeList(ctx.type_list());
      MethodSignature methodSignature =
          util.getIdentifierFactory()
              .getMethodSignature(
                  Jimple.unescape(methodname), clazz, type, params);
      positionContainer.add(ctx.start, ctx.stop, methodSignature, null);

      if (ctx.throws_clause() != null) {
        List<ClassType> exceptions = util.getClassTypeList(ctx.throws_clause().type_list());
        for (int i = 0, exceptionsSize = exceptions.size(); i < exceptionsSize; i++) {
          final JimpleParser.TypeContext typeContext = ctx.throws_clause().type_list().type(i);
          positionContainer.add(typeContext.start, typeContext.stop, exceptions.get(i), null);
        }
      }

      super.enterMethod(ctx);
    }

    @Override
    public void enterMethod_signature(JimpleParser.Method_signatureContext ctx) {
      positionContainer.add(ctx.start, ctx.stop, util.getMethodSignature(ctx, null), null);
      super.enterMethod_signature(ctx);
    }

    @Override
    public void enterField_signature(JimpleParser.Field_signatureContext ctx) {
      positionContainer.add(ctx.start, ctx.stop, util.getFieldSignature(ctx), null);
      super.enterField_signature(ctx);
    }

    @Override
    public void enterImportItem(JimpleParser.ImportItemContext ctx) {
      util.addImport(ctx);
      super.enterImportItem(ctx);
    }

    @Nullable
    Signature resolve(org.eclipse.lsp4j.Position position) {
      final Pair<Signature, String> resolve = positionContainer.resolve(position);
      if (resolve == null) {
        return null;
      }
      return resolve.getLeft();
    }
  }

}
