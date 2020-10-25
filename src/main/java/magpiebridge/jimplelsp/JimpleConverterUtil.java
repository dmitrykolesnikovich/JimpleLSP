package magpiebridge.jimplelsp;

import de.upb.swt.soot.core.IdentifierFactory;
import de.upb.swt.soot.core.frontend.ResolveException;
import de.upb.swt.soot.core.signatures.FieldSignature;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.core.signatures.PackageName;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.core.types.Type;
import de.upb.swt.soot.core.util.StringTools;
import de.upb.swt.soot.java.core.JavaIdentifierFactory;
import de.upb.swt.soot.jimple.JimpleParser;
import java.util.*;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

// TODO: move up into to Soot
/**
 * This Utility class provides common used methods in context with parsing Jimple.
 *
 * @author Markus Schmidt
 */
public class JimpleConverterUtil {

  private final IdentifierFactory identifierFactory = JavaIdentifierFactory.getInstance();
  private final Map<String, PackageName> imports = new HashMap<>();
  private final String fileUri;

  public JimpleConverterUtil(String fileUri) {
    this.fileUri = fileUri;
  }

  public IdentifierFactory getIdentifierFactory() {
    return identifierFactory;
  }

  public Type getType(String typename) {
    typename = StringTools.getUnEscapedStringOf(typename);
    PackageName packageName = imports.get(typename);
    return packageName == null
        ? identifierFactory.getType(typename)
        : identifierFactory.getType(packageName.getPackageName() + "." + typename);
  }

  public ClassType getClassType(String typename) {
    typename = StringTools.getUnEscapedStringOf(typename);
    PackageName packageName = this.imports.get(typename);
    return packageName == null
        ? this.identifierFactory.getClassType(typename)
        : this.identifierFactory.getClassType(typename, packageName.getPackageName());
  }

  @Nonnull
  public static Range buildRangeFromCtx(@Nonnull ParserRuleContext ctx) {
    return new Range(
        new Position(ctx.start.getLine(), ctx.start.getCharPositionInLine()),
        new Position(ctx.stop.getLine(), ctx.stop.getCharPositionInLine()));
  }

  @Nonnull
  public static de.upb.swt.soot.core.model.Position buildPositionFromCtx(
      @Nonnull ParserRuleContext ctx) {
    return new de.upb.swt.soot.core.model.Position(
        ctx.start.getLine(),
        ctx.start.getCharPositionInLine(),
        ctx.stop.getLine(),
        ctx.stop.getCharPositionInLine());
  }

  public void addImport(JimpleParser.ImportItemContext item, @Nonnull String fileuri) {
    if (item == null || item.location == null) {
      return;
    }
    final ClassType classType = identifierFactory.getClassType(item.location.getText());
    final PackageName duplicate =
        imports.putIfAbsent(classType.getClassName(), classType.getPackageName());
    if (duplicate != null) {
      throw new ResolveException(
          "Multiple Imports for the same ClassName can not be resolved!",
          fileuri,
          buildPositionFromCtx(item));
    }
  }

  @Nonnull
  public MethodSignature getMethodSignature(
      JimpleParser.Method_signatureContext ctx, ParserRuleContext parentCtx) {
    if (ctx == null) {
      throw new ResolveException(
          "MethodSignature is missing.", fileUri, buildPositionFromCtx(parentCtx));
    }

    JimpleParser.IdentifierContext class_name = ctx.class_name;
    JimpleParser.TypeContext typeCtx = ctx.method_subsignature().type();
    JimpleParser.Method_nameContext method_nameCtx = ctx.method_subsignature().method_name();
    if (class_name == null || typeCtx == null || method_nameCtx == null) {
      throw new ResolveException(
          "MethodSignature is not well formed.", fileUri, buildPositionFromCtx(ctx));
    }
    String classname = class_name.getText();
    Type type = getType(typeCtx.getText());
    String methodname = method_nameCtx.getText();
    List<Type> params = getTypeList(ctx.method_subsignature().type_list());
    return identifierFactory.getMethodSignature(methodname, getClassType(classname), type, params);
  }

  public FieldSignature getFieldSignature(JimpleParser.Field_signatureContext ctx) {
    String classname = ctx.classname.getText();
    Type type = getType(ctx.type().getText());
    String fieldname = ctx.fieldname.getText();
    return identifierFactory.getFieldSignature(fieldname, getClassType(classname), type);
  }

  public List<Type> getTypeList(JimpleParser.Type_listContext type_list) {
    if (type_list == null) {
      return Collections.emptyList();
    }
    List<JimpleParser.TypeContext> typeList = type_list.type();
    int size = typeList.size();
    if (size < 1) {
      return Collections.emptyList();
    }
    List<Type> list = new ArrayList<>(size);
    for (JimpleParser.TypeContext typeContext : typeList) {
      list.add(identifierFactory.getType(typeContext.getText()));
    }
    return list;
  }

  public List<ClassType> getClassTypeList(JimpleParser.Type_listContext type_list) {
    if (type_list == null) {
      return Collections.emptyList();
    }
    List<JimpleParser.TypeContext> typeList = type_list.type();
    int size = typeList.size();
    if (size < 1) {
      return Collections.emptyList();
    }
    List<ClassType> list = new ArrayList<>(size);
    for (JimpleParser.TypeContext typeContext : typeList) {
      list.add(identifierFactory.getClassType(typeContext.getText()));
    }
    return list;
  }
}
