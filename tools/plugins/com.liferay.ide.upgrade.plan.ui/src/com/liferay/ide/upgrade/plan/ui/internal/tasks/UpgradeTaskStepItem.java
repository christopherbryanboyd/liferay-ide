/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.upgrade.plan.ui.internal.tasks;

import com.liferay.ide.ui.util.UIUtil;
import com.liferay.ide.upgrade.plan.core.UpgradeEvent;
import com.liferay.ide.upgrade.plan.core.UpgradeListener;
import com.liferay.ide.upgrade.plan.core.UpgradePlanElement;
import com.liferay.ide.upgrade.plan.core.UpgradePlanElementStatus;
import com.liferay.ide.upgrade.plan.core.UpgradePlanElementStatusChangedEvent;
import com.liferay.ide.upgrade.plan.core.UpgradePlanner;
import com.liferay.ide.upgrade.plan.core.UpgradeTaskStep;
import com.liferay.ide.upgrade.plan.core.UpgradeTaskStepAction;
import com.liferay.ide.upgrade.plan.core.util.ServicesLookup;
import com.liferay.ide.upgrade.plan.ui.Disposable;
import com.liferay.ide.upgrade.plan.ui.internal.UpgradePlanUIPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * @author Christopher Bryan Boyd
 */
public class UpgradeTaskStepItem implements IExpansionListener, UpgradeItem, UpgradeListener {

	public UpgradeTaskStepItem(FormToolkit formToolkit, ScrolledForm scrolledForm, UpgradeTaskStep upgradeTaskStep) {
		_formToolkit = formToolkit;
		_scrolledForm = scrolledForm;
		_upgradeTaskStep = upgradeTaskStep;

		Composite parentComposite = _scrolledForm.getBody();

		GridDataFactory gridDataFactory = GridDataFactory.fillDefaults();

		gridDataFactory.grab(true, true);

		parentComposite.setLayoutData(gridDataFactory.create());

		_disposables.add(() -> parentComposite.dispose());

		parentComposite.setLayout(new TableWrapLayout());

		if (_upgradeTaskStep == null) {
			return;
		}

		FormText description = _formToolkit.createFormText(parentComposite, true);

		description.setText(_upgradeTaskStep.getDescription(), true, false);

		_disposables.add(() -> description.dispose());

		_buttonComposite = _formToolkit.createComposite(parentComposite);

		GridLayout buttonGridLayout = new GridLayout(2, false);

		buttonGridLayout.marginHeight = 2;
		buttonGridLayout.marginWidth = 2;
		buttonGridLayout.verticalSpacing = 2;

		_buttonComposite.setLayout(buttonGridLayout);

		_buttonComposite.setLayoutData(new TableWrapData(TableWrapData.FILL));

		_disposables.add(() -> _buttonComposite.dispose());

		Image taskStepActionPerformImage = UpgradePlanUIPlugin.getImage(
			UpgradePlanUIPlugin.TASK_STEP_ACTION_PERFORM_IMAGE);

		ImageHyperlink performImageHyperlink = createImageHyperlink(
			_formToolkit, _buttonComposite, taskStepActionPerformImage, this, "Click to perform",
			"Performing " + _upgradeTaskStep.getTitle() + "...", this::_perform);

		_disposables.add(() -> performImageHyperlink.dispose());

		_enables.add(performImageHyperlink);

		_fill(_formToolkit, _buttonComposite, _disposables);

		Image taskStepActionCompleteImage = UpgradePlanUIPlugin.getImage(
			UpgradePlanUIPlugin.TASK_STEP_ACTION_COMPLETE_IMAGE);

		ImageHyperlink completeImageHyperlink = createImageHyperlink(
			_formToolkit, _buttonComposite, taskStepActionCompleteImage, this, "Click when complete",
			"Completing " + _upgradeTaskStep.getTitle() + "...", this::_complete);

		_disposables.add(() -> completeImageHyperlink.dispose());

		_enables.add(completeImageHyperlink);

		_fill(_formToolkit, _buttonComposite, _disposables);

		Image taskStepActionSkipImage = UpgradePlanUIPlugin.getImage(UpgradePlanUIPlugin.TASK_STEP_ACTION_SKIP_IMAGE);

		ImageHyperlink skipImageHyperlink = createImageHyperlink(
			_formToolkit, _buttonComposite, taskStepActionSkipImage, this, "Skip",
			"Skipping " + _upgradeTaskStep.getTitle() + "...", this::_skip);

		_disposables.add(() -> skipImageHyperlink.dispose());

		_enables.add(skipImageHyperlink);

		_upgradePlanner = ServicesLookup.getSingleService(UpgradePlanner.class, null);

		_upgradePlanner.addListener(this);

		_updateEnablement(upgradeTaskStep, _enables);
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		_listeners.add(listener);
	}

