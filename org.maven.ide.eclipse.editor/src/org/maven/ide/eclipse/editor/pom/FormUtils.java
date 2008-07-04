/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.xml.MvnIndexPlugin;
import org.maven.ide.eclipse.editor.xml.search.Packaging;
import org.maven.ide.eclipse.editor.xml.search.SearchEngine;

/**
 * @author Eugene Kuleshov
 */
public abstract class FormUtils {

  /**
   * Proxy factory for compatibility stubs
   */
  @SuppressWarnings("unchecked")
  public static <T> T proxy(final Object o, Class<T> type) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), //
        new Class[] { type }, //
        new InvocationHandler() {
          public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
            try {
              Method mm = o.getClass().getMethod(m.getName(), m.getParameterTypes());
              return mm.invoke(o, args);
            } catch (final NoSuchMethodException e) {
              return null;
            }
          }
        });
  }

  /**
   * Stub interface for API added to FormToolikt in Eclipse 3.3
   */
  public interface FormTooliktStub {
    public void decorateFormHeading(Form form);
  }

  public static String nvl(String s) {
    return s == null ? "" : s;
  }

  public static boolean isEmpty(String s) {
    return s == null || s.length()==0;
  }

  public static boolean isEmpty(Text t) {
    return t == null || isEmpty(t.getText());
  }

  public static void setText(Text control, String text) {
    if(control!=null && !control.isDisposed() && !control.getText().equals(text)) {
      control.setText(nvl(text));
    }
  }

  public static void setText(CCombo control, String text) {
    if(control!=null && !control.isDisposed() && !control.getText().equals(text)) {
      control.setText(nvl(text));
    }
  }

  public static void setButton(Button control, boolean selection) {
    if(control!=null && !control.isDisposed() && control.getSelection()!=selection) {
      control.setSelection(selection);
    }
  }

  public static void openHyperlink(String url) {
    if(!isEmpty(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
      url = url.trim();
      try {
        IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
        IWebBrowser browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR
            | IWorkbenchBrowserSupport.LOCATION_BAR, url, url, url);
        browser.openURL(new URL(url));
      } catch(PartInitException ex) {
        MavenPlugin.log(ex);
      } catch(MalformedURLException ex) {
        MavenPlugin.log("Malformed url " + url, ex);
      }
    }
  }

  public static void setEnabled(Composite composite, boolean enabled) {
    if(composite!=null && !composite.isDisposed()) {
      composite.setEnabled(enabled);
      for(Control control : composite.getChildren()) {
        if(control instanceof Combo) {
          control.setEnabled(enabled);

        } else if(control instanceof CCombo) {
          control.setEnabled(enabled);

        } else if(control instanceof Hyperlink) {
          control.setEnabled(enabled);

        } else if(control instanceof Composite) {
          setEnabled((Composite) control, enabled);

        } else {
          control.setEnabled(enabled);

        }
      }
    }
  }

  public static void setReadonly(Composite composite, boolean readonly) {
    if(composite!=null) {
      for(Control control : composite.getChildren()) {
        if(control instanceof Text) {
          ((Text) control).setEditable(!readonly);

        } else if(control instanceof Combo) {
          ((Combo) control).setEnabled(!readonly);

        } else if(control instanceof CCombo) {
          ((CCombo) control).setEnabled(!readonly);

        } else if(control instanceof Button) {
          ((Button) control).setEnabled(!readonly);

        } else if(control instanceof Composite) {
          setReadonly((Composite) control, readonly);

        }
      }
    }
  }

  public static void addGroupIdProposal(final Text groupIdText, final Packaging packaging) {
    addCompletionProposal(groupIdText, new Searcher() {
      public Collection<String> search() {
        // TODO handle artifact info
        return getSearchEngine().findGroupIds(groupIdText.getText(), packaging, null);
      }
    });
  }

  public static void addArtifactIdProposal(final Text groupIdText, final Text artifactIdText,
      final Packaging packaging) {
    addCompletionProposal(artifactIdText, new Searcher() {
      public Collection<String> search() {
        // TODO handle artifact info
        return getSearchEngine().findArtifactIds(groupIdText.getText(), artifactIdText.getText(), packaging, null);
      }
    });
  }

  public static void addVersionProposal(final Text groupIdText, final Text artifactIdText, final Text versionText,
      final Packaging packaging) {
    addCompletionProposal(versionText, new Searcher() {
      public Collection<String> search() {
        return getSearchEngine().findVersions(groupIdText.getText(), //
            artifactIdText.getText(), versionText.getText(), packaging);
      }
    });
  }

  public static void addClassifierProposal(final Text groupIdText, final Text artifactIdText, final Text versionText,
      final Text classifierText, final Packaging packaging) {
    addCompletionProposal(classifierText, new Searcher() {
      public Collection<String> search() {
        return getSearchEngine().findClassifiers(groupIdText.getText(), //
            artifactIdText.getText(), versionText.getText(), classifierText.getText(), packaging);
      }
    });
  }

  public static void addTypeProposal(final Text groupIdText, final Text artifactIdText, final Text versionText,
      final CCombo typeCombo, final Packaging packaging) {
    addCompletionProposal(typeCombo, new Searcher() {
      public Collection<String> search() {
        return getSearchEngine().findTypes(groupIdText.getText(), //
            artifactIdText.getText(), versionText.getText(), typeCombo.getText(), packaging);
      }
    });
  }

  public static void addCompletionProposal(final Control control, final Searcher searcher) {
    FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
    ControlDecoration decoration = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
    decoration.setShowOnlyOnFocus(true);
    decoration.setDescriptionText(fieldDecoration.getDescription());
    decoration.setImage(fieldDecoration.getImage());

    IContentProposalProvider proposalProvider = new IContentProposalProvider() {
      public IContentProposal[] getProposals(String contents, int position) {
        ArrayList<IContentProposal> proposals = new ArrayList<IContentProposal>();
        for(final String text : searcher.search()) {
          proposals.add(new FormUtils.TextProposal(text));
        }
        return proposals.toArray(new IContentProposal[proposals.size()]);
      }
    };

    IControlContentAdapter contentAdapter;
    if(control instanceof Text) {
      contentAdapter = new TextContentAdapter();
    } else {
      contentAdapter = new CComboContentAdapter();
    }

    ContentProposalAdapter adapter = new ContentProposalAdapter(control, contentAdapter, //
        proposalProvider, KeyStroke.getInstance(SWT.CTRL, ' '), null);
    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
    adapter.setPopupSize(new Point(250, 120));
    adapter.setPopupSize(new Point(250, 120));
  }

  private static SearchEngine getSearchEngine() {
    return MvnIndexPlugin.getDefault().getSearchEngine();
  }

  public static abstract class Searcher {
    public abstract Collection<String> search();
  }


  public static final class TextProposal implements IContentProposal {
    private final String text;

    public TextProposal(String text) {
      this.text = text;
    }

    public int getCursorPosition() {
      return text.length();
    }

    public String getContent() {
      return text;
    }

    public String getLabel() {
      return text;
    }

    public String getDescription() {
      return null;
    }
  }

}
