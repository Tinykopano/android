/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// ATTENTION: This file has been automatically generated from roomSql.bnf. Do not edit it manually.

package com.android.tools.idea.lang.roomSql.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.android.tools.idea.lang.roomSql.psi.RoomPsiTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.android.tools.idea.lang.roomSql.psi.*;

public class RoomUpdateStatementImpl extends ASTWrapperPsiElement implements RoomUpdateStatement {

  public RoomUpdateStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RoomVisitor visitor) {
    visitor.visitUpdateStatement(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RoomVisitor) accept((RoomVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<RoomColumnName> getColumnNameList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RoomColumnName.class);
  }

  @Override
  @NotNull
  public List<RoomExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RoomExpression.class);
  }

  @Override
  @Nullable
  public RoomSingleTableStatementTable getSingleTableStatementTable() {
    return findChildByClass(RoomSingleTableStatementTable.class);
  }

  @Override
  @Nullable
  public RoomWhereClause getWhereClause() {
    return findChildByClass(RoomWhereClause.class);
  }

  @Override
  @Nullable
  public PsiElement getBacktickLiteral() {
    return findChildByType(BACKTICK_LITERAL);
  }

  @Override
  @Nullable
  public PsiElement getBracketLiteral() {
    return findChildByType(BRACKET_LITERAL);
  }

  @Override
  @Nullable
  public PsiElement getDoubleQuoteStringLiteral() {
    return findChildByType(DOUBLE_QUOTE_STRING_LITERAL);
  }

  @Override
  @Nullable
  public PsiElement getIdentifier() {
    return findChildByType(IDENTIFIER);
  }

  @Override
  @Nullable
  public PsiElement getSingleQuoteStringLiteral() {
    return findChildByType(SINGLE_QUOTE_STRING_LITERAL);
  }

}