	public void dispose() {
		_upgradePlanner.removeListener(this);

		for (Disposable disposable : _disposables) {
			try {
				disposable.dispose();
			}
			catch (Throwable t) {
			}
		}
	}

	@Override
	public void expansionStateChanged(ExpansionEvent expansionEvent) {
		ISelection selection = new StructuredSelection(_upgradeTaskStep);

		SelectionChangedEvent selectionChangedEvent = new SelectionChangedEvent(this, selection);

		_listeners.forEach(
			selectionChangedListener -> {
				selectionChangedListener.selectionChanged(selectionChangedEvent);
			});

		_scrolledForm.reflow(true);
	}

	@Override
	public void expansionStateChanging(ExpansionEvent expansionEvent) {
	}

	@Override
	public ISelection getSelection() {
		return null;
	}

	@Override
	public void onUpgradeEvent(UpgradeEvent upgradeEvent) {
		if (upgradeEvent instanceof UpgradePlanElementStatusChangedEvent) {
			UpgradePlanElementStatusChangedEvent upgradePlanElementStatusChangedEvent =
				(UpgradePlanElementStatusChangedEvent)upgradeEvent;

			UpgradePlanElement upgradePlanElement = upgradePlanElementStatusChangedEvent.getUpgradePlanElement();

			if (upgradePlanElement.equals(_upgradeTaskStep)) {
				UIUtil.async(() -> _updateEnablement(_upgradeTaskStep, _enables));
			}
		}
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		_listeners.remove(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
	}

	private static void _updateEnablement(UpgradeTaskStep upgradeTaskStep, Collection<Control> enables) {
		AtomicBoolean enabled = new AtomicBoolean(false);

		if (upgradeTaskStep.enabled() && !upgradeTaskStep.completed()) {
			enabled.set(true);
		}

		Stream<Control> stream = enables.stream();

		stream.filter(
			c -> !c.isDisposed()
		).forEach(
			c -> c.setEnabled(enabled.get())
		);
	}

	private IStatus _complete(IProgressMonitor progressMonitor) {
		_upgradeTaskStep.setStatus(UpgradePlanElementStatus.COMPLETED);

		return Status.OK_STATUS;
	}

	private void _fill(FormToolkit formToolkit, Composite parent, List<Disposable> disposables) {
		Label fillLabel = formToolkit.createLabel(parent, null);

		GridData gridData = new GridData();

		gridData.widthHint = 16;

		fillLabel.setLayoutData(gridData);

		disposables.add(() -> fillLabel.dispose());
	}

	private IStatus _perform(IProgressMonitor progressMonitor) {
		List<UpgradeTaskStepAction> actions = _upgradeTaskStep.getActions();

		if (actions.isEmpty()) {
			return _upgradeTaskStep.perform(progressMonitor);
		}

		return Status.OK_STATUS;
	}

	private IStatus _skip(IProgressMonitor progressMonitor) {
		_upgradeTaskStep.setStatus(UpgradePlanElementStatus.SKIPPED);

		return Status.OK_STATUS;
	}

	private Composite _buttonComposite;
	private List<Disposable> _disposables = new ArrayList<>();
	private List<Control> _enables = new ArrayList<>();
	private FormToolkit _formToolkit;
	private ListenerList<ISelectionChangedListener> _listeners = new ListenerList<>();
	private ScrolledForm _scrolledForm;
	private UpgradePlanner _upgradePlanner;
	private final UpgradeTaskStep _upgradeTaskStep;

}