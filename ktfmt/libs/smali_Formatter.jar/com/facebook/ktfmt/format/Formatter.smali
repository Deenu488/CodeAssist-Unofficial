.class public final Lcom/facebook/ktfmt/format/Formatter;
.super Ljava/lang/Object;
.source "Formatter.kt"


# annotations
.annotation runtime Lkotlin/Metadata;
    d1 = {
        "\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0008\u0002\n\u0002\u0018\u0002\n\u0002\u0008\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0002\n\u0002\u0018\u0002\n\u0002\u0008\u0002\n\u0002\u0010\u000b\n\u0002\u0008\u0004\u0008\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\u0008\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u000cH\u0002J\u0018\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u0011H\u0002J\u0018\u0010\u0012\u001a\u00020\u000c2\u0006\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u000cH\u0007J\u0010\u0010\u0012\u001a\u00020\u000c2\u0006\u0010\u000b\u001a\u00020\u000cH\u0007J\u0018\u0010\u0012\u001a\u00020\u000c2\u0006\u0010\u000b\u001a\u00020\u000c2\u0006\u0010\u0013\u001a\u00020\u0014H\u0007J \u0010\u0015\u001a\u00020\u000c2\u0006\u0010\u000b\u001a\u00020\u000c2\u0006\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u0016\u001a\u00020\u000cH\u0002J\u0010\u0010\u0017\u001a\u00020\u000c2\u0006\u0010\u000b\u001a\u00020\u000cH\u0002R\u0010\u0010\u0003\u001a\u00020\u00048\u0006X\u0087\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u00020\u00048\u0006X\u0087\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u00020\u00048\u0006X\u0087\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0008X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"
    }
    d2 = {
        "Lcom/facebook/ktfmt/format/Formatter;",
        "",
        "()V",
        "DROPBOX_FORMAT",
        "Lcom/facebook/ktfmt/format/FormattingOptions;",
        "GOOGLE_FORMAT",
        "KOTLINLANG_FORMAT",
        "MINIMUM_KOTLIN_VERSION",
        "Lkotlin/KotlinVersion;",
        "checkEscapeSequences",
        "",
        "code",
        "",
        "createAstVisitor",
        "Lorg/jetbrains/kotlin/com/intellij/psi/PsiElementVisitor;",
        "options",
        "builder",
        "Lcom/google/googlejavaformat/OpsBuilder;",
        "format",
        "removeUnusedImports",
        "",
        "prettyPrint",
        "lineSeparator",
        "sortedAndDistinctImports",
        "ktfmt"
    }
    k = 0x1
    mv = {
        0x1,
        0x6,
        0x0
    }
    xi = 0x30
.end annotation


# static fields
.field public static final DROPBOX_FORMAT:Lcom/facebook/ktfmt/format/FormattingOptions;
    .annotation build Lkotlin/jvm/JvmField;
    .end annotation

    .annotation build Lorg/jetbrains/annotations/NotNull;
    .end annotation
.end field

.field public static final GOOGLE_FORMAT:Lcom/facebook/ktfmt/format/FormattingOptions;
    .annotation build Lkotlin/jvm/JvmField;
    .end annotation

    .annotation build Lorg/jetbrains/annotations/NotNull;
    .end annotation
.end field

.field public static final INSTANCE:Lcom/facebook/ktfmt/format/Formatter;
    .annotation build Lorg/jetbrains/annotations/NotNull;
    .end annotation
.end field

.field public static final KOTLINLANG_FORMAT:Lcom/facebook/ktfmt/format/FormattingOptions;
    .annotation build Lkotlin/jvm/JvmField;
    .end annotation

    .annotation build Lorg/jetbrains/annotations/NotNull;
    .end annotation
.end field

.field private static final MINIMUM_KOTLIN_VERSION:Lkotlin/KotlinVersion;
    .annotation build Lorg/jetbrains/annotations/NotNull;
    .end annotation
.end field


