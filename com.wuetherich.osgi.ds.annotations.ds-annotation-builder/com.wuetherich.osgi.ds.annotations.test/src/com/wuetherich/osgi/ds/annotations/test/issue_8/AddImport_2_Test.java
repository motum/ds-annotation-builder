package com.wuetherich.osgi.ds.annotations.test.issue_8;

import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import com.wuetherich.osgi.ds.annotations.Constants;
import com.wuetherich.osgi.ds.annotations.test.util.AbstractDsAnnotationsTest;
import com.wuetherich.osgi.ds.annotations.test.util.EclipseProjectUtils;

public class AddImport_2_Test extends AbstractDsAnnotationsTest {

	@Test
	public void test() throws CoreException {

		//
		EclipseProjectUtils.checkFileExists(getProject(), Constants.COMPONENT_DESCRIPTION_FOLDER
				+ "/de.test.Test.xml");
	}

	@Override
	protected SourceFile createSourceFile() {
		return new SourceFile.Default(
				"de/test/Test.java",
				"package de.test; import org.osgi.service.component.annotations.Component; @Component public class Test {}");
	}

	@Override
	protected String[] getImportedPackages() {
		return new String[] { "org.osgi.framework" };
	}
}
