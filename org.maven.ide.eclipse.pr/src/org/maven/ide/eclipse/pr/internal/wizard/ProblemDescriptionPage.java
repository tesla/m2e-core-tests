/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.pr.internal.ProblemReportingImages;
import org.maven.ide.eclipse.wizards.AbstractMavenWizardPage;


/**
 * A problem description page
 * 
 * @author Anton Kraev
 */
public class ProblemDescriptionPage extends AbstractMavenWizardPage {

  String description = "";
  String summary = "";

  protected ProblemDescriptionPage() {
    super("problemDescriptionPage");
    setTitle("Problem description");
    setDescription("Enter problem summary and description");
    setImageDescriptor(ProblemReportingImages.REPORT_WIZARD);
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    composite.setLayout(gridLayout);
    setControl(composite);

    Label summaryLabel = new Label(composite, SWT.NONE);
    GridData gd_summaryLabel = new GridData();
    summaryLabel.setLayoutData(gd_summaryLabel);
    summaryLabel.setData("name", "summaryLabel");
    summaryLabel.setText("&Summary:");

    final Text summaryText = new Text(composite, SWT.BORDER);
    summaryText.setData("name", "summaryText");
    GridData gd_summaryText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    summaryText.setLayoutData(gd_summaryText);
    summaryText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        summary = summaryText.getText();
        getContainer().updateButtons();
      }
    });

    Label descriptionLabel = new Label(composite, SWT.NONE);
    GridData gd_descriptionLabel = new GridData();
    descriptionLabel.setLayoutData(gd_descriptionLabel);
    descriptionLabel.setData("name", "descriptionLabel");
    descriptionLabel.setText("&Description:");

    final Text descriptionText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP);
    descriptionText.setData("name", "descriptionText");
    GridData gd_descriptionText = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 21);
    descriptionText.setLayoutData(gd_descriptionText);
    descriptionText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        description = descriptionText.getText();
        getContainer().updateButtons();
      }
    });

  }

  public boolean canFlipToNextPage() {
    return !description.equals("") && !summary.equals("");
  }

  public String getDescription() {
    return description;
  }

  public String getSummary() {
    return summary;
  }

}
