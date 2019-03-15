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

package com.liferay.ide.upgrade.plan.core;

import com.liferay.ide.upgrade.plan.core.util.ServicesLookup;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Adapters;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

/**
 * @author Christopher Bryan Boyd
 * @author Gregory Amerson
 * @author Simon Jiang
 * @author Terry Jia
 */
public abstract class BaseUpgradeTask extends BaseUpgradePlanElement implements UpgradeTask {

	@Activate
	public void activate(ComponentContext componentContext) {
		super.activate(componentContext);

		Dictionary<String, Object> properties = componentContext.getProperties();

		_categoryId = getStringProperty(properties, "categoryId");

		_lookupTaskSteps(componentContext);
	}

	public boolean completed() {
		List<UpgradeTaskStep> steps = getSteps();

		if (steps.isEmpty()) {
			return super.completed();
		}

		for (UpgradeTaskStep step : steps) {
			List<UpgradeTaskStepAction> actions = step.getActions();

			if (actions.isEmpty() && !step.completed()) {
				return false;
			}

			for (UpgradeTaskStepAction action : actions) {
				if (!action.completed()) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean equals(Object object) {
		if ((object instanceof BaseUpgradeTask) == false) {
			return false;
		}

		BaseUpgradeTask baseUpgradeTask = Adapters.adapt(object, BaseUpgradeTask.class);

		if (baseUpgradeTask == null) {
			return false;
		}

		if (super.equals(object) && isEqualIgnoreCase(_categoryId, baseUpgradeTask._categoryId) &&
			isEqual(_upgradeTaskSteps, baseUpgradeTask._upgradeTaskSteps)) {

			return true;
		}

		return false;
	}

	@Override
	public String getCategoryId() {
		return _categoryId;
	}

	@Override
	public List<UpgradeTaskStep> getSteps() {
		return Collections.unmodifiableList(_upgradeTaskSteps);
	}

	@Override
	public int hashCode() {
		int hash = super.hashCode();

		hash = 31 * hash + (_categoryId != null ? _categoryId.hashCode() : 0);
		hash = 31 * hash + (Arrays.hashCode(_upgradeTaskSteps.toArray()));

		return hash;
	}

	private void _lookupTaskSteps(ComponentContext componentContext) {
		BundleContext bundleContext = componentContext.getBundleContext();

		List<UpgradeTaskStep> upgradeTaskSteps = ServicesLookup.getOrderedServices(
			bundleContext, UpgradeTaskStep.class, "(taskId=" + getId() + ")");

		UpgradePlanner upgradePlanner = ServicesLookup.getSingleService(UpgradePlanner.class, null);

		if (upgradePlanner == null) {
			return;
		}

		Stream<UpgradeTaskStep> stream = upgradeTaskSteps.stream();

		_upgradeTaskSteps = stream.filter(
			upgradeTaskStep -> upgradeTaskStep.appliesTo(upgradePlanner.getCurrentUpgradePlan())
		).collect(
			Collectors.toList()
		);
	}

	private String _categoryId;
	private List<UpgradeTaskStep> _upgradeTaskSteps;

}