/*
 * Copyright (C) 2015 The Android Open Source Project
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
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.idea.editors.gfxtrace.service;

import com.android.tools.rpclib.schema.*;
import com.android.tools.rpclib.binary.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.Map;

public final class Report implements BinaryObject {
  private Map<Long, List<Integer>> myAtomReportItemIdMap;

  @Nullable("there's no report item associated to a given atom id")
  public List<Integer> getForAtom(long atomId) {
    return myAtomReportItemIdMap.get(atomId);
  }

  @NotNull
  public List<Integer> getForAtoms(long firstAtomId, long lastAtomId) {
    List<Integer> list = new ArrayList<>();
    for (long i = firstAtomId; i <= lastAtomId; ++i) {
      List<Integer> value = myAtomReportItemIdMap.get(i);
      if (value != null) {
        list.addAll(value);
      }
    }
    return list;
  }

  public void buildMap() {
    myAtomReportItemIdMap = new HashMap<>();
    for (int i = 0; i < myItems.length; ++i) {
      ReportItem item = myItems[i];
      List<Integer> items = myAtomReportItemIdMap.get(item.getAtom());
      if (items == null) {
        items = new ArrayList<>();
        myAtomReportItemIdMap.put(item.getAtom(), items);
      }
      items.add(i);
    }
  }

  //<<<Start:Java.ClassBody:1>>>
  private ReportItem[] myItems;

  // Constructs a default-initialized {@link Report}.
  public Report() {}


  public ReportItem[] getItems() {
    return myItems;
  }

  public Report setItems(ReportItem[] v) {
    myItems = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("service", "Report", "", "");

  static {
    ENTITY.setFields(new Field[]{
      new Field("Items", new Slice("", new Struct(ReportItem.Klass.INSTANCE.entity()))),
    });
    Namespace.register(Klass.INSTANCE);
  }
  public static void register() {}
  //<<<End:Java.ClassBody:1>>>
  public enum Klass implements BinaryClass {
    //<<<Start:Java.KlassBody:2>>>
    INSTANCE;

    @Override @NotNull
    public Entity entity() { return ENTITY; }

    @Override @NotNull
    public BinaryObject create() { return new Report(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      Report o = (Report)obj;
      e.uint32(o.myItems.length);
      for (int i = 0; i < o.myItems.length; i++) {
        e.value(o.myItems[i]);
      }
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      Report o = (Report)obj;
      o.myItems = new ReportItem[d.uint32()];
      for (int i = 0; i <o.myItems.length; i++) {
        o.myItems[i] = new ReportItem();
        d.value(o.myItems[i]);
      }
    }
    //<<<End:Java.KlassBody:2>>>
  }
}
