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

package com.liferay.blade.test.deprecatedmethods;

import com.liferay.blade.api.FileMigrator;
import com.liferay.blade.api.Problem;
import com.liferay.blade.test.apichanges.APITestBase;

import java.io.File;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Amerson
 * @author Terry Jia
 */
public class DeprecatedMethodsTest extends APITestBase {

	@Test
	public void deprecatedMethods61TestFile() throws Exception {
		FileMigrator fmigrator = context.getService(fileMigrators[0]);

		List<Problem> problems = fmigrator.analyze(deprecatedMethods61TestFile);

		context.ungetService(fileMigrators[0]);

		Assert.assertNotNull(problems);
		Assert.assertEquals("", 4, problems.size());
	}

	@Test
	public void deprecatedMethodsNoneVersionTestFile() throws Exception {
		FileMigrator fmigrator = context.getService(fileMigrators[0]);

		List<Problem> problems = fmigrator.analyze(deprecatedMethodsNoneVersionTestFile);

		context.ungetService(fileMigrators[0]);

		Assert.assertNotNull(problems);
		Assert.assertEquals("", 2, problems.size());
	}

	@Override
	public int getExpectedNumber() {
		return 60;
	}

	@Override
	public String getComponentName() {
		return "com.liferay.blade.upgrade.liferay70.deprecatedmethods.DeprecatedMethodsMigrator";
	}

	@Override
	public File getTestFile() {
		return new File("projects/deprecated-methods-test/PortalMockFactory.java");
	}

	public File deprecatedMethods61TestFile = new File(
		"projects/deprecated-methods-test/AssetVocabularyServiceSoap.java");
	public File deprecatedMethodsNoneVersionTestFile = new File(
		"projects/deprecated-methods-test/WebServerServlet.java");

}