# direct methods
.method static constructor <clinit>()V
    .registers 10

    .prologue
    const/4 v3, 0x2

    const/4 v8, 0x0

    const/16 v7, 0x32

    const/4 v9, 0x4

    const/4 v2, 0x0

    new-instance v0, Lcom/facebook/ktfmt/format/Formatter;

    invoke-direct {v0}, Lcom/facebook/ktfmt/format/Formatter;-><init>()V

    sput-object v0, Lcom/facebook/ktfmt/format/Formatter;->INSTANCE:Lcom/facebook/ktfmt/format/Formatter;

    .line 47
    new-instance v0, Lcom/facebook/ktfmt/format/FormattingOptions;

    sget-object v1, Lcom/facebook/ktfmt/format/FormattingOptions$Style;->GOOGLE:Lcom/facebook/ktfmt/format/FormattingOptions$Style;

    move v4, v3

    move v5, v2

    move v6, v2

    invoke-direct/range {v0 .. v8}, Lcom/facebook/ktfmt/format/FormattingOptions;-><init>(Lcom/facebook/ktfmt/format/FormattingOptions$Style;IIIZZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    sput-object v0, Lcom/facebook/ktfmt/format/Formatter;->GOOGLE_FORMAT:Lcom/facebook/ktfmt/format/FormattingOptions;

    .line 51
    new-instance v0, Lcom/facebook/ktfmt/format/FormattingOptions;

    sget-object v1, Lcom/facebook/ktfmt/format/FormattingOptions$Style;->GOOGLE:Lcom/facebook/ktfmt/format/FormattingOptions$Style;

    move v3, v9

    move v4, v9

    move v5, v2

    move v6, v2

    invoke-direct/range {v0 .. v8}, Lcom/facebook/ktfmt/format/FormattingOptions;-><init>(Lcom/facebook/ktfmt/format/FormattingOptions$Style;IIIZZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    sput-object v0, Lcom/facebook/ktfmt/format/Formatter;->KOTLINLANG_FORMAT:Lcom/facebook/ktfmt/format/FormattingOptions;

    .line 54
    new-instance v0, Lcom/facebook/ktfmt/format/FormattingOptions;

    sget-object v1, Lcom/facebook/ktfmt/format/FormattingOptions$Style;->DROPBOX:Lcom/facebook/ktfmt/format/FormattingOptions$Style;

    move v3, v9

    move v4, v9

    move v5, v2

    move v6, v2

    invoke-direct/range {v0 .. v8}, Lcom/facebook/ktfmt/format/FormattingOptions;-><init>(Lcom/facebook/ktfmt/format/FormattingOptions$Style;IIIZZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    sput-object v0, Lcom/facebook/ktfmt/format/Formatter;->DROPBOX_FORMAT:Lcom/facebook/ktfmt/format/FormattingOptions;

    .line 56
    new-instance v0, Lkotlin/KotlinVersion;

    const/4 v1, 0x1

    invoke-direct {v0, v1, v9}, Lkotlin/KotlinVersion;-><init>(II)V

    sput-object v0, Lcom/facebook/ktfmt/format/Formatter;->MINIMUM_KOTLIN_VERSION:Lkotlin/KotlinVersion;

    return-void
.end method

.method private constructor <init>()V
    .registers 1

    .prologue
    .line 44
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static final synthetic access$sortedAndDistinctImports$canonicalText(Lorg/jetbrains/kotlin/psi/KtImportDirective;)Ljava/lang/String;
    .registers 2
    .param p0, "importDirective"    # Lorg/jetbrains/kotlin/psi/KtImportDirective;

    .prologue
    .line 44
    invoke-static {p0}, Lcom/facebook/ktfmt/format/Formatter;->sortedAndDistinctImports$canonicalText(Lorg/jetbrains/kotlin/psi/KtImportDirective;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method private final checkEscapeSequences(Ljava/lang/String;)V
    .registers 7
    .param p1, "code"    # Ljava/lang/String;

    .prologue
    const/4 v2, -0x1

    .line 130
    sget-object v1, Lcom/facebook/ktfmt/format/WhitespaceTombstones;->INSTANCE:Lcom/facebook/ktfmt/format/WhitespaceTombstones;

    invoke-virtual {v1, p1}, Lcom/facebook/ktfmt/format/WhitespaceTombstones;->indexOfWhitespaceTombstone(Ljava/lang/String;)I

    move-result v0

    .line 131
    .local v0, "index":I
    if-ne v0, v2, :cond_0

    .line 132
    sget-object v1, Lcom/facebook/ktfmt/kdoc/Escaping;->INSTANCE:Lcom/facebook/ktfmt/kdoc/Escaping;

    invoke-virtual {v1, p1}, Lcom/facebook/ktfmt/kdoc/Escaping;->indexOfCommentEscapeSequences(Ljava/lang/String;)I

    move-result v0

    .line 134
    :cond_0
    if-eq v0, v2, :cond_1

    .line 135
    new-instance v1, Lcom/facebook/ktfmt/format/ParseError;

    .line 136
    const-string v2, "ktfmt does not support code which contains one of {\\u0003, \\u0004, \\u0005} character; escape it"

    .line 138
    check-cast p1, Ljava/lang/CharSequence;

    .end local p1    # "code":Ljava/lang/String;
    invoke-static {p1, v0}, Lorg/jetbrains/kotlin/com/intellij/openapi/util/text/StringUtil;->offsetToLineColumn(Ljava/lang/CharSequence;I)Lorg/jetbrains/kotlin/com/intellij/openapi/util/text/LineColumn;

    move-result-object v3

    const-string v4, "offsetToLineColumn(code, index)"

    invoke-static {v3, v4}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    .line 135
    invoke-direct {v1, v2, v3}, Lcom/facebook/ktfmt/format/ParseError;-><init>(Ljava/lang/String;Lorg/jetbrains/kotlin/com/intellij/openapi/util/text/LineColumn;)V

    throw v1

    .line 140
    .restart local p1    # "code":Ljava/lang/String;
    :cond_1
    return-void
.end method

.method private final createAstVisitor(Lcom/facebook/ktfmt/format/FormattingOptions;Lcom/google/googlejavaformat/OpsBuilder;)Lorg/jetbrains/kotlin/com/intellij/psi/PsiElementVisitor;
    .registers 6
    .param p1, "options"    # Lcom/facebook/ktfmt/format/FormattingOptions;
    .param p2, "builder"    # Lcom/google/googlejavaformat/OpsBuilder;

    .prologue
    .line 123
    sget-object v0, Lkotlin/KotlinVersion;->CURRENT:Lkotlin/KotlinVersion;

    sget-object v1, Lcom/facebook/ktfmt/format/Formatter;->MINIMUM_KOTLIN_VERSION:Lkotlin/KotlinVersion;

    invoke-virtual {v0, v1}, Lkotlin/KotlinVersion;->compareTo(Lkotlin/KotlinVersion;)I

    move-result v0

    if-gez v0, :cond_0

    .line 124
    new-instance v0, Ljava/lang/RuntimeException;

    const-string v1, "Unsupported runtime Kotlin version: "

    sget-object v2, Lkotlin/KotlinVersion;->CURRENT:Lkotlin/KotlinVersion;

    invoke-static {v1, v2}, Lkotlin/jvm/internal/Intrinsics;->stringPlus(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-direct {v0, v1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 126
    :cond_0
    new-instance v0, Lcom/facebook/ktfmt/format/KotlinInputAstVisitor;

    invoke-direct {v0, p1, p2}, Lcom/facebook/ktfmt/format/KotlinInputAstVisitor;-><init>(Lcom/facebook/ktfmt/format/FormattingOptions;Lcom/google/googlejavaformat/OpsBuilder;)V

    check-cast v0, Lorg/jetbrains/kotlin/com/intellij/psi/PsiElementVisitor;

    return-object v0
.end method

.method public static final format(Lcom/facebook/ktfmt/format/FormattingOptions;Ljava/lang/String;)Ljava/lang/String;
    .registers 13
    .param p0, "options"    # Lcom/facebook/ktfmt/format/FormattingOptions;
        .annotation build Lorg/jetbrains/annotations/NotNull;
        .end annotation
    .end param
    .param p1, "code"    # Ljava/lang/String;
        .annotation build Lorg/jetbrains/annotations/NotNull;
        .end annotation
    .end param
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Lcom/google/googlejavaformat/java/FormatterException;,
            Lcom/facebook/ktfmt/format/ParseError;
        }
    .end annotation

    .annotation runtime Lkotlin/jvm/JvmStatic;
    .end annotation

    .annotation build Lorg/jetbrains/annotations/NotNull;
    .end annotation

    .prologue
    const/4 v10, 0x2

    const/4 v7, 0x1

    const/4 v8, 0x0

    const-string v6, "options"

    invoke-static {p0, v6}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v6, "code"

    invoke-static {p1, v6}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 82
    const-string v6, "#!"

    const/4 v9, 0x0

    invoke-static {p1, v6, v8, v10, v9}, Lkotlin/text/StringsKt;->startsWith$default(Ljava/lang/String;Ljava/lang/String;ZILjava/lang/Object;)Z

    move-result v6

    if-eqz v6, :cond_1

    .line 83
    check-cast p1, Ljava/lang/CharSequence;

    .end local p1    # "code":Ljava/lang/String;
    new-instance v6, Lkotlin/text/Regex;

    const-string v9, "\n"

    invoke-direct {v6, v9}, Lkotlin/text/Regex;-><init>(Ljava/lang/String;)V

    invoke-virtual {v6, p1, v10}, Lkotlin/text/Regex;->split(Ljava/lang/CharSequence;I)Ljava/util/List;

    move-result-object v6

    .line 81
    :goto_0
    invoke-interface {v6, v8}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    .local v4, "shebang":Ljava/lang/String;
    nop

    .line 82
    invoke-interface {v6, v7}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v0

    .line 81
    check-cast v0, Ljava/lang/String;

    .line 87
    .local v0, "kotlinCode":Ljava/lang/String;
    sget-object v6, Lcom/facebook/ktfmt/format/Formatter;->INSTANCE:Lcom/facebook/ktfmt/format/Formatter;

    invoke-direct {v6, v0}, Lcom/facebook/ktfmt/format/Formatter;->checkEscapeSequences(Ljava/lang/String;)V

    .line 89
    invoke-static {v0}, Lorg/jetbrains/kotlin/com/intellij/openapi/util/text/StringUtilRt;->convertLineSeparators(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const-string v6, "convertLineSeparators(kotlinCode)"

    invoke-static {v1, v6}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    .line 90
    .local v1, "lfCode":Ljava/lang/String;
    sget-object v6, Lcom/facebook/ktfmt/format/Formatter;->INSTANCE:Lcom/facebook/ktfmt/format/Formatter;

    invoke-direct {v6, v1}, Lcom/facebook/ktfmt/format/Formatter;->sortedAndDistinctImports(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 91
    .local v5, "sortedImports":Ljava/lang/String;
    sget-object v6, Lcom/facebook/ktfmt/format/RedundantElementRemover;->INSTANCE:Lcom/facebook/ktfmt/format/RedundantElementRemover;

    invoke-virtual {v6, v5, p0}, Lcom/facebook/ktfmt/format/RedundantElementRemover;->dropRedundantElements(Ljava/lang/String;Lcom/facebook/ktfmt/format/FormattingOptions;)Ljava/lang/String;

    move-result-object v2

    .line 93
    .local v2, "noRedundantElements":Ljava/lang/String;
    sget-object v6, Lcom/facebook/ktfmt/format/Formatter;->INSTANCE:Lcom/facebook/ktfmt/format/Formatter;

    invoke-static {v0}, Lcom/google/googlejavaformat/Newlines;->guessLineSeparator(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v9

    invoke-static {v9}, Lkotlin/jvm/internal/Intrinsics;->checkNotNull(Ljava/lang/Object;)V

    invoke-direct {v6, v2, p0, v9}, Lcom/facebook/ktfmt/format/Formatter;->prettyPrint(Ljava/lang/String;Lcom/facebook/ktfmt/format/FormattingOptions;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    .local v3, "prettyCode":Ljava/lang/String;
    move-object v6, v4

    .line 94
    check-cast v6, Ljava/lang/CharSequence;

    invoke-interface {v6}, Ljava/lang/CharSequence;->length()I

    move-result v6

    if-lez v6, :cond_2

    move v6, v7

    :goto_1
    if-eqz v6, :cond_0

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v6, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    const/16 v7, 0xa

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    .end local v3    # "prettyCode":Ljava/lang/String;
    :cond_0
    return-object v3

    .line 85
    .end local v0    # "kotlinCode":Ljava/lang/String;
    .end local v1    # "lfCode":Ljava/lang/String;
    .end local v2    # "noRedundantElements":Ljava/lang/String;
    .end local v4    # "shebang":Ljava/lang/String;
    .end local v5    # "sortedImports":Ljava/lang/String;
    .restart local p1    # "code":Ljava/lang/String;
    :cond_1
    new-array v6, v10, [Ljava/lang/String;

    const-string v9, ""

    aput-object v9, v6, v8

    aput-object p1, v6, v7

    invoke-static {v6}, Lkotlin/collections/CollectionsKt;->listOf([Ljava/lang/Object;)Ljava/util/List;

    move-result-object v6

    goto :goto_0

    .end local p1    # "code":Ljava/lang/String;
    .restart local v0    # "kotlinCode":Ljava/lang/String;
    .restart local v1    # "lfCode":Ljava/lang/String;
    .restart local v2    # "noRedundantElements":Ljava/lang/String;
    .restart local v3    # "prettyCode":Ljava/lang/String;
    .restart local v4    # "shebang":Ljava/lang/String;
    .restart local v5    # "sortedImports":Ljava/lang/String;
    :cond_2
    move v6, v8

    .line 94
    goto :goto_1
.end method

.method public static final format(Ljava/lang/String;)Ljava/lang/String;
    .registers 10
    .param p0, "code"    # Ljava/lang/String;
        .annotation build Lorg/jetbrains/annotations/NotNull;
        .end annotation
    .end param
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Lcom/google/googlejavaformat/java/FormatterException;,
            Lcom/facebook/ktfmt/format/ParseError;
        }
    .end annotation

    .annotation runtime Lkotlin/jvm/JvmStatic;
    .end annotation

    .annotation build Lorg/jetbrains/annotations/NotNull;
    .end annotation

    .prologue
    const/4 v1, 0x0

    const/4 v2, 0x0

    const-string v0, "code"

    invoke-static {p0, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 64
    sget-object v0, Lcom/facebook/ktfmt/format/Formatter;->INSTANCE:Lcom/facebook/ktfmt/format/Formatter;

    new-instance v0, Lcom/facebook/ktfmt/format/FormattingOptions;

    const/16 v7, 0x3f

    move v3, v2

    move v4, v2

    move v5, v2

    move v6, v2

    move-object v8, v1

    invoke-direct/range {v0 .. v8}, Lcom/facebook/ktfmt/format/FormattingOptions;-><init>(Lcom/facebook/ktfmt/format/FormattingOptions$Style;IIIZZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    invoke-static {v0, p0}, Lcom/facebook/ktfmt/format/Formatter;->format(Lcom/facebook/ktfmt/format/FormattingOptions;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static final format(Ljava/lang/String;Z)Ljava/lang/String;
    .registers 11
    .param p0, "code"    # Ljava/lang/String;
        .annotation build Lorg/jetbrains/annotations/NotNull;
        .end annotation
    .end param
    .param p1, "removeUnusedImports"    # Z
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Lcom/google/googlejavaformat/java/FormatterException;,
            Lcom/facebook/ktfmt/format/ParseError;
        }
    .end annotation

    .annotation runtime Lkotlin/jvm/JvmStatic;
    .end annotation

    .annotation build Lorg/jetbrains/annotations/NotNull;
    .end annotation

    .prologue
    const/4 v1, 0x0

    const/4 v2, 0x0

    const-string v0, "code"

    invoke-static {p0, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V

    .line 73
    sget-object v0, Lcom/facebook/ktfmt/format/Formatter;->INSTANCE:Lcom/facebook/ktfmt/format/Formatter;

    new-instance v0, Lcom/facebook/ktfmt/format/FormattingOptions;

    const/16 v7, 0x2f

    move v3, v2

    move v4, v2

    move v5, p1

    move v6, v2

    move-object v8, v1

    invoke-direct/range {v0 .. v8}, Lcom/facebook/ktfmt/format/FormattingOptions;-><init>(Lcom/facebook/ktfmt/format/FormattingOptions$Style;IIIZZILkotlin/jvm/internal/DefaultConstructorMarker;)V

    invoke-static {v0, p0}, Lcom/facebook/ktfmt/format/Formatter;->format(Lcom/facebook/ktfmt/format/FormattingOptions;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method private final prettyPrint(Ljava/lang/String;Lcom/facebook/ktfmt/format/FormattingOptions;Ljava/lang/String;)Ljava/lang/String;
    .registers 15
    .param p1, "code"    # Ljava/lang/String;
    .param p2, "options"    # Lcom/facebook/ktfmt/format/FormattingOptions;
    .param p3, "lineSeparator"    # Ljava/lang/String;

    .prologue
    const/4 v10, 0x0

    .line 99
    sget-object v7, Lcom/facebook/ktfmt/format/Parser;->INSTANCE:Lcom/facebook/ktfmt/format/Parser;

    invoke-virtual {v7, p1}, Lcom/facebook/ktfmt/format/Parser;->parse(Ljava/lang/String;)Lorg/jetbrains/kotlin/psi/KtFile;

    move-result-object v2

    .line 100
    .local v2, "file":Lorg/jetbrains/kotlin/psi/KtFile;
    new-instance v4, Lcom/facebook/ktfmt/format/KotlinInput;

    invoke-direct {v4, p1, v2}, Lcom/facebook/ktfmt/format/KotlinInput;-><init>(Ljava/lang/String;Lorg/jetbrains/kotlin/psi/KtFile;)V

    .line 102
    .local v4, "kotlinInput":Lcom/facebook/ktfmt/format/KotlinInput;
    new-instance v3, Lcom/google/googlejavaformat/java/JavaOutput;

    move-object v7, v4

    check-cast v7, Lcom/google/googlejavaformat/Input;

    new-instance v8, Lcom/facebook/ktfmt/kdoc/KDocCommentsHelper;

    invoke-virtual {p2}, Lcom/facebook/ktfmt/format/FormattingOptions;->getMaxWidth()I

    move-result v9

    invoke-direct {v8, p3, v9}, Lcom/facebook/ktfmt/kdoc/KDocCommentsHelper;-><init>(Ljava/lang/String;I)V

    check-cast v8, Lcom/google/googlejavaformat/CommentsHelper;

    invoke-direct {v3, p3, v7, v8}, Lcom/google/googlejavaformat/java/JavaOutput;-><init>(Ljava/lang/String;Lcom/google/googlejavaformat/Input;Lcom/google/googlejavaformat/CommentsHelper;)V

    .line 103
    .local v3, "javaOutput":Lcom/google/googlejavaformat/java/JavaOutput;
    new-instance v0, Lcom/google/googlejavaformat/OpsBuilder;

    move-object v7, v4

    check-cast v7, Lcom/google/googlejavaformat/Input;

    move-object v8, v3

    check-cast v8, Lcom/google/googlejavaformat/Output;

    invoke-direct {v0, v7, v8}, Lcom/google/googlejavaformat/OpsBuilder;-><init>(Lcom/google/googlejavaformat/Input;Lcom/google/googlejavaformat/Output;)V

    .line 104
    .local v0, "builder":Lcom/google/googlejavaformat/OpsBuilder;
    invoke-direct {p0, p2, v0}, Lcom/facebook/ktfmt/format/Formatter;->createAstVisitor(Lcom/facebook/ktfmt/format/FormattingOptions;Lcom/google/googlejavaformat/OpsBuilder;)Lorg/jetbrains/kotlin/com/intellij/psi/PsiElementVisitor;

    move-result-object v7

    invoke-virtual {v2, v7}, Lorg/jetbrains/kotlin/psi/KtFile;->accept(Lorg/jetbrains/kotlin/com/intellij/psi/PsiElementVisitor;)V

    .line 105
    invoke-virtual {v4}, Lcom/facebook/ktfmt/format/KotlinInput;->getText()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/String;->length()I

    move-result v7

    invoke-virtual {v0, v7}, Lcom/google/googlejavaformat/OpsBuilder;->sync(I)V

    .line 106
    invoke-virtual {v0}, Lcom/google/googlejavaformat/OpsBuilder;->drain()V

    .line 107
    invoke-virtual {v0}, Lcom/google/googlejavaformat/OpsBuilder;->build()Lcom/google/common/collect/ImmutableList;

    move-result-object v5

    .line 108
    .local v5, "ops":Lcom/google/common/collect/ImmutableList;
    invoke-virtual {p2}, Lcom/facebook/ktfmt/format/FormattingOptions;->getDebuggingPrintOpsAfterFormatting()Z

    move-result v7

    if-eqz v7, :cond_0

    .line 109
    const-string v7, "ops"

    invoke-static {v5, v7}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-static {v5}, Lcom/facebook/ktfmt/debughelpers/OpsDebugKt;->printOps(Lcom/google/common/collect/ImmutableList;)V

    .line 111
    :cond_0
    new-instance v7, Lcom/google/googlejavaformat/DocBuilder;

    invoke-direct {v7}, Lcom/google/googlejavaformat/DocBuilder;-><init>()V

    check-cast v5, Ljava/util/List;

    .end local v5    # "ops":Lcom/google/common/collect/ImmutableList;
    invoke-virtual {v7, v5}, Lcom/google/googlejavaformat/DocBuilder;->withOps(Ljava/util/List;)Lcom/google/googlejavaformat/DocBuilder;

    move-result-object v7

    invoke-virtual {v7}, Lcom/google/googlejavaformat/DocBuilder;->build()Lcom/google/googlejavaformat/Doc;

    move-result-object v1

    .line 112
    .local v1, "doc":Lcom/google/googlejavaformat/Doc;
    invoke-virtual {v3}, Lcom/google/googlejavaformat/java/JavaOutput;->getCommentsHelper()Lcom/google/googlejavaformat/CommentsHelper;

    move-result-object v7

    invoke-virtual {p2}, Lcom/facebook/ktfmt/format/FormattingOptions;->getMaxWidth()I

    move-result v8

    new-instance v9, Lcom/google/googlejavaformat/Doc$State;

    invoke-direct {v9, v10, v10}, Lcom/google/googlejavaformat/Doc$State;-><init>(II)V

    invoke-virtual {v1, v7, v8, v9}, Lcom/google/googlejavaformat/Doc;->computeBreaks(Lcom/google/googlejavaformat/CommentsHelper;ILcom/google/googlejavaformat/Doc$State;)Lcom/google/googlejavaformat/Doc$State;

    move-object v7, v3

    .line 113
    check-cast v7, Lcom/google/googlejavaformat/Output;

    invoke-virtual {v1, v7}, Lcom/google/googlejavaformat/Doc;->write(Lcom/google/googlejavaformat/Output;)V

    .line 114
    invoke-virtual {v3}, Lcom/google/googlejavaformat/java/JavaOutput;->flush()V

    .line 117
    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v7

    check-cast v7, Ljava/lang/Comparable;

    invoke-virtual {p1}, Ljava/lang/String;->length()I

    move-result v8

    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v8

    check-cast v8, Ljava/lang/Comparable;

    invoke-static {v7, v8}, Lcom/google/common/collect/Range;->closedOpen(Ljava/lang/Comparable;Ljava/lang/Comparable;)Lcom/google/common/collect/Range;

    move-result-object v7

    invoke-static {v7}, Lcom/google/common/collect/ImmutableList;->of(Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;

    move-result-object v7

    const-string v8, "of(Range.closedOpen(0, code.length))"

    invoke-static {v7, v8}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    check-cast v7, Ljava/util/Collection;

    invoke-virtual {v4, v7}, Lcom/facebook/ktfmt/format/KotlinInput;->characterRangesToTokenRanges(Ljava/util/Collection;)Lcom/google/common/collect/RangeSet;

    move-result-object v6

    .line 118
    .local v6, "tokenRangeSet":Lcom/google/common/collect/RangeSet;
    sget-object v8, Lcom/facebook/ktfmt/format/WhitespaceTombstones;->INSTANCE:Lcom/facebook/ktfmt/format/WhitespaceTombstones;

    .line 119
    invoke-virtual {v3, v6}, Lcom/google/googlejavaformat/java/JavaOutput;->getFormatReplacements(Lcom/google/common/collect/RangeSet;)Lcom/google/common/collect/ImmutableList;

    move-result-object v7

    check-cast v7, Ljava/util/List;

    invoke-static {p1, v7}, Lcom/google/googlejavaformat/java/JavaOutput;->applyReplacements(Ljava/lang/String;Ljava/util/List;)Ljava/lang/String;

    move-result-object v7

    const-string v9, "applyReplacements(code, \u2026lacements(tokenRangeSet))"

    invoke-static {v7, v9}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    .line 118
    invoke-virtual {v8, v7}, Lcom/facebook/ktfmt/format/WhitespaceTombstones;->replaceTombstoneWithTrailingWhitespace(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v7

    return-object v7
.end method

.method private final sortedAndDistinctImports(Ljava/lang/String;)Ljava/lang/String;
    .registers 31
    .param p1, "code"    # Ljava/lang/String;

    .prologue
    .line 143
    sget-object v3, Lcom/facebook/ktfmt/format/Parser;->INSTANCE:Lcom/facebook/ktfmt/format/Parser;

    move-object/from16 v0, p1

    invoke-virtual {v3, v0}, Lcom/facebook/ktfmt/format/Parser;->parse(Ljava/lang/String;)Lorg/jetbrains/kotlin/psi/KtFile;

    move-result-object v19

    .line 145
    .local v19, "file":Lorg/jetbrains/kotlin/psi/KtFile;
    invoke-virtual/range {v19 .. v19}, Lorg/jetbrains/kotlin/psi/KtFile;->getImportList()Lorg/jetbrains/kotlin/psi/KtImportList;

    move-result-object v20

    if-nez v20, :cond_1

    .line 174
    .end local p1    # "code":Ljava/lang/String;
    :cond_0
    :goto_0
    return-object p1

    .line 146
    .local v20, "importList":Lorg/jetbrains/kotlin/psi/KtImportList;
    .restart local p1    # "code":Ljava/lang/String;
    :cond_1
    invoke-virtual/range {v20 .. v20}, Lorg/jetbrains/kotlin/psi/KtImportList;->getImports()Ljava/util/List;

    move-result-object v3

    invoke-interface {v3}, Ljava/util/List;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_0

    .line 150
    new-instance v16, Ljava/util/ArrayList;

    invoke-direct/range {v16 .. v16}, Ljava/util/ArrayList;-><init>()V

    check-cast v16, Ljava/util/List;

    .line 153
    .local v16, "commentList":Ljava/util/List;
    invoke-virtual/range {v20 .. v20}, Lorg/jetbrains/kotlin/psi/KtImportList;->getFirstChild()Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;

    move-result-object v18

    .line 154
    .local v18, "element":Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;
    :goto_1
    if-eqz v18, :cond_4

    .line 155
    move-object/from16 v0, v18

    instance-of v3, v0, Lorg/jetbrains/kotlin/com/intellij/psi/PsiComment;

    if-eqz v3, :cond_3

    .line 156
    move-object/from16 v0, v16

    move-object/from16 v1, v18

    invoke-interface {v0, v1}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 162
    :cond_2
    invoke-interface/range {v18 .. v18}, Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;->getNextSibling()Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;

    move-result-object v18

    goto :goto_1

    .line 157
    :cond_3
    move-object/from16 v0, v18

    instance-of v3, v0, Lorg/jetbrains/kotlin/psi/KtImportDirective;

    if-nez v3, :cond_2

    move-object/from16 v0, v18

    instance-of v3, v0, Lorg/jetbrains/kotlin/com/intellij/psi/PsiWhiteSpace;

    if-nez v3, :cond_2

    .line 158
    new-instance v3, Lcom/facebook/ktfmt/format/ParseError;

    .line 159
    const-string v4, "Imports not contiguous: "

    invoke-interface/range {v18 .. v18}, Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;->getText()Ljava/lang/String;

    move-result-object v5

    invoke-static {v4, v5}, Lkotlin/jvm/internal/Intrinsics;->stringPlus(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v4

    .line 160
    check-cast p1, Ljava/lang/CharSequence;

    .end local p1    # "code":Ljava/lang/String;
    invoke-static/range {v18 .. v18}, Lorg/jetbrains/kotlin/psi/psiUtil/PsiUtilsKt;->getStartOffset(Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;)I

    move-result v5

    move-object/from16 v0, p1

    invoke-static {v0, v5}, Lorg/jetbrains/kotlin/com/intellij/openapi/util/text/StringUtil;->offsetToLineColumn(Ljava/lang/CharSequence;I)Lorg/jetbrains/kotlin/com/intellij/openapi/util/text/LineColumn;

    move-result-object v5

    const-string v6, "offsetToLineColumn(code, element.startOffset)"

    invoke-static {v5, v6}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    .line 158
    invoke-direct {v3, v4, v5}, Lcom/facebook/ktfmt/format/ParseError;-><init>(Ljava/lang/String;Lorg/jetbrains/kotlin/com/intellij/openapi/util/text/LineColumn;)V

    throw v3

    .line 171
    .restart local p1    # "code":Ljava/lang/String;
    :cond_4
    invoke-virtual/range {v20 .. v20}, Lorg/jetbrains/kotlin/psi/KtImportList;->getImports()Ljava/util/List;

    move-result-object v15

    const-string v3, "importList.imports"

    invoke-static {v15, v3}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V

    check-cast v15, Ljava/lang/Iterable;

    .local v15, "$this$sortedBy$iv":Ljava/lang/Iterable;
    const/4 v14, 0x0

    .line 181
    .local v14, "$i$f$sortedBy":I
    new-instance v3, Lcom/facebook/ktfmt/format/Formatter$sortedAndDistinctImports$$inlined$sortedBy$1;

    invoke-direct {v3}, Lcom/facebook/ktfmt/format/Formatter$sortedAndDistinctImports$$inlined$sortedBy$1;-><init>()V

    check-cast v3, Ljava/util/Comparator;

    invoke-static {v15, v3}, Lkotlin/collections/CollectionsKt;->sortedWith(Ljava/lang/Iterable;Ljava/util/Comparator;)Ljava/util/List;

    move-result-object v3

    check-cast v3, Ljava/lang/Iterable;

    .line 171
    nop

    const/4 v13, 0x0

    .line 182
    .local v13, "$i$f$distinctBy":I
    new-instance v25, Ljava/util/HashSet;

    invoke-direct/range {v25 .. v25}, Ljava/util/HashSet;-><init>()V

    .line 183
    .local v25, "set$iv":Ljava/util/HashSet;
    new-instance v23, Ljava/util/ArrayList;

    invoke-direct/range {v23 .. v23}, Ljava/util/ArrayList;-><init>()V

    .line 184
    .local v23, "list$iv":Ljava/util/ArrayList;
    invoke-interface {v3}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;

    move-result-object v3

    :cond_5
    :goto_2
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v4

    if-eqz v4, :cond_6

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v17

    .local v17, "e$iv":Ljava/lang/Object;
    move-object/from16 v24, v17

    .line 185
    check-cast v24, Lorg/jetbrains/kotlin/psi/KtImportDirective;

    .local v24, "p0":Lorg/jetbrains/kotlin/psi/KtImportDirective;
    const/4 v12, 0x0

    .line 171
    .local v12, "$i$a$-distinctBy-Formatter$sortedAndDistinctImports$sortedImports$2":I
    invoke-static/range {v24 .. v24}, Lcom/facebook/ktfmt/format/Formatter;->sortedAndDistinctImports$canonicalText(Lorg/jetbrains/kotlin/psi/KtImportDirective;)Ljava/lang/String;

    move-result-object v22

    .line 186
    .local v22, "key$iv":Ljava/lang/String;
    move-object/from16 v0, v25

    move-object/from16 v1, v22

    invoke-virtual {v0, v1}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_5

    .line 187
    move-object/from16 v0, v23

    move-object/from16 v1, v17

    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_2

    .end local v12    # "$i$a$-distinctBy-Formatter$sortedAndDistinctImports$sortedImports$2":I
    .end local v17    # "e$iv":Ljava/lang/Object;
    .end local v22    # "key$iv":Ljava/lang/String;
    .end local v24    # "p0":Lorg/jetbrains/kotlin/psi/KtImportDirective;
    :cond_6
    move-object/from16 v26, v23

    .line 189
    check-cast v26, Ljava/util/List;

    .line 172
    .local v26, "sortedImports":Ljava/util/List;
    check-cast v16, Ljava/util/Collection;

    .end local v16    # "commentList":Ljava/util/List;
    check-cast v26, Ljava/lang/Iterable;

    .end local v26    # "sortedImports":Ljava/util/List;
    move-object/from16 v0, v16

    move-object/from16 v1, v26

    invoke-static {v0, v1}, Lkotlin/collections/CollectionsKt;->plus(Ljava/util/Collection;Ljava/lang/Iterable;)Ljava/util/List;

    move-result-object v21

    .local v21, "importsWithComments":Ljava/util/List;
    move-object/from16 v3, v20

    .line 175
    check-cast v3, Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;

    invoke-static {v3}, Lorg/jetbrains/kotlin/psi/psiUtil/PsiUtilsKt;->getStartOffset(Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;)I

    move-result v27

    .line 176
    check-cast v20, Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;

    .end local v20    # "importList":Lorg/jetbrains/kotlin/psi/KtImportList;
    invoke-static/range {v20 .. v20}, Lorg/jetbrains/kotlin/psi/psiUtil/PsiUtilsKt;->getEndOffset(Lorg/jetbrains/kotlin/com/intellij/psi/PsiElement;)I

    move-result v28

    move-object/from16 v3, v21

    .line 177
    check-cast v3, Ljava/lang/Iterable;

    const-string v4, "\n"

    check-cast v4, Ljava/lang/CharSequence;

    const/4 v5, 0x0

    const/4 v6, 0x0

    const/4 v7, 0x0

    const/4 v8, 0x0

    sget-object v9, Lcom/facebook/ktfmt/format/Formatter$sortedAndDistinctImports$1;->INSTANCE:Lcom/facebook/ktfmt/format/Formatter$sortedAndDistinctImports$1;

    check-cast v9, Lkotlin/jvm/functions/Function1;

    const/16 v10, 0x1e

    const/4 v11, 0x0

    invoke-static/range {v3 .. v11}, Lkotlin/collections/CollectionsKt;->joinToString$default(Ljava/lang/Iterable;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;ILjava/lang/CharSequence;Lkotlin/jvm/functions/Function1;ILjava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "\n"

    invoke-static {v3, v4}, Lkotlin/jvm/internal/Intrinsics;->stringPlus(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    check-cast v3, Ljava/lang/CharSequence;

    .line 174
    check-cast p1, Ljava/lang/CharSequence;

    .end local p1    # "code":Ljava/lang/String;
    move-object/from16 v0, p1

    move/from16 v1, v27

    move/from16 v2, v28

    invoke-static {v0, v1, v2, v3}, Lkotlin/text/StringsKt;->replaceRange(Ljava/lang/CharSequence;IILjava/lang/CharSequence;)Ljava/lang/CharSequence;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p1

    goto/16 :goto_0
.end method

.method private static final sortedAndDistinctImports$canonicalText(Lorg/jetbrains/kotlin/psi/KtImportDirective;)Ljava/lang/String;
    .registers 9
    .param p0, "importDirective"    # Lorg/jetbrains/kotlin/psi/KtImportDirective;

    .prologue
    const/16 v7, 0x20

    const/4 v5, 0x0

    .line 165
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p0}, Lorg/jetbrains/kotlin/psi/KtImportDirective;->getImportedFqName()Lorg/jetbrains/kotlin/name/FqName;

    move-result-object v0

    if-nez v0, :cond_1

    move-object v0, v5

    :goto_0
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v7}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    move-result-object v6

    .line 167
    invoke-virtual {p0}, Lorg/jetbrains/kotlin/psi/KtImportDirective;->getAlias()Lorg/jetbrains/kotlin/psi/KtImportAlias;

    move-result-object v0

    if-nez v0, :cond_2

    .line 165
    :cond_0
    :goto_1
    invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, v7}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    move-result-object v1

    .line 169
    invoke-virtual {p0}, Lorg/jetbrains/kotlin/psi/KtImportDirective;->isAllUnder()Z

    move-result v0

    if-eqz v0, :cond_3

    const-string v0, "*"

    .line 165
    :goto_2
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    .line 169
    return-object v0

    .line 165
    :cond_1
    invoke-virtual {v0}, Lorg/jetbrains/kotlin/name/FqName;->asString()Ljava/lang/String;

    move-result-object v0

    goto :goto_0

    .line 167
    :cond_2
    invoke-virtual {v0}, Lorg/jetbrains/kotlin/psi/KtImportAlias;->getText()Ljava/lang/String;

    move-result-object v0

    if-eqz v0, :cond_0

    const-string v1, "`"

    const-string v2, ""

    const/4 v3, 0x0

    const/4 v4, 0x4

    invoke-static/range {v0 .. v5}, Lkotlin/text/StringsKt;->replace$default(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZILjava/lang/Object;)Ljava/lang/String;

    move-result-object v5

    goto :goto_1

    .line 169
    :cond_3
    const-string v0, ""

    goto :goto_2
.end method
