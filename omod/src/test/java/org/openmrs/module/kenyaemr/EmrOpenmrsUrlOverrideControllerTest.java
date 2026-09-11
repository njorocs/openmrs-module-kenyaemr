/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openmrs.api.context.Context;

import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link EmrOpenmrsUrlOverrideController}
 */
public class EmrOpenmrsUrlOverrideControllerTest {

	private EmrOpenmrsUrlOverrideController controller;

	private MockedStatic<Context> mockedContext;

	/**
	 * Setup each test
	 */
	@Before
	public void setup() {
		controller = new EmrOpenmrsUrlOverrideController();
		mockedContext = Mockito.mockStatic(Context.class);
	}

	@After
	public void tearDown() {
		mockedContext.close();
	}

	/**
	 * @see EmrOpenmrsUrlOverrideController#showOurHomePage()
	 */
	@Test
	public void showOurHomepage() {
		mockedContext.when(Context::isAuthenticated).thenReturn(false);
		Assert.assertThat(controller.showOurHomePage(), is("redirect:login.htm"));

		mockedContext.when(Context::isAuthenticated).thenReturn(true);
		Assert.assertThat(controller.showOurHomePage(), is("redirect:spa/home"));
	}

	/**
	 * @see EmrOpenmrsUrlOverrideController#showOurLoginPage()
	 */
	@Test
	public void showLoginHomepage() {
		mockedContext.when(Context::isAuthenticated).thenReturn(false);
		Assert.assertThat(controller.showOurLoginPage(), is("redirect:spa/login"));

		mockedContext.when(Context::isAuthenticated).thenReturn(true);
		Assert.assertThat(controller.showOurLoginPage(), is("redirect:index.htm"));
	}

	/**
	 * @see EmrOpenmrsUrlOverrideController#showOurForgotPasswordPage()
	 */
	@Test
	public void showOurForgotPasswordPage() {
		mockedContext.when(Context::isAuthenticated).thenReturn(false);
		Assert.assertThat(controller.showOurForgotPasswordPage(), is("redirect:/kenyaemr/forgotPassword.page"));

		mockedContext.when(Context::isAuthenticated).thenReturn(true);
		Assert.assertThat(controller.showOurForgotPasswordPage(), is("redirect:/index.htm"));
	}
}