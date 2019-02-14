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
package com.android.tools.idea.uibuilder.model;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.BUTTON;
import static com.android.SdkConstants.FRAME_LAYOUT;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.TEXT_VIEW;
import static com.android.SdkConstants.TOOLS_URI;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.tools.idea.common.SyncNlModel;
import com.android.tools.idea.common.api.InsertType;
import com.android.tools.idea.common.command.NlWriteCommandActionUtil;
import com.android.tools.idea.common.fixtures.ModelBuilder;
import com.android.tools.idea.common.model.AttributesTransaction;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.model.NlModel;
import com.android.tools.idea.common.surface.DesignSurface;
import com.android.tools.idea.common.util.NlTreeDumper;
import com.android.tools.idea.uibuilder.LayoutTestCase;
import com.android.tools.idea.uibuilder.property.MockNlComponent;
import com.android.tools.idea.uibuilder.surface.NlDesignSurface;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ui.UIUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

public final class NlComponentTest extends LayoutTestCase {
  private NlModel myModel;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myModel = createModelWithEmptyLayout();
  }

  @Override
  protected void tearDown() throws Exception {
    myModel = null;
    super.tearDown();
  }

  @NotNull
  private NlModel createModelWithEmptyLayout() {
    return model("linear.xml",
                 component(LINEAR_LAYOUT)
                   .withBounds(0, 0, 1000, 1000)
                   .id("@id/linear")
                   .matchParentWidth()
                   .matchParentHeight()).build();
  }

  @NotNull
  private NlModel createModel() {
    return model("linear.xml",
                 component(LINEAR_LAYOUT)
                   .withBounds(0, 0, 1000, 1000)
                   .id("@id/linear")
                   .matchParentWidth()
                   .matchParentHeight()
                   .children(
                     component(TEXT_VIEW)
                       .withBounds(0, 0, 200, 200)
                       .id("@id/textView1")
                       .wrapContentHeight()
                       .wrapContentWidth(),
                     component(FRAME_LAYOUT)
                       .withBounds(0, 200, 200, 200)
                       .id("@id/frameLayout1")
                       .wrapContentHeight()
                       .wrapContentWidth()
                   )).build();
  }

  @NotNull
  private NlComponent createComponent(@NotNull String tagName, @NotNull String id) {
    String text = String.format("<%1s id=\"@+id/%2s\" layout_width=\"wrap_content\" layout_height=\"wrap_content\"/>", tagName, id);
    return createComponent(createTagFromXml(text));
  }

  @NotNull
  private NlComponent createComponent(@NotNull XmlTag tag) {
    NlComponent result = new NlComponent(myModel, tag);
    NlComponentHelper.INSTANCE.registerComponent(result);
    return result;
  }

  @NotNull
  private XmlTag createTagFromXml(String xml) {
    return XmlElementFactory.getInstance(getProject()).createTagFromText(xml);
  }

  public void testNeedsDefaultId() {
    assertFalse(NlComponentHelperKt.needsDefaultId(createComponent("SwitchPreference", "preference")));
  }

  public void testBasic() {
    NlComponent linearLayout = myModel.find("linear");
    NlComponent textView = createComponent(TEXT_VIEW, "textView2");
    NlComponent button = createComponent(BUTTON, "button2");

    assertThat(linearLayout.getChildren()).isEmpty();

    linearLayout.addChild(textView);
    linearLayout.addChild(button);

    assertEquals(Arrays.asList(textView, button), linearLayout.getChildren());
    assertEquals("LinearLayout", linearLayout.getBackend().getTag().getName());
    assertEquals("Button", button.getBackend().getTag().getName());

    assertSame(linearLayout, linearLayout.findViewByTag(linearLayout.getBackend().getTag()));
    assertSame(button, linearLayout.findViewByTag(button.getBackend().getTag()));
    assertSame(textView, linearLayout.findViewByTag(textView.getBackend().getTag()));
    assertEquals(Collections.singletonList(textView), linearLayout.findViewsByTag(textView.getBackend().getTag()));

    NlComponentHelperKt.setBounds(linearLayout, 0, 0, 1000, 800);
    NlComponentHelperKt.setBounds(textView, 0, 0, 200, 100);
    NlComponentHelperKt.setBounds(button, 10, 110, 400, 100);

    assertEquals("NlComponent{tag=<LinearLayout>, bounds=[0,0:1000x800}\n" +
                 "    NlComponent{tag=<TextView>, bounds=[0,0:200x100}\n" +
                 "    NlComponent{tag=<Button>, bounds=[10,110:400x100}",
                 NlTreeDumper.dumpTree(Collections.singletonList(linearLayout)));
  }

  public void testEnsureNamespaceWithInvalidXmlTag() {
    myModel = createModel();

    NlComponent textView = myModel.find("textView1");
    deleteXmlTag(textView);
    String prefix = textView.ensureNamespace("app", AUTO_URI);
    assertNull(prefix);
  }

  private void deleteXmlTag(@NotNull NlComponent component) {
    XmlTag tag = component.getBackend().getTagPointer().getElement();
    WriteCommandAction.writeCommandAction(getProject()).run(() -> tag.delete());
    UIUtil.dispatchAllInvocationEvents();
  }

  public void testAttributeTransactions() {
    myModel = createModelWithEmptyLayout();

    XmlTag textViewXmlTag = createTagFromXml(
      "<TextView" +
      " xmlns:android=\"" + ANDROID_URI + "\"" +
      " xmlns:tools=\"" + TOOLS_URI + "\"" +
      " android:text=\"Initial\"" +
      " android:layout_width=\"wrap_content\"" +
      " android:layout_height=\"wrap_content\" />");

    NlComponent linearLayout = myModel.find("linear");
    NlComponent textView = createComponent(textViewXmlTag);

    linearLayout.addChild(textView);

    AttributesTransaction transaction = textView.startAttributeTransaction();
    assertEquals(transaction, textView.startAttributeTransaction());
    assertNotEquals(transaction, linearLayout.startAttributeTransaction());

    assertFalse(transaction.isComplete());
    assertFalse(transaction.isSuccessful());

    assertEquals("wrap_content", transaction.getAndroidAttribute("layout_width"));
    assertEquals("Initial", transaction.getAndroidAttribute("text"));

    transaction.setAndroidAttribute("layout_width", "150dp");
    transaction.setAttribute(ANDROID_URI, "text", "Hello world");

    // Before rollback
    assertEquals("150dp", transaction.getAndroidAttribute("layout_width"));
    assertEquals("Hello world", transaction.getAndroidAttribute("text"));
    assertEquals("wrap_content", textViewXmlTag.getAttribute("android:layout_width").getValue());
    assertEquals("Initial", textViewXmlTag.getAttribute("android:text").getValue());

    assertTrue(transaction.rollback());

    assertTrue(transaction.isComplete());
    assertFalse(transaction.isSuccessful());

    assertEquals("wrap_content", textViewXmlTag.getAttribute("android:layout_width").getValue());
    assertEquals("Initial", textViewXmlTag.getAttribute("android:text").getValue());

    AttributesTransaction oldTransaction = transaction;
    transaction = textView.startAttributeTransaction();
    assertNotEquals(oldTransaction, transaction);
    assertFalse(transaction.commit()); // Empty commit

    transaction = textView.startAttributeTransaction();
    transaction.setAttribute(ANDROID_URI, "layout_width", "150dp");
    // Check the namespace handling
    transaction.setAttribute(TOOLS_URI, "layout_width", "TOOLS-WIDTH");
    transaction.setAndroidAttribute("text", "Hello world");

    // Before commit
    // - The XML tag will have the old values
    // - The NlComponent will have the old values
    // - Only the transaction will have the new values
    assertEquals("wrap_content", textViewXmlTag.getAttribute("android:layout_width").getValue());
    assertEquals("Initial", textViewXmlTag.getAttribute("android:text").getValue());
    assertEquals("wrap_content", textView.getAndroidAttribute("layout_width"));
    assertEquals("Initial", textView.getAndroidAttribute("text"));
    assertEquals("150dp", transaction.getAndroidAttribute("layout_width"));
    assertEquals("Hello world", transaction.getAndroidAttribute("text"));

    assertTrue(transaction.commit());
    assertTrue(transaction.isSuccessful());
    assertTrue(transaction.isComplete());

    // Check XML tag values after the commit
    assertEquals("150dp", textViewXmlTag.getAttribute("android:layout_width").getValue());
    assertEquals("TOOLS-WIDTH", textViewXmlTag.getAttribute("tools:layout_width").getValue());
    assertEquals("Hello world", textViewXmlTag.getAttribute("android:text").getValue());
  }

  public void testAttributeTransactionsConflicts() {
    XmlTag textViewXmlTag = createTagFromXml(
      "<TextView" +
      " xmlns:android=\"" + ANDROID_URI + "\"" +
      " xmlns:tools=\"" + TOOLS_URI + "\"" +
      " android:text=\"Initial\"" +
      " android:layout_width=\"wrap_content\"" +
      " android:layout_height=\"wrap_content\" />");

    NlComponent linearLayout = myModel.find("linear");
    NlComponent textView = createComponent(textViewXmlTag);

    linearLayout.addChild(textView);

    AttributesTransaction transaction = textView.startAttributeTransaction();
    transaction.setAndroidAttribute("layout_width", "150dp");
    transaction.setAndroidAttribute("layout_height", null);

    // Change the XML tag content before the transaction is committed and remove one tag
    textViewXmlTag.setAttribute("android:layout_width", "300dp");
    textViewXmlTag.setAttribute("android:layout_height", "900dp");
    assertTrue(transaction.commit());
    assertTrue(transaction.isComplete());
    assertTrue(transaction.isSuccessful());

    // Check XML tag values after the commit (the commit takes precedence to the XML modifications)
    assertEquals("150dp", textViewXmlTag.getAttribute("android:layout_width").getValue());
    // Check we haven't executed the delete on the attribute that was modified
    assertEquals("900dp", textViewXmlTag.getAttribute("android:layout_height").getValue());
  }

  public void testRemoveObsoleteAttributes() {
    @Language("XML")
    String editText = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                      "<RelativeLayout\n" +
                      "  xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "  xmlns:tools=\"http://schemas.android.com/tools\">" +
                      "  <Button\n" +
                      "         android:id=\"@+id/editText\"\n" +
                      "         android:layout_width=\"wrap_content\"\n" +
                      "         android:layout_height=\"wrap_content\"\n" +
                      "         android:ems=\"10\"\n" +
                      "         android:inputType=\"textEmailAddress\"\n" +
                      "         android:orientation=\"vertical\"\n" +
                      "         tools:layout_editor_absoluteX=\"32dp\"\n" +
                      "         tools:layout_editor_absoluteY=\"43dp\"\n/>" +
                      "</RelativeLayout>";

    XmlFile xmlFile = (XmlFile)myFixture.addFileToProject("res/layout/layout.xml", editText);

    XmlTag[] subTags = xmlFile.getRootTag().getSubTags();
    assertEquals(1, subTags.length);

    NlComponent component = MockNlComponent.create(subTags[0]);
    CommandProcessor.getInstance().runUndoTransparentAction(component::removeObsoleteAttributes);

    @Language("XML")
    String expected = "<Button\n" +
                      "         android:id=\"@+id/editText\"\n" +
                      "         android:layout_width=\"wrap_content\"\n" +
                      "         android:layout_height=\"wrap_content\"\n" +
                      "         android:ems=\"10\"\n" +
                      "         android:inputType=\"textEmailAddress\"\n" +
                      "         />";
    assertEquals(expected, component.getBackend().getTag().getText());
  }

  public void testIdFromMixin() {
    XmlTag tag = mock(XmlTag.class);
    when(tag.isValid()).thenReturn(true);
    when(tag.getName()).thenReturn("");
    //noinspection unchecked
    NlComponent component = new NlComponent(mock(NlModel.class), tag, mock(SmartPsiElementPointer.class));

    NlComponent.XmlModelComponentMixin mixin = mock(NlComponent.XmlModelComponentMixin.class);
    when(mixin.getAttribute(ANDROID_URI, ATTR_ID)).thenReturn("@id/mixinId");
    component.setMixin(mixin);
    assertEquals("mixinId", component.getId());

    when(tag.getAttributeValue(ATTR_ID, ANDROID_URI)).thenReturn("@id/componentId");
    //noinspection unchecked
    component = new NlComponent(mock(NlModel.class), tag, mock(SmartPsiElementPointer.class));
    component.setMixin(mixin);
    assertEquals("componentId", component.getId());
  }

  public void testNamespaceTransfer() {
    @Language("XML")
    String editText = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                      "<layout>\n" +
                      "<RelativeLayout\n" +
                      "  xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "  xmlns:tools123=\"http://schemas.android.com/tools\">" +
                      "  <Button\n" +
                      "         android:id=\"@+id/editText\"\n" +
                      "         android:layout_width=\"wrap_content\"\n" +
                      "         android:layout_height=\"wrap_content\"\n" +
                      "         android:ems=\"10\"\n" +
                      "         android:inputType=\"textEmailAddress\"\n" +
                      "         android:orientation=\"vertical\"\n" +
                      "         tools123:layout_editor_absoluteX=\"32dp\"\n" +
                      "         tools123:layout_editor_absoluteY=\"43dp\"\n/>" +
                      "</RelativeLayout>\n" +
                      "</layout>\n";
    XmlFile xmlFile = (XmlFile)myFixture.addFileToProject("res/layout/layout.xml", editText);
    DesignSurface surface = ModelBuilder.createSurface(NlDesignSurface.class);
    Consumer<NlComponent> componentRegistrar = (@NotNull NlComponent component) -> NlComponentHelper.INSTANCE.registerComponent(component);
    when(surface.getComponentRegistrar()).thenReturn(componentRegistrar);
    myModel = SyncNlModel.create(surface, getTestRootDisposable(), myFacet, xmlFile.getVirtualFile());
    myModel.syncWithPsi(xmlFile.getRootTag(), Collections.emptyList());
    NlComponent relativeLayout = myModel.getComponents().get(0).getChild(0);

    XmlTag textViewXmlTag = createTagFromXml(
      "<TextView" +
      " xmlns:android=\"" + ANDROID_URI + "\"" +
      " xmlns:tools=\"" + TOOLS_URI + "\"" +
      " android:text=\"Initial\"" +
      " tools:text=\"ToolText\"" +
      " android:layout_width=\"wrap_content\"" +
      " android:layout_height=\"wrap_content\" />");
    NlComponent textView = createComponent(textViewXmlTag);
    NlWriteCommandActionUtil.run(relativeLayout, "addTextView", () -> textView.addTags(relativeLayout, null, InsertType.PASTE));
    UIUtil.dispatchAllInvocationEvents();

    @Language("XML")
    String expected = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                      "<layout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    xmlns:tools123=\"http://schemas.android.com/tools\">\n" +
                      "\n" +
                      "    <RelativeLayout>\n" +
                      "\n" +
                      "        <Button\n" +
                      "            android:id=\"@+id/editText\"\n" +
                      "            android:layout_width=\"wrap_content\"\n" +
                      "            android:layout_height=\"wrap_content\"\n" +
                      "            android:ems=\"10\"\n" +
                      "            android:inputType=\"textEmailAddress\"\n" +
                      "            android:orientation=\"vertical\"\n" +
                      "            tools123:layout_editor_absoluteX=\"32dp\"\n" +
                      "            tools123:layout_editor_absoluteY=\"43dp\" />\n" +
                      "\n" +
                      "        <TextView\n" +
                      "            android:layout_width=\"wrap_content\"\n" +
                      "            android:layout_height=\"wrap_content\"\n" +
                      "            android:text=\"Initial\"\n" +
                      "            tools123:text=\"ToolText\" />\n" +
                      "    </RelativeLayout>\n" +
                      "</layout>\n";
    assertEquals(expected, xmlFile.getText());
  }

  public void testAddTagsWithInvalidXmlTag() {
    myModel = createModel();
    NlComponent frameLayout = myModel.find("frameLayout1");
    deleteXmlTag(frameLayout);

    NlComponent newTextView = createComponent(TEXT_VIEW, "textView2");
    NlWriteCommandActionUtil.run(frameLayout, "addTextView", () -> newTextView.addTags(frameLayout, null, InsertType.PASTE));
    assertThat(frameLayout.getChildren()).isEmpty();
  }
}
